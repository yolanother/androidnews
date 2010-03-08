package vn.evolus.android.news.rss;

import java.util.Date;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RssHandler extends DefaultHandler {
	final int RSS_CHANNEL = 0;
	final int RSS_CHANNEL_TITLE = 1;
	final int RSS_CHANNEL_LINK = 2;
	final int RSS_CHANNEL_DESCRIPTION = 3;
	
	final int RSS_ITEM = 20;
	final int RSS_ITEM_TITLE = 21;
	final int RSS_ITEM_PUB_DATE = 22;
	final int RSS_ITEM_LINK = 23;
	final int RSS_ITEM_DESCRIPTION = 24;
	
	Channel channel;	
	int currentState = RSS_CHANNEL;
	private Item item;
	private StringBuffer currentTextValue;	
	
	public RssHandler(Channel channel) {
		this.channel = channel;
		channel.getItems().clear();
	}
	
	public Channel getChannel() {
		return channel;
	}	
	
	@Override
	public void startDocument() throws SAXException {
		item = new Item();
	}
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) 
		throws SAXException {			
		
		currentTextValue = new StringBuffer();
		if (uri != null && uri.length() != 0)
		{
			return;
		}
		
		if (localName.equals("channel"))
		{			
			currentState = RSS_CHANNEL;
			return;
		}
		if (localName.equals("item"))
		{
			item = new Item();
			currentState = RSS_ITEM;
			return;
		}
		if (localName.equals("title"))
		{
			if (currentState >= RSS_ITEM) {
				currentState = RSS_ITEM_TITLE;
			} else {
				currentState = RSS_CHANNEL_TITLE;
			}
			return;
		}	
		if (localName.equals("pubDate"))
		{
			currentState = RSS_ITEM_PUB_DATE;
			return;
		}
		if (localName.equals("link"))
		{
			if (currentState >= RSS_ITEM) {
				currentState = RSS_ITEM_LINK;
			} else {
				currentState = RSS_CHANNEL_LINK;
			}
			return;
		}	
		if (localName.equals("description"))
		{
			if (currentState >= RSS_ITEM) {
				currentState = RSS_ITEM_DESCRIPTION;
			} else {
				currentState = RSS_CHANNEL_DESCRIPTION;
			}
			return;
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {				
		
		if (uri != null && uri.length() > 0)
		{
			return;
		}
		
		if (localName.equals("item"))
		{
			channel.getItems().add(item);
			currentState = RSS_CHANNEL;
		}
		if (localName.equals("title"))
		{
			if (currentState == RSS_ITEM_TITLE) {
				item.setTitle(htmlDecode(cleanUpText(currentTextValue)));
				currentState = RSS_ITEM;
			} else {
				channel.setTitle(cleanUpText(currentTextValue));
				currentState = RSS_CHANNEL;
			}			
		}
		if (localName.equals("pubDate"))
		{		
			if (currentState == RSS_ITEM_PUB_DATE) {
				item.setPubDate(new Date(cleanUpText(currentTextValue)));
				currentState = RSS_ITEM;
			}
		}
		if (localName.equals("description"))
		{
			if (currentState == RSS_ITEM_DESCRIPTION) {
				item.setDescription(cleanUpText(currentTextValue));
				currentState = RSS_ITEM;
			} else {
				channel.setDescription(cleanUpText(currentTextValue));
				currentState = RSS_CHANNEL;
			}			
		}
		if (localName.equals("link"))
		{
			if (currentState == RSS_ITEM_LINK) {
				item.setLink(cleanUpText(currentTextValue));
				currentState = RSS_ITEM;
			} else {
				channel.setLink(cleanUpText(currentTextValue));
				currentState = RSS_CHANNEL;
			}			
		}
		//currentTextValue = new StringBuffer();
	}	
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		StringBuffer value = new StringBuffer();
		value.append(ch, start, length);		
		currentTextValue.append(value);
	}
	
	public String htmlDecode(String html) {
        return html
        	.replace("&lt;", "<")
        	.replace("&gt;", ">")
        	.replace("&quot;", "\"")
        	.replace("&apos;", "'")
        	.replace("&amp;", "&");
	}
	
	private String cleanUpText(StringBuffer text) {
		if (text == null) return null;
		return text.toString().replace("\r", "").replace("\t", "").trim();		
	}	
}
