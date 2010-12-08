package vn.evolus.droidreader.model;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Observable;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import vn.evolus.droidreader.Constants;
import vn.evolus.droidreader.GoogleReaderFactory;
import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.providers.ContentsProvider;
import vn.evolus.droidreader.rss.RssHandler;
import vn.evolus.droidreader.util.ActiveList;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.reader.GoogleReader;
import com.google.reader.atom.AtomFeed;
import com.google.reader.atom.Entry;
import com.google.reader.atom.AtomHandler.OnNewEntryCallback;

public class Channel extends Observable implements Serializable {
	public static final int OPTIONS_MOBILIZE = 0x1;
	private static final long serialVersionUID = 6204335219893986724L;
	
	private static SAXParserFactory factory = SAXParserFactory.newInstance();
	private Object synRoot = new Object();
	
	public int id = 0;
	public String url;
	public String title;
	public String link;
	public String description;
	public String imageUrl;
	public long options;
	public int unread;
	private transient ActiveList<Item> items = new ActiveList<Item>();
	private boolean updating = false;	
	
	public Channel() {
		this.options = 0;
	}
	public Channel(String url) {
		this("", url);
	}	
	public Channel(String title, String url) {		
		this.title = title;
		this.url = url;
		this.options = 0;
	}
	public Channel(int id) {			
		this.id = id;
		this.options = 0;
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
		item.channel = this;
		
		synchronized (synRoot) {
			ActiveList<Item> items = this.getItems();
			if (items.indexOf(item) < 0) {
				// find insert location
				int position = 0;
				for (Item currentItem : this.items) {
					if (currentItem.pubDate.before(item.pubDate)) {
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
	public void setOptions(int options) {
		this.options = this.options | options;
	}
	public void unsetOptions(int options) {		
		this.options = this.options & ~options;
	}
	public boolean hasOptions(int options) {		
		return (this.options & options) > 0;
	}
	public boolean isUpdating() {
		synchronized (synRoot) {
			return updating;
		}
	}	
	public int update(int maxItems) {
		return update(maxItems, System.currentTimeMillis());
	}
	
	public int update(int maxItems, long timestamp) {
		synchronized (synRoot) {
			if (updating) return 0;
			updating = true;
			this.setChanged();
			this.notifyObservers(updating);
		}
		
		updateItems(maxItems);
		if (hasOptions(OPTIONS_MOBILIZE)) {
			mobilizeItems();
		}
		int newItems = saveItems(timestamp);
    	
    	synchronized (synRoot) {
			updating = false;
			this.setChanged();
			this.notifyObservers(updating);
		}
    	
    	return newItems;
    }
	
	protected void updateItems(int maxItems) {
		try {
			GoogleReader reader = GoogleReaderFactory.getGoogleReader();
			String continuation = null;
			int numberOfFetchedItems = 0;
			while (true) {
				AtomFeed feed = reader.fetchEntriesOfFeed(this.url, Constants.MAX_ITEMS_PER_FETCH, 
						continuation, 
						new OnNewEntryCallback() {
							public void onNewEntry(Entry entry) {
								if (ContentManager.existItem(entry.getLink())) {
									throw new RuntimeException("Found exist item. Stop parsing " + Channel.this.title);
								}
							}					
				});
				List<Entry> entries = feed.getEntries();			
				for (Entry entry : entries) {
					Item item = Item.fromEntry(entry);
					this.addItem(item);
				}
				continuation = feed.getContinution();
				numberOfFetchedItems += feed.getEntries().size();
				if (numberOfFetchedItems >= maxItems ||
						feed.getEntries().size() < Constants.MAX_ITEMS_PER_FETCH ||
						continuation == null) {
					break;
				}
				
				try {
					Thread.yield();
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (RuntimeException e) {
			Log.e("ERROR", e.getMessage());
			//e.printStackTrace();		
		} catch (Throwable e) {
			Log.e("ERROR", e.getMessage());
			//e.printStackTrace();
		}				
	}
	
	protected int parse(InputStream is, int maxItems) {
		// instantiate our handler
		RssHandler rssHandler = new RssHandler(this, maxItems);
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
		return rssHandler.getNewItems();
	}
	
	private void mobilizeItems() {		
		if (items != null) {
			for (Item item : items) {
				item.mobilize();
			}
		}
	}
	
	private int saveItems(long timestamp) {
		int newItems = 0;
		if (items != null) {
			if (ContentManager.existChannel(this)) {
				for (Item item : items) {
					item.updateTime = timestamp;
					if (ContentManager.saveItem(item)) {
						newItems++;
					}
				}
			}
		}
		return newItems;
	}
	
	public void loadLightweightItems() {
		ContentManager.loadAllItemsOfChannel(this, ContentManager.LIGHTWEIGHT_ITEM_LOADER);
	}		
	
	public static final class Channels implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + ContentsProvider.AUTHORITY + "/channels");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.evolus.droidreader.channels";
		
		public static final String ID = "ID";
		public static final String TITLE = "TITLE";
		public static final String URL = "URL";
		public static final String LINK = "LINK";
		public static final String DESCRIPTION = "DESCRIPTION";
		public static final String IMAGE_URL = "IMAGE_URL";
		public static final String UNREAD = "UNREAD";
		public static final String LATEST_ITEM_IMAGE_URL = "LATEST_ITEM_IMAGE_URL";
		public static final String TAG_ID = "TAG_ID";
		public static final String OPTIONS = "OPTIONS";
		
		private Channels() {			
		}
	}	
}
