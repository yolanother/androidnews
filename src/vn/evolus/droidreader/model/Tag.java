package vn.evolus.droidreader.model;

import vn.evolus.droidreader.providers.ContentsProvider;
import android.net.Uri;
import android.provider.BaseColumns;

public class Tag {
	public static final byte STATE = 1;
	public static final byte LABEL = 2;
	public static final byte OTHER = 3;
	
	public int id;
	public byte type;
	public String name;
	public String sortId;
	public int unreadCount;
	
	public Tag(String readerTag) {
		parseTag(readerTag);
	}	
	
	public Tag(int id, byte type, String title) {
		this.id = id;
		this.type = type;
		this.name = title;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + type;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tag other = (Tag) obj;		
		if (this.id == other.id) return true;		
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	private void parseTag(String tag) {
		int pos = tag.indexOf("/label/");
		if (pos > 0) {
			this.type = LABEL;
			this.name = tag.substring(pos + 7);
		} 
		pos = tag.indexOf("/state/com.google");
		if (pos > 0) {
			this.type = STATE;
			this.name = tag.substring(pos + 18);
		}
		pos = tag.indexOf("/state/com.blogger");
		if (pos > 0) {
			this.type = OTHER;
			this.name = tag.substring(pos + 19);
		}		
	}
	
	public static final class Tags implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" 
				+ ContentsProvider.AUTHORITY + "/tags");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.evolus.droidreader.tags";
		
		public static final String ID = "ID";
		public static final String TYPE = "TYPE";
		public static final String NAME = "NAME";
		public static final String SORT_ID = "SORT_ID";
		
		public static final Uri limit(int limit) {
			return Uri.parse("content://" 
					+ ContentsProvider.AUTHORITY + "/tags/" + limit);
		}		
		private Tags() {
		}
	}
	
	public static final class TagsOfItems implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" 
				+ ContentsProvider.AUTHORITY + "/tagOfItems");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.evolus.droidreader.tagOfItems";
		
		public static final String ID = "ID";
		public static final String ITEM_ID = "ITEM_ID";
		public static final String ITEM_TYPE = "ITEM_TYPE";
		public static final String TAG_ID = "TAG_ID";
		
		public static final Uri limit(int limit) {
			return Uri.parse("content://" 
					+ ContentsProvider.AUTHORITY + "/tagOfItems/" + limit);
		}		
		private TagsOfItems() {
		}
	}
}
