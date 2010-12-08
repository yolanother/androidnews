package vn.evolus.droidreader.model;

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.model.Tag.TagsOfItems;
import vn.evolus.droidreader.providers.ContentsProvider;
import vn.evolus.droidreader.util.StreamUtils;
import android.net.Uri;
import android.provider.BaseColumns;

import com.google.reader.atom.Entry;

public class Item implements Serializable {	
	private static final long serialVersionUID = 1783666956248831428L;
	
	private static final String MOBILIZE_SERVICE_API = "http://droidreader.appspot.com/mobilize/?url=";
		
	public static final int UNREAD = 0;
	public static final int READ = 1;
	public static final int TEMPORARILY_MARKED_AS_READ = 2;
	public static final int KEPT_UNREAD = 3;

	public int id;
	public String title;
	public String description;
	public Date pubDate;
	public String link;
	public String imageUrl;
	public int read;
	public boolean starred;
	public boolean shared;
	public boolean kept;
	public Channel channel;
	public long updateTime;
	public String originalId;
	public List<String> tags;
		
	public Item() {
		this.id = 0;
		this.read = UNREAD;
		this.pubDate = new Date(System.currentTimeMillis());
	}		
	
	public Item(int id) {
		this();
		this.id = id;
	}
	
	public boolean isRead() {
		return this.read == READ;
	}
	
	public boolean isKeptUnread() {
		return this.read == KEPT_UNREAD;
	}
	
	public static Item fromEntry(Entry entry) {
		Item item = new Item();
		item.link = entry.getLink();
		item.originalId = entry.getId();
		item.title = entry.getTitle();
		
		item.description = entry.getContent();
		if (item.description == null) {
			item.description = entry.getSummary();
		}
		item.pubDate = entry.getUpdated();
		item.read = entry.getKeptUnread() ? KEPT_UNREAD : (entry.getRead() ? READ : UNREAD);
		item.starred = entry.getStarred();
		item.shared = entry.getShared();
		item.kept = entry.getKeptUnread() || entry.getStarred();
		
		if (entry.getTags().size() > 0) {
			item.tags = entry.getTags();					
		}
		return item;
	}
	
	public boolean mobilize() {
		String url = MOBILIZE_SERVICE_API + URLEncoder.encode(this.link);
		String content = StreamUtils.readFromUrl(url, "UTF-8");
		if (content != null) {
			this.description = content;
			ContentManager.processItem(this);				
			return true;			
		}
		return false;
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
				
	public static final class Items implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" 
				+ ContentsProvider.AUTHORITY + "/items");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.evolus.droidreader.items";
		
		public static final String ID = "ID";
		public static final String TITLE = "TITLE";
		public static final String DESCRIPTION = "DESCRIPTION";
		public static final String PUB_DATE = "PUB_DATE";
		public static final String LINK = "LINK";
		public static final String IMAGE_URL = "IMAGE_URL";
		public static final String READ = "READ";
		public static final String STARRRED = "STARRED";
		public static final String KEPT = "KEPT";
		public static final String CHANNEL_ID = "CHANNEL_ID";
		public static final String UPDATE_TIME = "UPDATE_TIME";
		public static final String ORIGINAL_ID = "ORIGINAL_ID";
		public static final String UNREAD_COUNT = "UNREAD";
		public static final String COUNT = "COUNT(DISTINCT ID)";
		public static final String TAG_ID = TagsOfItems.TAG_ID;
		
		public static final Uri hasTagAndLimit(int limit) {
			return Uri.parse("content://" 
					+ ContentsProvider.AUTHORITY + "/items/tag/" + limit);
		}
		public static final Uri count() {
			return Uri.parse("content://" 
					+ ContentsProvider.AUTHORITY + "/items/count");
		}
		public static final Uri countUnreadEachChannel() {
			return Uri.parse("content://" 
					+ ContentsProvider.AUTHORITY + "/items/unread");
		}
		public static final Uri countUnread() {
			return Uri.parse("content://" 
					+ ContentsProvider.AUTHORITY + "/items/unread/all");
		}
		public static final Uri countUnreadOfTag() {
			return Uri.parse("content://" 
					+ ContentsProvider.AUTHORITY + "/items/tag/unread");
		}
		public static final Uri limit(int limit) {
			return Uri.parse("content://" 
					+ ContentsProvider.AUTHORITY + "/items/" + limit);
		}
		
		public static final Uri limitAndStartAt(int limit, int offset) {
			return Uri.parse("content://" 
					+ ContentsProvider.AUTHORITY + "/items/" + limit + "/" + offset);
		}
		
		private Items() {
		}
	}
}
