package vn.evolus.news.rss;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import vn.evolus.news.providers.ContentsProvider;
import vn.evolus.news.rss.Item.Items;
import vn.evolus.news.util.ActiveList;
import vn.evolus.news.util.ImageLoader;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class Channel extends Observable implements Serializable {	
	public static final int MAX_ITEMS = 20;
	
	private static final long serialVersionUID = 6204335219893986724L;	
	private static SAXParserFactory factory = SAXParserFactory.newInstance();
	private Object synRoot = new Object();
	
	private long id = 0;
	private String url;
	private String title;
	private String link;
	private String description;
	private String imageUrl;
	private int unread;
	private transient ActiveList<Item> items = new ActiveList<Item>();
	private boolean updating = false;	
	
	private Channel() {		
	}
	public Channel(String url) {			
		this("", url);
	}	
	public Channel(String title, String url) {		
		this.title = title;
		this.url = url;
	}	
		
	public long getId() {
		return id;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}	
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	public ActiveList<Item> getItems() {
		synchronized (items) {
			if (items == null) {
				 items = new ActiveList<Item>();
			}
			return items;
		}		
	}	
	public void clearItems() {
		getItems().clear();
	}	
	public boolean existItem(Item item) {		
		return this.getItems().indexOf(item) >= 0;
	}
	public void addItem(Item item) {
		synchronized (synRoot) {
			ActiveList<Item> items = this.getItems();
			if (items.indexOf(item) < 0) {
				// find insert location
				int position = 0;
				for (Item currentItem : this.items) {
					if (currentItem.getPubDate().before(item.getPubDate())) {
						items.add(position, item);
						return;
					}
					position++;
				}
				items.add(item);
			}
		}
	}
	public boolean isEmpty() {
		return this.getItems().size() == 0;
	}	
	public int countUnreadItems() {
		return this.unread;
	}	
	public void setUnreadItems(int unread) {
		this.unread = unread;
	}
	public boolean isUpdating() {
		synchronized (synRoot) {
			return updating;
		}
	}
	
	public void save(ContentResolver cr) {
		ContentValues values = new ContentValues();
		values.put(Channels.TITLE, this.title);
		values.put(Channels.URL, this.url);
		values.put(Channels.DESCRIPTION, this.description);
		values.put(Channels.LINK, this.link);
		values.put(Channels.IMAGE_URL, this.imageUrl);
		if (this.id == 0) {
			Uri contentUri = cr.insert(Channels.CONTENT_URI, values);
			this.id = ContentUris.parseId(contentUri);
		} else {
			cr.update(Channels.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
					new String[] { String.valueOf(this.id) });
		}		
		//saveItems(cr);
	}
	
	private void load(ContentResolver cr) {
		Cursor cursor = cr.query(Channels.CONTENT_URI, 
				new String[] {
					Channels.ID,
					Channels.TITLE,
					Channels.URL,
					Channels.DESCRIPTION,
					Channels.LINK,
					Channels.IMAGE_URL,
					Channels.UNREAD
				}, 
				ContentsProvider.WHERE_ID, 
				new String[] { String.valueOf(this.id) }, null);
		if (cursor.moveToFirst()) {
			load(cursor);
		}
		cursor.close();
	}
	
	public static Channel load(long id, ContentResolver cr) {		
		Channel channel = new Channel();
		channel.id = id;
		channel.load(cr);
		return channel;
	}
	
	public static ArrayList<Channel> loadAllChannels(ContentResolver cr) {
		Cursor cursor = cr.query(Channels.CONTENT_URI, 
				new String[] {
					Channels.ID,
					Channels.TITLE,
					Channels.URL,
					Channels.DESCRIPTION,
					Channels.LINK,
					Channels.IMAGE_URL//,
					//Channels.UNREAD
				}, 
				null, null, null);
		ArrayList<Channel> channels = new ActiveList<Channel>();
		while (cursor.moveToNext()) {
			Channel channel = new Channel();
			channel.load(cursor);
			channels.add(channel);
		}
		cursor.close();
		return channels;
	}
	
	public static Map<Long, Integer> countUnreadItems(ContentResolver cr) {
		Cursor cursor = cr.query(Items.UNREAD_COUNT_URI, 
				new String[] { Items.CHANNEL_ID, Items.UNREAD_COUNT }, 
				null, null, null);		
		Map<Long, Integer> unreadCounts = new HashMap<Long, Integer>();
		while (cursor.moveToNext()) {					
			unreadCounts.put(cursor.getLong(0), cursor.getInt(1));
		}
		cursor.close();
		return unreadCounts;
	}
	
	public int update(ContentResolver cr) {
		synchronized (synRoot) {
			if (updating) return 0;			
			updating = true;
			this.setChanged();
			this.notifyObservers(updating);
		}
		int newItems = 0;
		InputStream is = null;
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(this.url);			
			HttpResponse response = client.execute(get);
			is = response.getEntity().getContent();
			newItems = parse(is, cr);			
    	} catch (Throwable e) {
    		Log.e("ERROR", "Error on parsing " + this.getUrl() + ": " +  e.getMessage());
    	} finally {
    		if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				is = null;
			}
    		
    		//deleteStream();
    		
    		saveItems(cr);
    	}
    	
    	synchronized (synRoot) {
			updating = false;
			this.setChanged();
			this.notifyObservers(updating);
		}
    	
    	return newItems;
    }
	
	private int parse(InputStream is, ContentResolver cr) {
		long lastTicks = System.currentTimeMillis();
		// instantiate our handler
		RssHandler rssHandler = new RssHandler(this, cr);
		try {
			// create a parser
			SAXParser parser = factory.newSAXParser();
			// create the reader (scanner)
			XMLReader xmlReader = parser.getXMLReader();			
			// assign our handler
			xmlReader.setContentHandler(rssHandler);
			// perform the synchronous parse
			xmlReader.parse(new InputSource(is));
		} catch (Exception e) {
			e.printStackTrace();
		}			
		Log.d("DEBUG", "Parsing " + this.title + " in " + (System.currentTimeMillis() - lastTicks) + "ms: " + rssHandler.getNewItems() + " new items");
		
		Set<String> images = rssHandler.getImages();
		for (String imageUrl : images) {
			ImageLoader.start(imageUrl, null);
		}
		return rssHandler.getNewItems();
	}
	
	private void saveItems(ContentResolver cr) {
		if (items != null) {
			for (Item item : items) {
				item.save(cr);
			}
		}
	}
	
	public void loadLightweightItems(ContentResolver cr) {
		Item.loadAllItemsOfChannel(cr, this, true);
	}
	
	public void loadFullItems(ContentResolver cr) {
		Item.loadAllItemsOfChannel(cr, this, false);
	}
	
	private void load(Cursor cursor) {
		this.id = cursor.getLong(0);//cursor.getColumnIndex(Channels.ID));
		this.title = cursor.getString(1);//cursor.getColumnIndex(Channels.TITLE));
		this.url = cursor.getString(2);//cursor.getColumnIndex(Channels.URL));
		this.description = cursor.getString(3);//cursor.getColumnIndex(Channels.DESCRIPTION));
		this.link = cursor.getString(4);//cursor.getColumnIndex(Channels.LINK));
		this.imageUrl = cursor.getString(5);//cursor.getColumnIndex(Channels.IMAGE_URL));
		//this.unread = cursor.getInt(6);//cursor.getColumnIndex(Channels.UNREAD));
	}	
						
	public static final class Channels implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + ContentsProvider.AUTHORITY + "/channels");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.evolus.droidnews.channels";
		
		public static final String ID = "ID";
		public static final String TITLE = "TITLE";
		public static final String URL = "URL";
		public static final String LINK = "LINK";
		public static final String DESCRIPTION = "DESCRIPTION";
		public static final String IMAGE_URL = "IMAGE_URL";
		public static final String UNREAD = "UNREAD";
		
		private Channels() {			
		}
	}
}
