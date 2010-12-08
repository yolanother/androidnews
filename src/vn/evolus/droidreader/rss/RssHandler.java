package vn.evolus.droidreader.rss;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.model.Item;
import vn.evolus.droidreader.util.Html;

public class RssHandler extends DefaultHandler {	
	private static DateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
	private static String feedBurnerUri = "http://purl.org/rss/1.0/modules/content/";	

	final int RSS_CHANNEL = 0;
	final int RSS_CHANNEL_TITLE = 1;
	final int RSS_CHANNEL_LINK = 2;
	final int RSS_CHANNEL_DESCRIPTION = 3;
	final int RSS_CHANNEL_IMAGE = 4;
	
	final int RSS_ITEM = 20;
	final int RSS_ITEM_TITLE = 21;
	final int RSS_ITEM_PUB_DATE = 22;
	final int RSS_ITEM_LINK = 23;
	final int RSS_ITEM_DESCRIPTION = 24;
		
	private Channel channel;
	private int maxItemsPerChannel;
	private int currentState = RSS_CHANNEL;
	private Item item;
	private StringBuffer currentTextValue;	
	private int newItems = 0;
	
	public RssHandler(Channel channel, int maxItemsPerChannel) {
		this.channel = channel;			
		this.newItems = 0;
		this.maxItemsPerChannel = maxItemsPerChannel;
	}
	
	public Channel getChannel() {
		return channel;
	}	
	public int getNewItems() {
		return newItems;
	}

	@Override
	public void startDocument() throws SAXException {		
		currentState = RSS_CHANNEL;
	}
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) 
		throws SAXException {
		currentTextValue = new StringBuffer();
		if (uri != null && uri.length() != 0 && !feedBurnerUri.equals(uri)) {
			return;
		}
		
		if (localName.equals("channel")) {			
			currentState = RSS_CHANNEL;
			return;
		}
		if (localName.equals("item")) {
			item = new Item();
			item.channel = this.channel;
			currentState = RSS_ITEM;
			return;
		}
		if (localName.equals("image")) {
			if (currentState == RSS_CHANNEL) {
				currentState = RSS_CHANNEL_IMAGE;
			}
			return;
		}
		if (localName.equals("title")) {
			if (currentState >= RSS_ITEM) {
				currentState = RSS_ITEM_TITLE;
			} else if (currentState == RSS_CHANNEL){
				currentState = RSS_CHANNEL_TITLE;
			}
			return;
		}	
		if (localName.equals("pubDate")) {
			if (currentState >= RSS_ITEM) {
				currentState = RSS_ITEM_PUB_DATE;
			}
			return;
		}
		if (localName.equals("link")) {
			if (currentState >= RSS_ITEM) {
				currentState = RSS_ITEM_LINK;
			} else if (currentState == RSS_CHANNEL) {
				currentState = RSS_CHANNEL_LINK;
			}
			return;
		}	
		if (localName.equals("description")) {
			if (currentState >= RSS_ITEM) {
				currentState = RSS_ITEM_DESCRIPTION;
			} else {
				currentState = RSS_CHANNEL_DESCRIPTION;
			}
			return;
		}
		if (localName.equals("encoded") && feedBurnerUri.equals(uri)) {
			if (currentState >= RSS_ITEM) {
				currentState = RSS_ITEM_DESCRIPTION;
			}
			return;
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {				
		
		if (uri != null && uri.length() > 0 && !feedBurnerUri.equals(uri)) {
			return;
		}		
		if (localName.equals("image")) {			
			if (currentState == RSS_CHANNEL_IMAGE) {
				currentState = RSS_CHANNEL;
			}
			return;
		}
		if (localName.equals("item")) {
			channel.addItem(item);
			newItems++;
			currentState = RSS_CHANNEL;
			if (channel.getItems().size() == maxItemsPerChannel) {
				throw new SAXException("Reaching maximum items. Stop parsing.");
			}			
		}		
		if (localName.equals("title")) {
			if (currentState == RSS_ITEM_TITLE) {
				item.title = Html.decode(cleanUpText(currentTextValue));
				currentState = RSS_ITEM;
			} else if (currentState == RSS_CHANNEL_TITLE) {
				channel.title = cleanUpText(currentTextValue);
				currentState = RSS_CHANNEL;
			}			
		}
		if (localName.equals("pubDate")) {
			if (currentState == RSS_ITEM_PUB_DATE) {				
				try {
					item.pubDate = dateFormat.parse(cleanUpText(currentTextValue));
				} catch (Throwable e) {					
					item.pubDate = new Date(System.currentTimeMillis());
				}				
				currentState = RSS_ITEM;
			} else {
				currentState = RSS_CHANNEL;
			}
		}
		if (localName.equals("description")) {
			if (currentState == RSS_ITEM_DESCRIPTION) {
				if (item.description == null) { 
					item.description = cleanUpText(currentTextValue);
				}
				currentState = RSS_ITEM;
			} else if (currentState == RSS_CHANNEL_DESCRIPTION) {
				channel.description = cleanUpText(currentTextValue);
				currentState = RSS_CHANNEL;
			}			
		}
		if (localName.equals("encoded") && feedBurnerUri.equals(uri)) {
			if (currentState == RSS_ITEM_DESCRIPTION) {
				item.description = cleanUpText(currentTextValue);
				currentState = RSS_ITEM;
			}		
		}
		if (localName.equals("link")) {
			if (currentState == RSS_ITEM_LINK) {
				item.link = cleanUpText(currentTextValue);
				if (ContentManager.existItem(item)) {
					throw new SAXException("Found existing item. Stop parsing.");
				}
				currentState = RSS_ITEM;
			} else if (currentState == RSS_CHANNEL_LINK) {
				channel.link = cleanUpText(currentTextValue);
				currentState = RSS_CHANNEL;
			}
		}
	}			

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		currentTextValue.append(ch, start, length);	
	}
	
	private String cleanUpText(StringBuffer text) {
		if (text == null) return null;
		return text.toString().replace("\r", "").replace("\t", "").trim();		
	}			
}
