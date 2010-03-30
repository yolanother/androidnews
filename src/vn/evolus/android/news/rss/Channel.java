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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import vn.evolus.android.news.util.ActiveList;
import android.util.Log;

public class Channel implements Serializable {	
	private static final long serialVersionUID = 6204335219893986724L;
	private Object synRoot = new Object();
	
	private String url;
	private String title;
	private String link;
	private String description;	
	private ActiveList<Item> items = new ActiveList<Item>();
	private boolean updating = false; 
	
	public Channel() {	
	}	
	public Channel(String url) {
		this.url = url;
	}	
	public Channel(String title, String url) {
		this.title = title;
		this.url = url;
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
		return items;
	}	
	public void clearItems() {
		items.clear();
	}	
	public boolean existItem(Item item) {
		return this.items.indexOf(item) >= 0;
	}
	public void addItem(Item item) {
		synchronized (synRoot) {
			if (this.items.indexOf(item) < 0) {
				// find insert location
				int position = 0;
				for (Item currentItem : this.items) {
					if (currentItem.getPubDate().before(item.getPubDate())) {
						this.items.add(position, item);
						return;
					}
					position++;
				}
				this.items.add(item);
			}
		}
	}
	public boolean isEmpty() {
		return this.items.size() == 0;
	}	
	public int countUnreadItems() {
		int total = 0;
		for (Item item : this.items) {
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
		InputStream istream = null;
		try {
			synchronized (synRoot) {
				if (updating) return;
				updating = true;
			}
			// setup the URL
			URL url = new URL(this.getUrl());
			URLConnection connection = url.openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0(Windows; U; Windows NT 5.2; rv:1.9.2) Gecko/20100101 Firefox/3.6");
			// create the factory
			SAXParserFactory factory = SAXParserFactory.newInstance();
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
			InputSource is = new InputSource(connection.getInputStream());
			// perform the synchronous parse    			
			xmlReader.parse(is);					
    	} catch (Exception e) {
    		e.printStackTrace();
    		Log.e("ERROR", e.getMessage());
    	} finally {
    		if (istream != null) {
    			try {
					istream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    		synchronized (synRoot) {
				updating = false;
			}
    	}
    }	
}
