package vn.evolus.news.rss;

import java.io.Serializable;
import java.util.Date;

import vn.evolus.news.providers.ContentsProvider;
import vn.evolus.news.util.ActiveList;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class Item implements Serializable {	
	private static final long serialVersionUID = 1783666956248831428L;

	private static final ItemLoader FULL_ITEM_LOADER = new FullItemLoader();
	private static final ItemLoader LIGHTWEIGHT_ITEM_LOADER = new LightweightItemLoader();
	
	private long id;
	private String title;
	private String description;
	private Date pubDate;
	private String link;
	private String imageUrl;	
	private boolean read;
	private Channel channel;
		
	public Item() {
		this.id = 0;
		this.read = false;
		this.pubDate = new Date(System.currentTimeMillis());
	}		
	public Item(long id) {
		this();
		this.id = id;
	}
	public long getId() {
		return id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;		
	}		
	public Date getPubDate() {
		return pubDate;
	}
	public void setPubDate(Date pubDate) {
		this.pubDate = pubDate;
	}
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {		
		this.imageUrl = imageUrl;
	}		
	public boolean getRead() {
		return read;
	}
	public void setRead(boolean read) {
		this.read = read;
	}
	public Channel getChannel() {
		return channel;
	}
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	@Override
	public int hashCode() {		
		return 31 + ((link == null) ? 0 : link.hashCode());		
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Item other = (Item) obj;
		
		if (id != 0 && other.id != 0) {
			return id == other.id;
		}
		if (link == null) {
			if (other.link != null)
				return false;
		} else if (!link.equals(other.link))
			return false;
		return true;
	}	
	
	public void save(ContentResolver cr) {
		ContentValues values = new ContentValues();
		if (this.id == 0) {
			if (hasExist(cr, this)) return;
			
			values.put(Items.TITLE, this.title);
			values.put(Items.DESCRIPTION, this.description);
			values.put(Items.PUB_DATE, this.pubDate.getTime());
			values.put(Items.LINK, this.link);
			values.put(Items.IMAGE_URL, this.imageUrl);
			values.put(Items.READ, this.read);
			values.put(Items.CHANNEL_ID, this.channel.getId());
			Uri contentUri = cr.insert(Items.CONTENT_URI, values);
			this.id = ContentUris.parseId(contentUri);
		} else {
			values.put(Items.READ, this.read);
			cr.update(Items.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
					new String[] { String.valueOf(this.id) });
		}
	}	
	
	public static boolean hasExist(ContentResolver cr, Item item) {
		Cursor cursor = cr.query(Items.CONTENT_URI, 
				new String[] {
					Items.ID
				}, 
				Items.LINK + "=?",
				new String[] { item.getLink() },
				null);
		boolean result = cursor.moveToFirst();
		cursor.close();
		return result;
	}
	public static void loadAllItemsOfChannel(ContentResolver cr, Channel channel, boolean lightweight) {
		ItemLoader loader = lightweight ? LIGHTWEIGHT_ITEM_LOADER : FULL_ITEM_LOADER;
		Cursor cursor = cr.query(Items.CONTENT_URI,
				loader.getProjection(),
				Items.CHANNEL_ID + "=?", 
				new String[] { String.valueOf(channel.getId()) }, 
				Items.PUB_DATE + " DESC");
		ActiveList<Item> items = channel.getItems();
		items.clear();
		while (cursor.moveToNext()) {						
			Item item = new Item();
			item.setChannel(channel);
			loader.load(cursor, item);
			items.add(item);			
		}
		cursor.close();
	}		
	
	public interface ItemLoader {
		String[] getProjection();
		void load(Cursor cursor, Item item);
	}
	
	public static final class FullItemLoader implements ItemLoader {
		public String[] getProjection() {
			return new String[] {
					Items.ID,
					Items.TITLE,
					Items.DESCRIPTION,
					Items.PUB_DATE,
					Items.LINK,
					Items.IMAGE_URL,
					Items.READ
				};
		}
		public void load(Cursor cursor, Item item) {	
			// using magic numbers !!!
			item.id = cursor.getLong(0);//cursor.getColumnIndex(Items.ID));
			item.title = cursor.getString(1);//cursor.getColumnIndex(Items.TITLE));
			item.description = cursor.getString(2);//cursor.getColumnIndex(Items.DESCRIPTION));		
			item.pubDate = new Date(cursor.getLong(3));//cursor.getColumnIndex(Items.DESCRIPTION));
			item.link = cursor.getString(4);//cursor.getColumnIndex(Items.DESCRIPTION));
			item.imageUrl = cursor.getString(5);//cursor.getColumnIndex(Items.DESCRIPTION));
			item.read = cursor.getInt(6) > 0;//cursor.getColumnIndex(Items.DESCRIPTION));
		}
	}

	public static final class LightweightItemLoader implements ItemLoader {
		public String[] getProjection() {
			return new String[] {
					Items.ID,
					Items.TITLE,
					Items.PUB_DATE,					
					Items.IMAGE_URL,
					Items.READ
				};
		}
		public void load(Cursor cursor, Item item) {	
			// using magic numbers !!!
			item.id = cursor.getLong(0);//cursor.getColumnIndex(Items.ID));
			item.title = cursor.getString(1);//cursor.getColumnIndex(Items.TITLE));
			item.pubDate = new Date(cursor.getLong(2));//cursor.getColumnIndex(Items.DESCRIPTION));
			item.imageUrl = cursor.getString(3);//cursor.getColumnIndex(Items.DESCRIPTION));
			item.read = cursor.getInt(4) > 0;//cursor.getColumnIndex(Items.DESCRIPTION));
		}
	}
	
	public static final class Items implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" 
				+ ContentsProvider.AUTHORITY + "/items");
		public static final Uri UNREAD_COUNT_URI = Uri.parse("content://" 
				+ ContentsProvider.AUTHORITY + "/items/unread");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.evolus.droidnews.items";
		
		public static final String ID = "ID";
		public static final String TITLE = "TITLE";
		public static final String DESCRIPTION = "DESCRIPTION";
		public static final String PUB_DATE = "PUB_DATE";
		public static final String LINK = "LINK";
		public static final String IMAGE_URL = "IMAGE_URL";
		public static final String READ = "READ";
		public static final String CHANNEL_ID = "CHANNEL_ID";
		public static final String UNREAD_COUNT = "UNREAD";
		
		private Items() {
		}
	}
}
