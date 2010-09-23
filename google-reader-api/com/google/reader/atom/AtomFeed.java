package com.google.reader.atom;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.google.reader.atom.AtomHandler.OnNewEntryCallback;

public class AtomFeed {
	private static SAXParserFactory factory = SAXParserFactory.newInstance();
	
	private String id;
	private String title;
	private String link;
	private Date updated;
	private List<Entry> entries;
	private String continution;
	
	public AtomFeed() {
		entries = new ArrayList<Entry>();
	}	
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
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
	public String getUrl() {
		return this.link;
	}
	public Date getUpdated() {
		return updated;
	}
	public void setUpdated(Date updated) {
		this.updated = updated;
	}
	public List<Entry> getEntries() {
		return entries;
	}
	public void setEntries(List<Entry> entries) {
		this.entries = entries;
	}
	public void addEntry(Entry entry) {
		this.entries.add(entry);
	}
	public String getContinution() {
		return continution;
	}
	public void setContinution(String continution) {
		this.continution = continution;
	}
	public static AtomFeed parse(InputStream is, int maxItems) {
		return parse(is, maxItems, null);
	}
	public static AtomFeed parse(InputStream is, int maxItems, OnNewEntryCallback callback) {
		// instantiate our handler
		AtomHandler atomHandler = new AtomHandler(maxItems);
		atomHandler.setCallback(callback);
		try {
			// create a parser
			SAXParser parser = factory.newSAXParser();
			// create the reader (scanner)
			XMLReader xmlReader = parser.getXMLReader();
			// assign our handler
			xmlReader.setContentHandler(atomHandler);
			// perform the synchronous parse
			xmlReader.parse(new InputSource(is));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return atomHandler.getFeed();
	}	
}
