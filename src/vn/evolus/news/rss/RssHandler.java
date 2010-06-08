package vn.evolus.news.rss;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import vn.evolus.news.model.Image;
import vn.evolus.news.providers.ImagesProvider;
import vn.evolus.news.util.ImageCache;
import android.content.ContentResolver;

public class RssHandler extends DefaultHandler {	
	private static Pattern imagePattern = Pattern.compile("<img[^>]*src=[\"']([^\"']*)[^>]*>", Pattern.CASE_INSENSITIVE);	
	private static Pattern iframePattern = Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE);
	private static Pattern blackListImagePattern;
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
	
	private ContentResolver cr;
	private Channel channel;	
	private int currentState = RSS_CHANNEL;
	private Item item;
	private StringBuffer currentTextValue;	
	private int newItems = 0;
	
	static {
		String[] blackList = new String[] {
			"www.engadget.com/media/post_label",
			"feeds.wordpress.com",
			"stats.wordpress.com",
			"feedads",
			"feedburner",
			"api.tweetmeme.com",
			"creatives.commindo-media.de",
			"ads.pheedo.com",
			"images.pheedo.com/images/mm",
			"cdn.stumble-upon.com",
			"vietnamnet.gif",
			"digg-badge-custom-1.gif"
		};
		
		//
		StringBuilder sb = new StringBuilder();		
		for (String item : blackList) {
			sb.append("|(");
			sb.append(Pattern.quote(item));
			sb.append(")");
		}
		blackListImagePattern = Pattern.compile(sb.substring(1));
	}
	
	public RssHandler(Channel channel, ContentResolver cr) {
		this.channel = channel;
		this.cr = cr;		
		this.newItems = 0;
	}
	
	public Channel getChannel() {
		return channel;
	}	
	public int getNewItems() {
		return newItems;
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
			item.setChannel(this.channel);
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
			processItem(item);
			channel.addItem(item);
			newItems++;
			currentState = RSS_CHANNEL;
			if (channel.getItems().size() == Channel.MAX_ITEMS) {
				throw new SAXException("Reaching maximum items. Stop parsing.");
			}			
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
				if (Item.exists(cr, item)) {
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
		
	public void processItem(Item item) {
		String itemDescription = item.getDescription(); 	
		Matcher matcher = imagePattern.matcher(itemDescription);
		boolean found = false;
		Set<String> images = new HashSet<String>();
		while (matcher.find()) {
			String imageUrl = matcher.group(1);
			String foundImageTag = matcher.group();
			if (!blackListImagePattern.matcher(imageUrl).find()) {				
				String cachedImageUrl = "http://image-resize.appspot.com/?width=300&height=300&url=" + URLEncoder.encode(imageUrl);				
				if (!images.contains(cachedImageUrl)) {					
					images.add(cachedImageUrl);
					Image.queue(cachedImageUrl, cr);
					itemDescription = itemDescription.replace(foundImageTag,
							"<img src=\"" + ImagesProvider.constructUri(ImageCache.getCacheFileName(cachedImageUrl)) + "\" />");
				}
				if (!found) {
					item.setImageUrl("http://feeds.demo.evolus.vn/resizer/?width=60&height=60&url=" +
							URLEncoder.encode(imageUrl));
					images.add(item.getImageUrl());
					Image.queue(item.getImageUrl(), cr);
					found = true;
				}
			} else {
				itemDescription = itemDescription.replace(foundImageTag, "");
			}
		}		
		itemDescription = iframePattern.matcher(itemDescription).replaceAll("");
		item.setDescription(itemDescription);		
	}		
}
