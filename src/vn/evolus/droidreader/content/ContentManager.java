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
import vn.evolus.droidreader.content.loader.ChannelLoader;
import vn.evolus.droidreader.content.loader.FullChannelLoader;
import vn.evolus.droidreader.content.loader.FullItemLoader;
import vn.evolus.droidreader.content.loader.IdOnlyItemLoader;
import vn.evolus.droidreader.content.loader.LightweightChannelLoader;
import vn.evolus.droidreader.content.loader.LightweightItemLoader;
import vn.evolus.droidreader.content.loader.WithImageChannelLoader;
import vn.evolus.droidreader.content.processor.ImageItemProcessor;
import vn.evolus.droidreader.content.processor.VideoItemProcessor;
import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.model.Image;
import vn.evolus.droidreader.model.Item;
import vn.evolus.droidreader.model.Job;
import vn.evolus.droidreader.model.SyncItemTagJob;
import vn.evolus.droidreader.model.Tag;
import vn.evolus.droidreader.model.Channel.Channels;
import vn.evolus.droidreader.model.Image.Images;
import vn.evolus.droidreader.model.Item.Items;
import vn.evolus.droidreader.model.Job.Jobs;
import vn.evolus.droidreader.model.Tag.Tags;
import vn.evolus.droidreader.model.Tag.TagsOfItems;
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
	public static final int ALL_CHANNELS = -1;
		
	public static final int ITEM_TYPE_CHANNEL = 1;
	public static final int ITEM_TYPE_ITEM = 2;
	
	public static final ChannelLoader FULL_CHANNEL_LOADER = new FullChannelLoader();
	public static final ChannelLoader LIGHTWEIGHT_CHANNEL_LOADER = new LightweightChannelLoader();
	public static final ChannelLoader WITH_IMAGE_CHANNEL_LOADER = new WithImageChannelLoader();
	public static final ItemLoader FULL_ITEM_LOADER = new FullItemLoader();
	public static final ItemLoader LIGHTWEIGHT_ITEM_LOADER = new LightweightItemLoader();
	public static final ItemLoader ID_ONLY_ITEM_LOADER = new IdOnlyItemLoader();
	
	private static final Map<Integer, WeakReference<Channel>> channelCache = 
		new HashMap<Integer, WeakReference<Channel>>();
	private static final Map<Tag, Integer> tagCache = new HashMap<Tag, Integer>();
	
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
			channel.id = (int)ContentUris.parseId(contentUri);
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
	
	public static Channel loadChannel(int id, ChannelLoader loader) {
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
	
	public static void cleanUp(int keepMaxItems) {
		Cursor cursor = cr.query(Items.limitAndStartAt(1, keepMaxItems - 1),
				new String[] { Items.ID, Items.PUB_DATE, Items.UPDATE_TIME },
				null,
				null,
				Items.UPDATE_TIME + " DESC, " +
				Items.PUB_DATE + " DESC, " +
				Items.ID + " ASC");
		if (cursor.moveToNext()) {			
			long id = cursor.getLong(0);
			long lastPubDate = cursor.getLong(1);
			long updateTime = cursor.getLong(2);
			cursor.close();
			
			String selection = Items.KEPT + "=0 AND ("				
				+ Items.UPDATE_TIME + "<? OR (" + Items.UPDATE_TIME + "=? AND (" 
				+ Items.PUB_DATE + "<? OR (" + Items.PUB_DATE + " =? AND " + Items.ID + ">?))))";
			cr.delete(Items.CONTENT_URI,
					selection,
					new String[] {											
						String.valueOf(updateTime),
						String.valueOf(updateTime),
						String.valueOf(lastPubDate),
						String.valueOf(lastPubDate),
						String.valueOf(id) });
		}				
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
			String selection = Items.KEPT + "=0 AND " 
				+ Items.CHANNEL_ID + "=? AND (" 
				+ Items.UPDATE_TIME + "<? OR (" + Items.UPDATE_TIME + "=? AND (" 
				+ Items.PUB_DATE + "<? OR (" + Items.PUB_DATE + " =? AND " + Items.ID + ">?))))";
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
						
			ContentManager.processItem(item);			
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
			item.id = (int)ContentUris.parseId(contentUri);
			
			if (item.tags != null) {
				saveItemTags(item);
			}
		} else {
			values.put(Items.READ, item.read);
			cr.update(Items.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
					new String[] { String.valueOf(item.id) });			
		}
		return true;
	}
	
	public static void saveItemTags(Item item) {
		deleteAllTagsOfItem(item.id);
		for (String tag : item.tags) {					
			saveItemTag(item, tag, false);
		}		
	}

	private static void saveItemTag(Item item, String tag) {
		saveItemTag(item, tag, true);
	}
	
	private static void saveItemTag(Item item, String tag, boolean checkExist) {
		int tagId = getTagId(tag);
		if (tagId > 0) {
			if (checkExist && existItemTag(item.id, tag)) return;
			
			ContentValues values = new ContentValues();
			values.put(TagsOfItems.ITEM_TYPE, ITEM_TYPE_ITEM);
			values.put(TagsOfItems.ITEM_ID, item.id);
			values.put(TagsOfItems.TAG_ID, tagId);
			cr.insert(TagsOfItems.CONTENT_URI, values);
		}
	}
	
	private static void deleteItemTag(int itemId, String tag) {
		int tagId = getTagId(tag);
		cr.delete(TagsOfItems.CONTENT_URI, 				
				TagsOfItems.ITEM_TYPE + "=? AND " +
				TagsOfItems.ITEM_ID + "=? AND " +
				TagsOfItems.TAG_ID + "=?", 
				new String[] {
					String.valueOf(ITEM_TYPE_ITEM),
					String.valueOf(itemId),
					String.valueOf(tagId)
				});
	}
	
	private static void deleteAllTagsOfItem(int itemId) {		
		cr.delete(TagsOfItems.CONTENT_URI, 				
				TagsOfItems.ITEM_TYPE + "=? AND " +
				TagsOfItems.ITEM_ID + "=?", 
				new String[] {
					String.valueOf(ITEM_TYPE_ITEM),
					String.valueOf(itemId)					
				});
	}
	
	private static boolean existItemTag(int itemId, String tag) {
		int tagId = getTagId(tag);
		Cursor cursor = cr.query(TagsOfItems.CONTENT_URI, 
				new String[] { TagsOfItems.ID }, 
				TagsOfItems.ITEM_TYPE + "=? AND " +
				TagsOfItems.ITEM_ID + "=? AND " +
				TagsOfItems.TAG_ID + "=?", 
				new String[] {
					String.valueOf(ITEM_TYPE_ITEM),
					String.valueOf(itemId),
					String.valueOf(tagId)
				}, null);
		boolean result = false;
		if (cursor.moveToNext()) {
			result = true;
		}
		cursor.close();
		return result;
	}

	public static Item loadItem(int id, ItemLoader loader, ChannelLoader channelLoader) {		
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
		saveItemReadState(item, Item.READ);
	}
	
	public static void saveItemReadState(Item item, int readState) {
		if (item.isRead()) return;
		
		item.read = Item.READ;
		ContentValues values = new ContentValues();
		values.put(Items.READ, readState);
		cr.update(Items.CONTENT_URI, values, ContentsProvider.WHERE_ID,
				new String[] { String.valueOf(item.id) });
				
		addTagToItem(item, GoogleReader.ITEM_STATE_READ);
	}		
	
	public static void markAllTemporarilyMarkedReadItemsAsRead() {
		ContentValues values = new ContentValues();
		values.put(Items.READ, Item.READ);
		cr.update(Items.CONTENT_URI, values, Items.READ + "=" + Item.TEMPORARILY_MARKED_AS_READ, null);
	}
	
	public static void markItemAsStarred(Item item) {
		if (item.starred) return;
		
		// saving state to DB
		item.starred = true;
		item.kept = true;		
		ContentValues values = new ContentValues();
		values.put(Items.STARRRED, 1);
		values.put(Items.KEPT, 1);
		cr.update(Items.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
				new String[] { String.valueOf(item.id) });
		
		addTagToItem(item, GoogleReader.ITEM_STATE_STARRED);
	}
	
	public static void unmarkItemAsStarred(Item item) {
		if (!item.starred) return;
		
		// saving state to DB
		item.starred = false;
		item.kept = false;
		ContentValues values = new ContentValues();
		values.put(Items.STARRRED, 0);
		values.put(Items.KEPT, 0);
		cr.update(Items.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
				new String[] { String.valueOf(item.id) });
		
		removeTagFromItem(item, GoogleReader.ITEM_STATE_STARRED);		
	}
	
	public static void markItemAsSharred(Item item) {
		// saving state to DB
		item.kept = true;		
		ContentValues values = new ContentValues();		
		values.put(Items.KEPT, 1);
		cr.update(Items.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
				new String[] { String.valueOf(item.id) });
		
		addTagToItem(item, GoogleReader.ITEM_STATE_STARRED);
	}
	
	public static void unmarkItemAsSharred(Item item) {
		// saving state to DB
		item.kept = false;
		ContentValues values = new ContentValues();		
		values.put(Items.KEPT, 0);
		cr.update(Items.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
				new String[] { String.valueOf(item.id) });
		
		removeTagFromItem(item, GoogleReader.ITEM_STATE_SHARED);		
	}
	
	public static void markItemAsKeptUnread(Item item) {
		// saving state to DB
		item.kept = true;
		item.read = Item.KEPT_UNREAD;
		ContentValues values = new ContentValues();		
		values.put(Items.KEPT, item.kept);
		values.put(Items.READ, item.read);
		cr.update(Items.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
				new String[] { String.valueOf(item.id) });
		
		addTagToItem(item, GoogleReader.ITEM_STATE_KEPT_UNREAD);		
	}
	
	public static void unmarkItemAsKeptUnread(Item item) {
		// saving state to DB
		item.kept = false;
		item.read = Item.READ;
		ContentValues values = new ContentValues();
		values.put(Items.KEPT, item.kept);
		values.put(Items.READ, item.read);
		cr.update(Items.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
				new String[] { String.valueOf(item.id) });
		
		removeTagFromItem(item, GoogleReader.ITEM_STATE_KEPT_UNREAD);		
	}
	
	public static void addTagToItem(Item item, String tag) {
		// delete previous REMOVE tag job
		SyncItemTagJob removeJob = new SyncItemTagJob(SyncItemTagJob.ACTION_REMOVE, 
				item.originalId, tag);
		deleteJob(removeJob.type, removeJob.params);
		
		saveItemTag(item, tag);
				
		// create a new ADD tag job
		SyncItemTagJob addJob = new SyncItemTagJob(SyncItemTagJob.ACTION_ADD, 
				item.originalId, tag);
		saveJob(addJob);
	}
	
	public static void removeTagFromItem(Item item, String tag) {
		// delete previous ADD tag job
		SyncItemTagJob addJob = new SyncItemTagJob(SyncItemTagJob.ACTION_ADD, 
				item.originalId, tag);
		deleteJob(addJob.type, addJob.params);
		
		deleteItemTag(item.id, tag);
				
		// create a new REMOVE tag job
		SyncItemTagJob removeJob = new SyncItemTagJob(SyncItemTagJob.ACTION_REMOVE, 
				item.originalId, tag);
		saveJob(removeJob);
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
	
	public static List<Item> loadItems(ItemCriteria criteria, ItemLoader loader, ChannelLoader channelLoader) {
		Cursor cursor = cr.query(criteria.getContentUri(),
				loader.getProjection(),
				criteria.getSelection(),
				criteria.getSelectionArgs(),
				criteria.getOrderBy());
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
			item.id = cursor.getInt(0);
			item.read = Item.UNREAD;
			item.originalId = cursor.getString(1);
			markItemAsRead(item);
		}
		for (Item channelItem : channel.getItems()) {
			channelItem.read = Item.READ;
		}
		cursor.close();
	}
	
	/**
	 * 
	 * @param cr
	 * @return a map of ChannelId <-> Unread count
	 */
	public static Map<Integer, Integer> countUnreadItemsForEachChannel() {
		Cursor cursor = cr.query(Items.countUnreadEachChannel(), 
				new String[] { Items.CHANNEL_ID, Items.UNREAD_COUNT }, 
				Items.READ + "=? OR " + Items.READ + "=?", 
				new String[] { String.valueOf(Item.UNREAD), String.valueOf(Item.KEPT_UNREAD)},
				null);
		Map<Integer, Integer> unreadCounts = new HashMap<Integer, Integer>();
		while (cursor.moveToNext()) {					
			unreadCounts.put(cursor.getInt(0), cursor.getInt(1));
		}
		cursor.close();
		return unreadCounts;
	}
	
	public static int countUnreadItems() {
		Cursor cursor = cr.query(Items.countUnread(), 
				new String[] { Items.UNREAD_COUNT }, 
				Items.READ + "=? OR " + Items.READ + "=?", 
				new String[] { String.valueOf(Item.UNREAD), String.valueOf(Item.KEPT_UNREAD)},
				null);
		int unreadCounts = 0;
		if (cursor.moveToNext()) {					
			unreadCounts = cursor.getInt(0);
		}
		cursor.close();
		return unreadCounts;
	}
		
	public static int countItems(ItemCriteria criteria) {
		Cursor cursor = cr.query(criteria.getContentUri(),
				new String[] { Items.COUNT },
				criteria.getSelection(),
				criteria.getSelectionArgs(),
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
			Image image = new Image(cursor.getInt(0), cursor.getString(1), (byte)cursor.getInt(2));
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
	
	public static Image loadImage(int id) {
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
			image = new Image(cursor.getInt(0), cursor.getString(1), (byte)cursor.getInt(2));
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
			image = new Image(cursor.getInt(0), cursor.getString(1), (byte)cursor.getInt(2));			
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
			image.id = (int)ContentUris.parseId(contentUri);
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
	
	private static Channel getChannelFromCache(int id) {
		if (isChannelInCache(id)) {
			WeakReference<Channel> channelRef = channelCache.get(id);
			return channelRef.get();
		}
		return null;
	}

	public static List<Tag> loadAllTags() {
		// count unread
		Cursor cursor = cr.query(Items.countUnreadOfTag(), 
				new String[] { Items.TAG_ID, Items.UNREAD_COUNT }, 
				Items.READ + "=? OR " + Items.READ + "=?", 
				new String[] { String.valueOf(Item.UNREAD), String.valueOf(Item.KEPT_UNREAD)}, 
				null);
		Map<Integer, Integer> unreadCounts = new HashMap<Integer, Integer>();
		while (cursor.moveToNext()) {
			unreadCounts.put(cursor.getInt(0), cursor.getInt(1));
		}
		cursor.close();	
				
		cursor = cr.query(Tags.CONTENT_URI, 
				new String[] {		
					Tags.ID,
					Tags.TYPE,
					Tags.NAME
				},
				null,
				null,
				Tags.SORT_ID);
		List<Tag> tags = new ArrayList<Tag>();
		while (cursor.moveToNext()) {			
			Tag tag = new Tag(cursor.getInt(0), (byte)cursor.getInt(1), cursor.getString(2));
			if (unreadCounts.containsKey(tag.id)) {
				tag.unreadCount = unreadCounts.get(tag.id); 
			}
			tags.add(tag);
		}
		cursor.close();
		return tags;
	}
	
	public static Tag loadTag(int tagId) {
		Cursor cursor = cr.query(Tags.CONTENT_URI,
				new String[] {		
					Tags.ID,
					Tags.TYPE,
					Tags.NAME
				},
				ContentsProvider.WHERE_ID,
				new String[] { String.valueOf(tagId) },
				Tags.SORT_ID);
		Tag tag = null;
		if (cursor.moveToNext()) {			
			tag = new Tag(cursor.getInt(0), (byte)cursor.getInt(1), cursor.getString(2));			
		}
		cursor.close();
		return tag;
	}

	public static void saveTag(Tag tag) {
		ContentValues values = new ContentValues();
		values.put(Tags.TYPE, tag.type);
		values.put(Tags.NAME, tag.name);
		values.put(Tags.SORT_ID, tag.sortId);
		cr.insert(Tags.CONTENT_URI, values);
	}

	public static void deleteTag(int tagId) {
		for (Tag tag : tagCache.keySet()) {
			if (tag.id == tagId) {
				tagCache.remove(tag);
				break;
			}
		}
		cr.delete(Tags.CONTENT_URI, ContentsProvider.WHERE_ID, new String[] { String.valueOf(tagId) });
	}
	
	private static int getTagId(String tagName) {			
		ensureTagCacheAvailable();
		Tag tag = new Tag(tagName);
		if (tagCache.containsKey(tag)) {
			return tagCache.get(tag);
		}
		return 0;
	}	
	
	private static void ensureTagCacheAvailable() {
		if (tagCache.size() == 0) {
			List<Tag> allTags = loadAllTags();
			for (Tag tag : allTags) {
				tagCache.put(tag, tag.id);
			}
		}
	}
}
