package vn.evolus.news.rss;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RssHandler extends DefaultHandler {
	private static Pattern imagePattern = Pattern.compile("<img[^>]*src=[\"']([^\"']*)", Pattern.CASE_INSENSITIVE);
	private static Pattern blackListImagePattern = Pattern.compile(
			"(api\\.tweetmeme\\.com)|(www\\.engadget\\.com/media/post_label)|(feedads)|(feedburner)|((feeds|stats)\\.wordpress\\.com)|(cdn\\.stumble-upon\\.com)|(vietnamnet\\.gif)|(images\\.pheedo\\.com/images/mm)" +
			"|(creatives\\.commindo-media\\.de)");
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
	
	Channel channel;	
	int currentState = RSS_CHANNEL;
	private Item item;
	private StringBuffer currentTextValue;	
	
	public RssHandler(Channel channel) {
		this.channel = channel;
	}
	
	public Channel getChannel() {
		return channel;
	}
	
	@Override
	public void startDocument() throws SAXException {
		item = new Item();
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
			postProcessItem(item);
			channel.addItem(item);			
			currentState = RSS_CHANNEL;
		}		
		if (localName.equals("title")) {
			if (currentState == RSS_ITEM_TITLE) {
				item.setTitle(htmlDecode(cleanUpText(currentTextValue)));
				currentState = RSS_ITEM;
			} else if (currentState == RSS_CHANNEL_TITLE) {
				channel.setTitle(cleanUpText(currentTextValue));
				currentState = RSS_CHANNEL;
			}			
		}
		if (localName.equals("pubDate")) {
			if (currentState == RSS_ITEM_PUB_DATE) {				
				try {
					item.setPubDate(dateFormat.parse(cleanUpText(currentTextValue)));
				} catch (Throwable e) {
					e.printStackTrace();
					item.setPubDate(new Date(System.currentTimeMillis()));
				}				
				currentState = RSS_ITEM;
			} else {
				currentState = RSS_CHANNEL;
			}
		}
		if (localName.equals("description")) {
			if (currentState == RSS_ITEM_DESCRIPTION) {
				if (item.getDescription() == null) { 
					item.setDescription(cleanUpText(currentTextValue));
				}
				currentState = RSS_ITEM;
			} else if (currentState == RSS_CHANNEL_DESCRIPTION) {
				channel.setDescription(cleanUpText(currentTextValue));				
				currentState = RSS_CHANNEL;
			}			
		}
		if (localName.equals("encoded") && feedBurnerUri.equals(uri)) {
			if (currentState == RSS_ITEM_DESCRIPTION) {
				item.setDescription(cleanUpText(currentTextValue));
				currentState = RSS_ITEM;
			}		
		}
		if (localName.equals("link")) {
			if (currentState == RSS_ITEM_LINK) {
				item.setLink(cleanUpText(currentTextValue));
				if (channel.existItem(item)) {
					throw new SAXException("Found existing item. Stop parsing.");
				}
				currentState = RSS_ITEM;
			} else if (currentState == RSS_CHANNEL_LINK) {
				channel.setLink(cleanUpText(currentTextValue));
				currentState = RSS_CHANNEL;
			}
		}
	}			

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		currentTextValue.append(ch, start, length);	
	}
	
	public String htmlDecode(String html) {
        return html
        	.replace("&lt;", "<")
        	.replace("&gt;", ">")
        	.replace("&quot;", "\"")
        	.replace("&apos;", "'")
        	.replace("&nbsp;", " ")
        	.replace("&amp;", "&")
        	.replace("&mdash;", "—");
	}
	
	private String cleanUpText(StringBuffer text) {
		if (text == null) return null;
		return text.toString().replace("\r", "").replace("\t", "").trim();		
	}
	
	private void postProcessItem(Item item) {		
		extractItemImageFromDescription(item);
	}
	
	private void extractItemImageFromDescription(Item item) {
		Matcher matcher = imagePattern.matcher(item.getDescription());
		while (matcher.find()) {
			String imageUrl = matcher.group(1);			
			if (!blackListImagePattern.matcher(imageUrl).find()) {
				//Log.d("DEBUG", "Found image URL: " + imageUrl);				
				item.setImageUrl("http://feeds.demo.evolus.vn/resizer/?width=60&height=60&url=" +
						URLEncoder.encode(imageUrl));		
				return;
			} else {
				//Log.d("DEBUG", "MATCH BLACK LIST : " + imageUrl);
			}
		}		
	}	
}
