package vn.evolus.android.news.rss;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.Observable;
import java.util.UUID;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import vn.evolus.android.news.util.ActiveList;
import android.util.Log;

public class Channel extends Observable implements Serializable {	
	private static final long serialVersionUID = 6204335219893986724L;	
	private static SAXParserFactory factory = SAXParserFactory.newInstance();
	private Object synRoot = new Object();
	
	protected String id;
	private String url;
	private String title;
	private String link;
	private String description;	
	private transient ActiveList<Item> items = new ActiveList<Item>();
	private boolean updating = false;
		
	public Channel() {
		id = UUID.randomUUID().toString();
	}	
	public Channel(String url) {	
		this();
		this.url = url;
	}	
	public Channel(String title, String url) {
		this();
		this.title = title;
		this.url = url;
	}	
	public String getId() {
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
		int total = 0;
		for (Item item : this.getItems()) {
			if (!item.getRead()) {
				total += 1;
			}
		}
		return total;
	}	
	public void save(FileOutputStream fos) {		
		try {
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this);
		} catch (Exception e) {			
			Log.e("ERROR", e.getMessage());
		}		
	}	
	public static Channel load(FileInputStream fis) {
		try {
			ObjectInputStream oos = new ObjectInputStream(fis);
			return (Channel)oos.readObject();
		} catch (Exception e) {			
			Log.e("ERROR", e.getMessage());
		}
		return null;
	}	
	public boolean isUpdating() {
		synchronized (synRoot) {
			return updating;
		}
	}
	public void update() {
		synchronized (synRoot) {
			if (updating) return;			
			updating = true;
			this.setChanged();
			this.notifyObservers(updating);
		}
		
		InputStream istream = null;
		try {			
			// setup the URL
			URL url = new URL(this.getUrl());
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(5000);
			//connection.setUseCaches(false);
			//connection.setDefaultUseCaches(false);
			connection.setRequestProperty("User-Agent", "Mozilla/5.0(Windows; U; Windows NT 5.2; rv:1.9.2) Gecko/20100101 Firefox/3.6");
			// create a parser
			SAXParser parser = factory.newSAXParser();
			// create the reader (scanner)
			XMLReader xmlReader = parser.getXMLReader();
			// instantiate our handler
			RssHandler rssHandler = new RssHandler(this);
			// assign our handler
			xmlReader.setContentHandler(rssHandler);
			// get our data via the Url class			
			istream = connection.getInputStream();			
			// perform the synchronous parse    			
			xmlReader.parse(new InputSource(istream));
    	} catch (Throwable e) {
    		e.printStackTrace();
    		Log.e("ERROR", e.getMessage());
    	}
    	finally {    		
    		if (istream != null) {
    			try {
					istream.close();					
				} catch (IOException e) {
					Log.e("ERROR", "Error on closing connection to " + this.getUrl() + ": " + e.getMessage());
					e.printStackTrace();
				}
    		}    		
    	}
    	
    	synchronized (synRoot) {
			updating = false;				
			this.setChanged();
			this.notifyObservers(updating);
		}
    }
}
