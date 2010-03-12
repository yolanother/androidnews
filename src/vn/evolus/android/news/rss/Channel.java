package vn.evolus.android.news.rss;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.graphics.drawable.Drawable;

public class Channel {
	private String url;
	
	private String title;
	private String link;
	private String description;
	private Drawable image;
	private List<Item> items = new ArrayList<Item>();		
	
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
	public Drawable getImage() {
		return image;
	}
	public void setImage(Drawable image) {
		this.image = image;
	}		
	public List<Item> getItems() {
		return items;
	}
	public void setItems(List<Item> items) {
		this.items = items;
	}
		
	public static Channel create(Channel channel) {
    	try {
    		// setup the URL
    	   URL url = new URL(channel.getUrl());
    	   URLConnection connection = url.openConnection();
    	   connection.setRequestProperty("User-Agent", "Mozilla/5.0(Windows; U; Windows NT 5.2; rv:1.9.2) Gecko/20100101 Firefox/3.6");
           // create the factory
           SAXParserFactory factory = SAXParserFactory.newInstance();
           // create a parser
           SAXParser parser = factory.newSAXParser();
           // create the reader (scanner)
           XMLReader xmlReader = parser.getXMLReader();
           // instantiate our handler
           RssHandler rssHandler = new RssHandler(channel);
           // assign our handler
           xmlReader.setContentHandler(rssHandler);
           // get our data via the Url class
           InputSource is = new InputSource(connection.getInputStream());
           // perform the synchronous parse           
           xmlReader.parse(is);
           // get the results - should be a fully populated RSSFeed instance, or null on error           
           return rssHandler.getChannel();
    	} catch (Exception ee) {
    		ee.printStackTrace();
    		// if we have a problem, simply return null
    		return channel;
    	}
    }
}
