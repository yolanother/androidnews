package vn.evolus.droidnews.content;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.evolus.droidnews.Application;
import vn.evolus.droidnews.content.processors.ImageItemProcessor;
import vn.evolus.droidnews.content.processors.VideoItemProcessor;
import vn.evolus.droidnews.model.Channel;
import vn.evolus.droidnews.model.Image;
import vn.evolus.droidnews.model.Item;
import vn.evolus.droidnews.model.Channel.Channels;
import vn.evolus.droidnews.model.Image.Images;
import vn.evolus.droidnews.model.Item.Items;
import vn.evolus.droidnews.providers.ContentsProvider;
import vn.evolus.droidnews.util.ActiveList;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class ContentManager {
	public static final ChannelLoader FULL_CHANNEL_LOADER = new FullChannelLoader();
	public static final ChannelLoader LIGHTWEIGHT_CHANNEL_LOADER = new LightweightChannelLoader();
	public static final ChannelLoader WITH_IMAGE_CHANNEL_LOADER = new WithImageChannelLoader();
	public static final ItemLoader FULL_ITEM_LOADER = new FullItemLoader();
	public static final ItemLoader LIGHTWEIGHT_ITEM_LOADER = new LightweightItemLoader();
	
	private static List<ItemProcessor> itemProcessors;
	
	private static ContentResolver cr;
	
	static {
		cr = Application.getInstance().getContentResolver();
		itemProcessors = new ArrayList<ItemProcessor>();
		itemProcessors.add(new ImageItemProcessor());
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
				new String[] { Items.PUB_DATE },
				Items.CHANNEL_ID + "=?", 
				new String[] { String.valueOf(channel.id) },
				Items.PUB_DATE + " DESC");		
		while (cursor.moveToNext()) {
			long lastPubDate = cursor.getLong(0);						
			cr.delete(Items.CONTENT_URI, Items.CHANNEL_ID + "=? AND " + Items.PUB_DATE + " < ?",					
					new String[] {
						String.valueOf(channel.id),
						String.valueOf(lastPubDate) });
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
	
	public static void saveItem(Item item) {
		ContentValues values = new ContentValues();
		if (item.id == 0) {
			if (existItem(item)) return;						
						
			values.put(Items.TITLE, item.title);
			values.put(Items.DESCRIPTION, item.description);
			values.put(Items.PUB_DATE, item.pubDate.getTime());
			values.put(Items.LINK, item.link);
			values.put(Items.IMAGE_URL, item.imageUrl);
			values.put(Items.READ, item.read);
			values.put(Items.CHANNEL_ID, item.channel.id);
			Uri contentUri = cr.insert(Items.CONTENT_URI, values);
			item.id = ContentUris.parseId(contentUri);
		} else {
			values.put(Items.READ, item.read);
			cr.update(Items.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
					new String[] { String.valueOf(item.id) });
		}
	}
	
	public static Item loadItem(long id) {		
		ItemLoader loader = FULL_ITEM_LOADER;
		Cursor cursor = cr.query(Items.CONTENT_URI,
				loader.getProjection(),
				Items.ID + "=?", 
				new String[] { String.valueOf(id) }, 
				null);
		Item item = null;
		if (cursor.moveToNext()) {						
			item = loader.load(cursor);
		}
		cursor.close();
		return item;
	}
	
	public static void processItem(Item item) {
		for (ItemProcessor processor : itemProcessors) {
			processor.process(item);
		}
	}
	
	public static boolean existItem(Item item) {
		Cursor cursor = cr.query(Items.CONTENT_URI, 
				new String[] {
					Items.ID
				}, 
				Items.LINK + "=?",
				new String[] { item.link },
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
				Items.PUB_DATE + " DESC");
		ActiveList<Item> items = channel.getItems();
		items.clear();
		while (cursor.moveToNext()) {						
			Item item = loader.load(cursor);
			item.channel = channel;			
			items.add(item);
		}
		cursor.close();
	}
	
	public static ActiveList<Item> loadLatestItems(int maxItems, 
			ItemLoader loader, ChannelLoader channelLoader) {
		Cursor cursor = cr.query(Items.limit(maxItems),
				loader.getProjection(),
				null,//Items.READ + "=?",
				null,//new String[] { String.valueOf(0) },
				Items.READ + " ASC, " +
				Items.PUB_DATE + " DESC");
		ActiveList<Item> items = new ActiveList<Item>();
		items.clear();
		while (cursor.moveToNext()) {						
			Item item = loader.load(cursor);
			item.channel = loadChannel(item.channel.id, channelLoader);
			items.add(item);			
		}
		cursor.close();
		return items;
	}

	public static void markAllItemsOfChannelAsRead(Channel channel) {
		ContentValues values = new ContentValues();
		values.put(Items.READ, true);
		cr.update(Items.CONTENT_URI, values, Items.CHANNEL_ID + "=?", 
				new String[] { String.valueOf(channel.id) });
		for (Item item : channel.getItems()) {
			item.read = true;
		}
	}
	
	/**
	 * 
	 * @param cr
	 * @return a map of ChannelId <-> Unread count
	 */
	public static Map<Long, Integer> countUnreadItemsForEachChannel(ContentResolver cr) {
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
	
	public static int countUnreadItems(ContentResolver cr) {
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
	
	public static ArrayList<Image> loadAllQueuedImages(ContentResolver cr) {
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
	
	public static boolean existImage(String url, ContentResolver cr) {
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
			if (existImage(image.url, cr)) return;
			
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
	
	public void delete(Image image) {		
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
