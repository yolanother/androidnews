package vn.evolus.news.rss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.Observable;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import vn.evolus.news.util.ActiveList;
import vn.evolus.news.util.ImageLoader;
import vn.evolus.news.util.StreamUtils;
import android.os.Environment;
import android.util.Log;

public class Channel extends Observable implements Serializable {	
	public static final int MAX_ITEMS = 20;
	
	private static final long serialVersionUID = 6204335219893986724L;	
	private static SAXParserFactory factory = SAXParserFactory.newInstance();
	private Object synRoot = new Object();
	
	private String url;
	private String title;
	private String link;
	private String description;	
	private transient ActiveList<Item> items = new ActiveList<Item>();
	private boolean updating = false;	
			
	public Channel(String url) {			
		this("", url);
	}	
	public Channel(String title, String url) {		
		this.title = title;
		this.url = url;
	}	
	public Channel(String url, boolean autoLoad) {
		this(url);
		if (autoLoad) {
			load();
		}
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
	public boolean isUpdating() {
		synchronized (synRoot) {
			return updating;
		}
	}
	
	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("Title", this.title);
		json.put("Url", this.url);		
		return json;
	}
	
	public static Channel fromJSON(JSONObject json) throws JSONException {
		return new Channel(json.getString("Title"), json.getString("Url"));		
	}		
	
	public void update() {		
		synchronized (synRoot) {
			if (updating) return;			
			updating = true;
			this.setChanged();
			this.notifyObservers(updating);
		}
		
		InputStream is = null;
		try {			
			// setup the URL
			URL url = new URL(this.getUrl());
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(5000);
			// get our data via the Url class	
			save(connection.getInputStream());
			// load & parse
			load(true);
    	} catch (Throwable e) {    		
    		e.printStackTrace();    		
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
    	}
    	
    	synchronized (synRoot) {
			updating = false;				
			this.setChanged();
			this.notifyObservers(updating);
		}
    }
	
	private void parse(InputStream is, boolean processImages) {
		long lastTicks = System.currentTimeMillis();	
		// instantiate our handler
		RssHandler rssHandler = new RssHandler(this, processImages);
		try {
			// create a parser
			SAXParser parser = factory.newSAXParser();
			// create the reader (scanner)
			XMLReader xmlReader = parser.getXMLReader();
			
			// assign our handler
			xmlReader.setContentHandler(rssHandler);
			// perform the synchronous parse
			xmlReader.parse(new InputSource(is));
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {			
			Log.d("DEBUG", "Parsing " + this.title + " in " + (System.currentTimeMillis() - lastTicks) + "ms");
		}
		Set<String> images = rssHandler.getImages();
		for (String imageUrl : images) {
			ImageLoader.start(imageUrl, null);
		}
	}
	
	public void load() {
		load(false);
	}
	
	private void load(boolean processImages) {
		long lastTicks = System.currentTimeMillis();
		File file = new File(getFileName());
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			parse(fis, processImages);					
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			Log.d("DEBUG", "Loading " + this.title + " in " + (System.currentTimeMillis() - lastTicks) + "ms");
		}
	}
	
	private void save(InputStream is) throws Exception {
		long lastTicks = System.currentTimeMillis();
		File file = new File(getFileName());
		if (!file.exists()) {
			file.createNewFile();
		}
		FileOutputStream fos = new FileOutputStream(file, false);
		StreamUtils.writeStream(is, fos);
		fos.flush();
		fos.close();
		Log.d("DEBUG", "Saving " + this.title + " in " + (System.currentTimeMillis() - lastTicks) + "ms");
	}
	
	protected void saveJSON() throws Exception {
		long lastTicks = System.currentTimeMillis();
		File file = new File(getFileName());
		if (!file.exists()) {
			file.createNewFile();
		}
		FileOutputStream fos = new FileOutputStream(file, false);
		JSONArray itemArray = new JSONArray();
		int numberOfItems = 0;
		for (Item item : items) {
			itemArray.put(item.toJSON());
			if (++numberOfItems == MAX_ITEMS) break;
		}
		OutputStreamWriter writer = new OutputStreamWriter(fos);
		writer.write(itemArray.toString());
		writer.flush();
		fos.close();
		Log.d("DEBUG", "Saving " + this.title + " in " + (System.currentTimeMillis() - lastTicks) + "ms");
	}		
	
	private String getFileName() {		
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/droidnews/" 
			+ this.url.replace("http://", "").replaceAll("[\\./\\?&#;\\+]", "_") + ".xml";
	}
}
