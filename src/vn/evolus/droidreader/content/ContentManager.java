package vn.evolus.droidreader.content;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.evolus.droidreader.Application;
import vn.evolus.droidreader.GoogleReaderFactory;
import vn.evolus.droidreader.content.processors.ImageItemProcessor;
import vn.evolus.droidreader.content.processors.VideoItemProcessor;
import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.model.Image;
import vn.evolus.droidreader.model.Item;
import vn.evolus.droidreader.model.Job;
import vn.evolus.droidreader.model.SyncItemTagJob;
import vn.evolus.droidreader.model.Channel.Channels;
import vn.evolus.droidreader.model.Image.Images;
import vn.evolus.droidreader.model.Item.Items;
import vn.evolus.droidreader.model.Job.Jobs;
import vn.evolus.droidreader.providers.ContentsProvider;
import vn.evolus.droidreader.util.ActiveList;
import vn.evolus.droidreader.util.StreamUtils;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.google.reader.GoogleReader;

public class ContentManager {
	public static final long ALL_CHANNELS = -1;
	public static final ChannelLoader FULL_CHANNEL_LOADER = new FullChannelLoader();
	public static final ChannelLoader LIGHTWEIGHT_CHANNEL_LOADER = new LightweightChannelLoader();
	public static final ChannelLoader WITH_IMAGE_CHANNEL_LOADER = new WithImageChannelLoader();
	public static final ItemLoader FULL_ITEM_LOADER = new FullItemLoader();
	public static final ItemLoader LIGHTWEIGHT_ITEM_LOADER = new LightweightItemLoader();
	public static final ItemLoader ID_ONLY_ITEM_LOADER = new IdOnlyItemLoader();
	
	private static List<ItemProcessor> itemProcessors;
	
	private static ContentResolver cr;
	
