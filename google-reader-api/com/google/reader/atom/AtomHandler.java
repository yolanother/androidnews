package com.google.reader.atom;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import vn.evolus.droidreader.util.Html;

public class AtomHandler extends DefaultHandler {
	private static String NAMESPACE = "http://www.w3.org/2005/Atom";
	private static String GOOGLE_READER_NAMESPACE = "http://www.google.com/schemas/reader/atom/";
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
	
	private static final int ATOM_FEED = 1;	
	private static final int ATOM_ENTRY = 2;
	private static final int ATOM_SOURCE = 3;
		
	private int currentState = ATOM_FEED;
	
	private int maxItems = 20;
	private AtomFeed feed;
	private Entry entry;
	private StringBuffer sb;	
	private OnNewEntryCallback callback;
	
	public AtomHandler(int maxItems) {
		super();
		this.maxItems = maxItems;
	}
		
	public OnNewEntryCallback getCallback() {
		return callback;
	}
	public void setCallback(OnNewEntryCallback callback) {
		this.callback = callback;
	}
	public AtomFeed getFeed() {
		return feed;
	}

	@Override
	public void startDocument() throws SAXException {
	}
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) 
		throws SAXException {
		//Log.d("DEBUG", "<" + localName + ">" + " - " + uri);
		sb = new StringBuffer();
		
		if (localName.equals("feed")) {
			feed = new AtomFeed();
			currentState = ATOM_FEED;
		} else if (localName.equals("entry")) {
			entry = new Entry();
			currentState = ATOM_ENTRY;
			String readState = attributes.getValue("gr:is-read-state-locked");
			if (readState != null && "true".equals(readState)) {
				entry.setRead(true);
			}			
		} else if (localName.equals("source")) {			
			String feedId = attributes.getValue(GOOGLE_READER_NAMESPACE, "stream-id");
			if (currentState == ATOM_FEED) {
				entry.setFeedId(feedId);					
			}
			currentState = ATOM_SOURCE;
		} else if (localName.equals("link")) {
			String type = attributes.getValue("type");
			String href = attributes.getValue("href");
			if ("text/html".equals(type)) {
				if (currentState == ATOM_ENTRY) {
					entry.setLink(href);					
				} else if (currentState == ATOM_FEED) {
					feed.setLink(href);
				}
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		//Log.d("DEBUG", sb.toString());
		//Log.d("DEBUG", "</" + localName + ">" + " - " + uri);
		
		if (localName.equals("id")) {			
			String id = cleanUpText(sb);
			if (currentState == ATOM_ENTRY) {
				entry.setId(id);
			} else if (currentState == ATOM_FEED) {
				feed.setId(id);
			}
		} else if (localName.equals("title")) {			
			String title = Html.decode(cleanUpText(sb));
			if (currentState == ATOM_ENTRY) {
				entry.setTitle(title);
			} else if (currentState == ATOM_FEED) {
				feed.setTitle(title);
			}
		} else if (localName.equals("updated")) {					
			Date updated = new Date();
			try {
				updated = dateFormat.parse(cleanUpText(sb).replace("Z", "GMT+00:00"));
			} catch (ParseException pe) {
				pe.printStackTrace();
			}
			if (currentState == ATOM_ENTRY) {
				entry.setUpdated(updated);
			} else if (currentState == ATOM_FEED) {
				feed.setUpdated(updated);
			}
		} else if (localName.equals("entry")) {
			if (callback != null) {
				try {
					callback.onNewEntry(entry);
				} catch (Throwable t) {
					throw new SAXException(t.getMessage());
				}
			}
			
			feed.addEntry(entry);			
			currentState = ATOM_FEED;
			if (feed.getEntries().size() == maxItems) {
				throw new SAXException("Reaching maximum items (" + maxItems + "). Stop parsing.");
			}
		} else if (localName.equals("source")) {
			currentState = ATOM_ENTRY;
		} else if (localName.equals("summary") && NAMESPACE.equals(uri)) {
			if (currentState == ATOM_ENTRY) {
				entry.setSummary(sb.toString());
			}
		} else if (localName.equals("content") && NAMESPACE.equals(uri)) {
			if (currentState == ATOM_ENTRY) {
				entry.setContent(sb.toString());
			}
		} else if (localName.equals("continution") && GOOGLE_READER_NAMESPACE.equals(uri)) {
			if (currentState == ATOM_FEED) {
				feed.setContinution(sb.toString());
			}
		}
	}			

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		sb.append(ch, start, length);
	}
	
	private String cleanUpText(StringBuffer sb) {
		if (sb == null) return null;
		return sb.toString().replace("\r", "").replace("\t", "").trim();		
	}
	
	public interface OnNewEntryCallback {
		void onNewEntry(Entry entry);
	}
}