	static {
		cr = Application.getInstance().getContentResolver();
		itemProcessors = new ArrayList<ItemProcessor>();
		InputStream is = null;
		try {
			is = Application.getInstance().getAssets().open("blacklist.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		itemProcessors.add(new ImageItemProcessor(StreamUtils.readLines(is, "UTF-8")));
		itemProcessors.add(new VideoItemProcessor());
	}
	
	private static final Map<Long, WeakReference<Channel>> channelCache = 
		new HashMap<Long, WeakReference<Channel>>();	
	
	public static ArrayList<Channel> loadAllChannels(ChannelLoader loader) {
		Cursor cursor = cr.query(Channels.CONTENT_URI,
				loader.getProjection(), 
				null, 
				null,
				null);
		ArrayList<Channel> channels = new ActiveList<Channel>();
		while (cursor.moveToNext()) {
			Channel channel = loader.load(cursor);
			putChannelToCache(channel);
			channels.add(channel);
		}
		cursor.close();
		return channels;
	}
	
	public static void subscribe(Channel channel) throws Exception {
		GoogleReader reader = GoogleReaderFactory.getGoogleReader();
		reader.subscribe(channel.url);
		saveChannel(channel);
	}
	
	public static void unsubscribe(Channel channel) throws Exception {
		GoogleReader reader = GoogleReaderFactory.getGoogleReader();
		reader.unsubscribe(channel.url);
		deleteChannel(channel);
	}
	
	public static void saveChannel(Channel channel) {
		ContentValues values = new ContentValues();
		values.put(Channels.TITLE, channel.title);
		values.put(Channels.URL, channel.url);
		values.put(Channels.DESCRIPTION, channel.description);
		values.put(Channels.LINK, channel.link);
		values.put(Channels.IMAGE_URL, channel.imageUrl);
		if (channel.id == 0) {
			Uri contentUri = cr.insert(Channels.CONTENT_URI, values);
			channel.id = ContentUris.parseId(contentUri);
		} else {
			cr.update(Channels.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
					new String[] { String.valueOf(channel.id) });
		}
		
		// invalidate cache
		Channel channelInCache = getChannelFromCache(channel.id);
		if (channelInCache != null) {
			channelCache.remove(channel.id);
			putChannelToCache(channel);
		}
	}
	
	public static Channel loadChannel(long id, ChannelLoader loader) {
		Channel channel = getChannelFromCache(id);
		if (channel != null) return channel;
		
		Cursor cursor = cr.query(Channels.CONTENT_URI, 
				loader.getProjection(), 
				ContentsProvider.WHERE_ID, 
				new String[] { String.valueOf(id) }, null);
		if (cursor.moveToFirst()) {
			channel = loader.load(cursor);
			putChannelToCache(channel);
		}
		cursor.close();
		
		return channel;
	}
	
	public static Channel loadChannel(String url, ChannelLoader loader) {		
		Cursor cursor = cr.query(Channels.CONTENT_URI, 
				loader.getProjection(), 
				Channels.URL + "=?", 
				new String[] { String.valueOf(url) }, null);
		Channel channel = null;
		if (cursor.moveToFirst()) {
			channel = loader.load(cursor);
			putChannelToCache(channel);
		}
		cursor.close();
		
		return channel;
	}

	public static boolean existChannel(Channel channel) {
		Cursor cursor = cr.query(Channels.CONTENT_URI, 
				new String[] {
					Channels.ID
				}, 
				Channels.URL + "=?",
				new String[] { channel.url },
				null);
		boolean result = cursor.moveToFirst();
		cursor.close();
		return result;
	}
	
	public static void cleanChannel(Channel channel, int keepMaxItems) {
		// delete old items
		Cursor cursor = cr.query(Items.limitAndStartAt(1, keepMaxItems - 1),
				new String[] { Items.ID, Items.PUB_DATE, Items.UPDATE_TIME },
				Items.CHANNEL_ID + "=?", 
				new String[] { String.valueOf(channel.id) },
				Items.UPDATE_TIME + " DESC, " +
				Items.PUB_DATE + " DESC, " +
				Items.ID + " ASC");
		if (cursor.moveToNext()) {
			long id = cursor.getLong(0);
			long lastPubDate = cursor.getLong(1);
			long updateTime = cursor.getLong(2);
			String selection = Items.CHANNEL_ID + "=? AND (" + Items.UPDATE_TIME + "<? OR (" + Items.UPDATE_TIME + "=? AND (" + 
				Items.PUB_DATE + "<? OR (" + Items.PUB_DATE + " =? AND " + Items.ID + ">?))))";
			cr.delete(Items.CONTENT_URI,
					selection,
					new String[] {
						String.valueOf(channel.id),						
						String.valueOf(updateTime),
						String.valueOf(updateTime),
						String.valueOf(lastPubDate),
						String.valueOf(lastPubDate),
						String.valueOf(id) });
		}		
		cursor.close();
	}
	
	public static void deleteChannel(Channel channel) {
		cr.delete(Items.CONTENT_URI, Items.CHANNEL_ID + "=?", new String[] { String.valueOf(channel.id) });
		cr.delete(Channels.CONTENT_URI, ContentsProvider.WHERE_ID, new String[] { String.valueOf(channel.id) });
		channel.id = 0;
		
		if (isChannelInCache(channel.id)) {
			channelCache.remove(channel.id);
		}
	}
	
	public static boolean saveItem(Item item) {
		ContentValues values = new ContentValues();
		if (item.id == 0) {
			if (existItem(item)) return false;
						
			values.put(Items.TITLE, item.title);
			values.put(Items.DESCRIPTION, item.description);
			values.put(Items.PUB_DATE, item.pubDate.getTime());
			values.put(Items.LINK, item.link);
			values.put(Items.IMAGE_URL, item.imageUrl);
			values.put(Items.READ, item.read);
			values.put(Items.STARRRED, item.starred);
			values.put(Items.KEPT, item.kept);
			values.put(Items.CHANNEL_ID, item.channel.id);
			values.put(Items.UPDATE_TIME, item.updateTime);			
			values.put(Items.ORIGINAL_ID, item.originalId);
			Uri contentUri = cr.insert(Items.CONTENT_URI, values);
			item.id = ContentUris.parseId(contentUri);
		} else {
			values.put(Items.READ, item.read);
			cr.update(Items.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
					new String[] { String.valueOf(item.id) });			
		}
		return true;
	}
	
	public static Item loadItem(long id, ItemLoader loader, ChannelLoader channelLoader) {		
		Cursor cursor = cr.query(Items.CONTENT_URI,
				loader.getProjection(),
				Items.ID + "=?", 
				new String[] { String.valueOf(id) }, 
				null);
		Item item = null;
		if (cursor.moveToNext()) {						
			item = loader.load(cursor);
			if (channelLoader != null) {
				item.channel = loadChannel(item.channel.id, channelLoader);
			}
		}
		cursor.close();
		return item;
	}
	
	public static void deleteItem(Item item) {
		cr.delete(Items.CONTENT_URI, Items.ID + "=?", new String[] { String.valueOf(item.id) });		
	}
	
	public static void markItemAsRead(Item item) {
		if (item.read) return;
		
		item.read = true;
		ContentValues values = new ContentValues();
		values.put(Items.READ, 1);
		cr.update(Items.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
				new String[] { String.valueOf(item.id) });
		
		SyncItemTagJob job = new SyncItemTagJob(SyncItemTagJob.ACTION_ADD, 
					item.originalId, GoogleReader.ITEM_STATE_READ);
		saveJob(job);		
	}
	
	public static void markItemAsStarred(Item item) {
		if (item.starred) return;
		
		// delete previous REMOVE tag job
		SyncItemTagJob removeJob = new SyncItemTagJob(SyncItemTagJob.ACTION_REMOVE, 
				item.originalId, GoogleReader.ITEM_STATE_READ);
		deleteJob(removeJob.type, removeJob.params);
		
		// saving state to DB
		item.starred = true;
		ContentValues values = new ContentValues();
		values.put(Items.STARRRED, 1);
		cr.update(Items.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
				new String[] { String.valueOf(item.id) });
		
		// create a new REMOVE tag job
		SyncItemTagJob job = new SyncItemTagJob(SyncItemTagJob.ACTION_ADD, 
				item.originalId, GoogleReader.ITEM_STATE_STARRED);
		saveJob(job);
	}
	
	public static void unmarkItemAsStarred(Item item) {
		if (!item.starred) return;
		
		// delete previous ADD tag job
		SyncItemTagJob addJob = new SyncItemTagJob(SyncItemTagJob.ACTION_REMOVE, 
				item.originalId, GoogleReader.ITEM_STATE_READ);
		deleteJob(addJob.type, addJob.params);
		
		// saving state to DB
		item.starred = false;
		ContentValues values = new ContentValues();
		values.put(Items.STARRRED, 1);
		cr.update(Items.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
				new String[] { String.valueOf(item.id) });
		
		// create a new ADD tag job
		SyncItemTagJob job = new SyncItemTagJob(SyncItemTagJob.ACTION_REMOVE, 
				item.originalId, GoogleReader.ITEM_STATE_STARRED);
		saveJob(job);
	}

	public static void processItem(Item item) {
		for (ItemProcessor processor : itemProcessors) {
			processor.process(item);
		}
	}
	
	public static boolean existItem(Item item) {
		return existItem(item.link);
	}
	
	public static boolean existItem(String link) {
		Cursor cursor = cr.query(Items.CONTENT_URI, 
				new String[] {
					Items.ID
				}, 
				Items.LINK + "=?",
				new String[] { link },
				null);
		boolean result = cursor.moveToFirst();
		cursor.close();
		return result;
	}
	
	public static void loadAllItemsOfChannel(Channel channel, ItemLoader loader) {
		Cursor cursor = cr.query(Items.CONTENT_URI,
				loader.getProjection(),
				Items.CHANNEL_ID + "=?", 
				new String[] { String.valueOf(channel.id) },
				Items.UPDATE_TIME + " DESC, " +
				Items.PUB_DATE + " DESC, " +
				Items.ID + " ASC");
		ActiveList<Item> items = channel.getItems();
		items.clear();
		while (cursor.moveToNext()) {						
			Item item = loader.load(cursor);
			item.channel = channel;			
			items.add(item);
		}
		cursor.close();
	}
	
	public static ActiveList<Item> loadLatestItems(int maxItems, boolean showRead,
			ItemLoader loader, ChannelLoader channelLoader) {
		String selection = null;		
		if (!showRead) {
			selection = Items.READ + "=0";			
		}
		Cursor cursor = cr.query(Items.limit(maxItems),
				loader.getProjection(),
				selection,
				null,
				Items.UPDATE_TIME + " DESC, " +
				Items.PUB_DATE + " DESC, " +
				Items.ID + " ASC");
		ActiveList<Item> items = new ActiveList<Item>();
		items.clear();
		while (cursor.moveToNext()) {						
			Item item = loader.load(cursor);
			if (channelLoader != null) {
				item.channel = loadChannel(item.channel.id, channelLoader);
			}
			items.add(item);			
		}
		cursor.close();
		return items;
	}
	
	public static List<Item> loadOlderItems(
			Item olderThanItem,
			int maxItems,
			boolean showRead,
			ItemLoader loader, 
			ChannelLoader channelLoader) {
		return loadOlderItems(olderThanItem, ALL_CHANNELS, maxItems, showRead, loader, channelLoader);
	}
	
	public static List<Item> loadOlderItems(
			Item olderThanItem,
			long channelId,
			int maxItems,
			boolean showRead,
			ItemLoader loader, 
			ChannelLoader channelLoader) {
		String selection = 
			"(" + Items.UPDATE_TIME + "<? OR (" + Items.UPDATE_TIME + "=? AND (" + 
				Items.PUB_DATE + "<? OR (" + Items.PUB_DATE + " =? AND " + Items.ID + ">?))))";
		if (!showRead) {
			selection = selection + " AND " + Items.READ + "=0";
		}
		String[] selectionArgs = null;
		if (channelId > 0) {
			selection += " AND " + Items.CHANNEL_ID + "=?";
			selectionArgs = new String[] {				
				String.valueOf(olderThanItem.updateTime),
				String.valueOf(olderThanItem.updateTime),
				String.valueOf(olderThanItem.pubDate.getTime()),
				String.valueOf(olderThanItem.pubDate.getTime()),
				String.valueOf(olderThanItem.id),
				String.valueOf(channelId)
			};
		} else {
			selectionArgs = new String[] {
				String.valueOf(olderThanItem.updateTime),
				String.valueOf(olderThanItem.updateTime),
				String.valueOf(olderThanItem.pubDate.getTime()),
				String.valueOf(olderThanItem.pubDate.getTime()),
				String.valueOf(olderThanItem.id)
			};
		}	
		Cursor cursor = cr.query(Items.limit(maxItems),
				loader.getProjection(),
				selection,
				selectionArgs,
				Items.UPDATE_TIME + " DESC, " +
				Items.PUB_DATE + " DESC, " +
				Items.ID + " ASC");
		List<Item> items = new ArrayList<Item>();
		items.clear();
		while (cursor.moveToNext()) {						
			Item item = loader.load(cursor);
			if (channelLoader != null) {
				item.channel = loadChannel(item.channel.id, channelLoader);
			}
			items.add(item);			
		}
		cursor.close();
		return items;
	}
	
	public static List<Item> loadNewerItems(
			Item newerThanItem,			
			int maxItems,
			boolean loadReadItems,
			ItemLoader loader, 
			ChannelLoader channelLoader) {
		return loadNewerItems(newerThanItem, ALL_CHANNELS, maxItems, loadReadItems, loader, channelLoader);
	}
	
	public static List<Item> loadNewerItems(
			Item newerThanItem,
			long channelId,
			int maxItems,
			boolean loadReadItems,
			ItemLoader loader, 
			ChannelLoader channelLoader) {
		String selection = 
			"(" + Items.UPDATE_TIME + ">? OR (" + Items.UPDATE_TIME + "=? AND (" + 
				Items.PUB_DATE + ">? OR (" + Items.PUB_DATE + " =? AND " + Items.ID + "<?))))";
		if (!loadReadItems) {
			selection = selection + " AND " + Items.READ + "=0";
		}
		String[] selectionArgs = null;
		if (channelId > 0) {
			selection += " AND " + Items.CHANNEL_ID + "=?";
			selectionArgs = new String[] {				
				String.valueOf(newerThanItem.updateTime),
				String.valueOf(newerThanItem.updateTime),
				String.valueOf(newerThanItem.pubDate.getTime()),
				String.valueOf(newerThanItem.pubDate.getTime()),
				String.valueOf(newerThanItem.id),
				String.valueOf(channelId)
			};
		} else {
			selectionArgs = new String[] {
				String.valueOf(newerThanItem.updateTime),
				String.valueOf(newerThanItem.updateTime),
				String.valueOf(newerThanItem.pubDate.getTime()),
				String.valueOf(newerThanItem.pubDate.getTime()),
				String.valueOf(newerThanItem.id)
			};
		}
		Cursor cursor = cr.query(Items.limit(maxItems),
				loader.getProjection(),
				selection,
				selectionArgs,
				Items.UPDATE_TIME + " ASC," +				
				Items.PUB_DATE + " ASC, " + 
				Items.ID +  " DESC");
		List<Item> items = new ArrayList<Item>();
		items.clear();
		while (cursor.moveToNext()) {
			Item item = loader.load(cursor);
			if (channelLoader != null) {
				item.channel = loadChannel(item.channel.id, channelLoader);
			}
			items.add(item);			
		}
		cursor.close();
		return items;
	}
	
	public static void markAllItemsOfChannelAsRead(Channel channel) {		
		Cursor cursor = cr.query(Items.CONTENT_URI,				
				new String[] {
					Items.ID,
					Items.ORIGINAL_ID,
				},				
				Items.CHANNEL_ID + "=? AND " + Items.READ + "=0",
				new String[] { String.valueOf(channel.id) },
				null);
		Item item = new Item();
		while (cursor.moveToNext()) {
			item.id = cursor.getLong(0);
			item.read = false;
			item.originalId = cursor.getString(1);
			markItemAsRead(item);
		}
		for (Item channelItem : channel.getItems()) {
			channelItem.read = true;
		}
		cursor.close();
	}
	
	/**
	 * 
	 * @param cr
	 * @return a map of ChannelId <-> Unread count
	 */
	public static Map<Long, Integer> countUnreadItemsForEachChannel() {
		Cursor cursor = cr.query(Items.countUnread(), 
				new String[] { Items.CHANNEL_ID, Items.UNREAD_COUNT }, 
				Items.READ + "=?", new String[] { "0" }, null);
		Map<Long, Integer> unreadCounts = new HashMap<Long, Integer>();
		while (cursor.moveToNext()) {					
			unreadCounts.put(cursor.getLong(0), cursor.getInt(1));
		}
		cursor.close();
		return unreadCounts;
	}
	
	public static int countUnreadItems() {
		Cursor cursor = cr.query(Items.countUnread(), 
				new String[] { Items.UNREAD_COUNT }, 
				Items.READ + "=?", new String[] { "0" }, null);		
		int unreadCounts = 0;
		if (cursor.moveToNext()) {					
			unreadCounts = cursor.getInt(0);
		}
		cursor.close();
		return unreadCounts;
	}
		
	public static int countItems(long channelId, boolean countReadItems) {
		String selection = null;
		String[] selectionArgs = null;
		if (channelId > 0) {
			selection = Items.CHANNEL_ID + "=?";
			selectionArgs = new String[] { String.valueOf(channelId) };
		}
		if (!countReadItems) {
			selection = Items.READ + "=0" + (selection != null ? " AND " + selection : "");
		}
		Cursor cursor = cr.query(Items.CONTENT_URI, 
				new String[] { Items.COUNT }, 
				selection, 
				selectionArgs,
				null);		
		int count = 0;
		if (cursor.moveToNext()) {					
			count = cursor.getInt(0);
		}
		cursor.close();
		return count;
	}
	
	public static int countNewerItems(Item item, long channelId, boolean countReadItems) {
		String selection = 
			"(" + Items.UPDATE_TIME + ">? OR (" + Items.UPDATE_TIME + "=? AND (" + 
				Items.PUB_DATE + ">? OR (" + Items.PUB_DATE + " =? AND " + Items.ID + "<?))))";
		if (!countReadItems) {
			selection = selection + " AND " + Items.READ + "=0";
		}
		String[] selectionArgs = null;
		if (channelId > 0) {
			selection += " AND " + Items.CHANNEL_ID + "=?";
			selectionArgs = new String[] {				
				String.valueOf(item.updateTime),
				String.valueOf(item.updateTime),
				String.valueOf(item.pubDate.getTime()),
				String.valueOf(item.pubDate.getTime()),
				String.valueOf(item.id),
				String.valueOf(channelId)
			};
		} else {
			selectionArgs = new String[] {
				String.valueOf(item.updateTime),
				String.valueOf(item.updateTime),
				String.valueOf(item.pubDate.getTime()),
				String.valueOf(item.pubDate.getTime()),
				String.valueOf(item.id)
			};
		}
		Cursor cursor = cr.query(Items.CONTENT_URI, 
				new String[] { Items.COUNT }, 
				selection,
				selectionArgs, 
				null);
		int count = 0;
		if (cursor.moveToNext()) {					
			count = cursor.getInt(0);
		}
		cursor.close();				
		return count;
	}
	
	public static ArrayList<Image> loadAllQueuedImages() {
		Cursor cursor = cr.query(Images.CONTENT_URI, 
				new String[] {
					Images.ID,
					Images.URL,
					Images.STATUS,
					Images.RETRIES
				},
				Images.STATUS + "=?",
				new String[] { String.valueOf(Image.IMAGE_STATUS_QUEUED) },
				Images.RETRIES + " DESC, " + Images.ID);
		ArrayList<Image> images = new ArrayList<Image>();
		while (cursor.moveToNext()) {			
			Image image = new Image(cursor.getLong(0), cursor.getString(1), (byte)cursor.getInt(2));
			image.retries = (byte)cursor.getInt(3);
			images.add(image);
		}
		cursor.close();
		return images;
	}
	
	public static List<Long> loadOldestImageIds(int keepMaxItems) {
		Cursor cursor = cr.query(Images.CONTENT_URI, 
				new String[] {
					Images.COUNT
				},
				null,
				null,
				null);
		int totalImages = 0;
		if (cursor.moveToNext()) {
			totalImages = cursor.getInt(0);
		}
		cursor.close();
		ArrayList<Long> images = new ArrayList<Long>();
		if (totalImages - keepMaxItems > 0) {
			cursor = cr.query(Images.limit(totalImages - keepMaxItems), 
					new String[] {
						Images.ID
					},
					null,
					null,
					Images.ID + " ASC");
			
			while (cursor.moveToNext()) {			
				images.add(cursor.getLong(0));
			}
			cursor.close();
		}
		return images;
	}
	
	public static boolean existImage(String url) {
		Cursor cursor = cr.query(Images.CONTENT_URI, 
				new String[] {
					Images.ID
				}, 
				Images.URL + "=?",
				new String[] { url },
				null);
		boolean result = cursor.moveToFirst();
		cursor.close();
		return result;		
	}
	
	public static Image loadImage(long id) {
		Cursor cursor = cr.query(Images.CONTENT_URI, 
				new String[] {
					Images.ID,
					Images.URL,
					Images.STATUS
				}, 
				Images.ID + "=?",
				new String[] { String.valueOf(id) },
				null);
		Image image = null;
		if (cursor.moveToFirst()) {			
			image = new Image(cursor.getLong(0), cursor.getString(1), (byte)cursor.getInt(2));
		}
		cursor.close();
		return image;
	}
	
	public static Image loadImage(String url) {
		Cursor cursor = cr.query(Images.CONTENT_URI, 
				new String[] {
					Images.ID,
					Images.URL,
					Images.STATUS
				}, 
				Images.URL + "=?",
				new String[] { url },
				null);
		Image image = null;
		if (cursor.moveToFirst()) {			
			image = new Image(cursor.getLong(0), cursor.getString(1), (byte)cursor.getInt(2));			
		}
		cursor.close();
		return image;
	}
	
	public static void saveImage(Image image) {
		ContentValues values = new ContentValues();
		if (image.id == 0) {
			if (existImage(image.url)) return;
			
			values.put(Images.URL, image.url);
			values.put(Images.STATUS, image.status);
			values.put(Images.RETRIES, image.retries);
			Uri contentUri = cr.insert(Images.CONTENT_URI, values);
			image.id = ContentUris.parseId(contentUri);
		} else {
			values.put(Images.STATUS, image.status);
			values.put(Images.RETRIES, image.retries);
			cr.update(Images.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
					new String[] { String.valueOf(image.id) });
		}
	}
	
	public static void deleteImage(Image image) {		
		cr.delete(Images.CONTENT_URI, ContentsProvider.WHERE_ID, new String[] { String.valueOf(image.id) });
		image.id = 0;
	}
	
	public static long queueImage(String imageUrl) {
		Image image = loadImage(imageUrl);
		if (image != null) return image.id;		
		
		image = new Image(imageUrl, Image.IMAGE_STATUS_PENDING);
		saveImage(image);		
		return image.id;
	}
		
	public static void saveJob(Job job) {
		ContentValues values = new ContentValues();
		values.put(Jobs.TYPE, job.type);		
		values.put(Jobs.PARAMS, job.params);
		cr.insert(Jobs.CONTENT_URI, values);
	}
	
	public static List<Job> loadJobs(int maxItems) {
		Cursor cursor = cr.query(Jobs.limit(maxItems), 
				new String[] {		
					Jobs.ID,
					Jobs.TYPE,
					Jobs.PARAMS
				},
				null,
				null,
				Jobs.ID);
		List<Job> jobs = new ArrayList<Job>();
		while (cursor.moveToNext()) {			
			Job job = new Job();
			job.id = cursor.getLong(0);
			job.type = cursor.getString(1);
			job.params = cursor.getString(2);
			jobs.add(job);
		}
		cursor.close();
		return jobs;
	}
	
	public static void deleteJob(String type, String params) {
		cr.delete(Jobs.CONTENT_URI, 
				Jobs.TYPE + "=? AND " + Jobs.PARAMS + "=?", 
				new String[] { type, params });			
	}
	
	public static void deleteJob(long jobId) {		
		cr.delete(Jobs.CONTENT_URI, ContentsProvider.WHERE_ID, new String[] { String.valueOf(jobId) });
	}
	
	private static boolean isChannelInCache(long channelId) {
		return channelCache.containsKey(channelId);
	}
	
	private static void putChannelToCache(Channel channel) {
		if (!isChannelInCache(channel.id)) {
			channelCache.put(channel.id, new WeakReference<Channel>(channel));
		}
	}
	
	private static Channel getChannelFromCache(long id) {
		if (isChannelInCache(id)) {
			WeakReference<Channel> channelRef = channelCache.get(id);
			return channelRef.get();
		}
		return null;
	}		
}
