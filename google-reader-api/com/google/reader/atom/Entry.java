package com.google.reader.atom;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Entry {
	private String id;
	private String title;
	private String link;
	private String summary;
	private String content;
	private Date updated;
	private boolean read = false;
	private boolean starred = false;
	private boolean keptUnread = false;
	private String feedId;
	private List<String> tags;
		
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
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public Date getUpdated() {
		return updated;
	}
	public void setUpdated(Date updated) {
		this.updated = updated;
	}
	public boolean getRead() {
		return read;
	}
	public void setRead(boolean read) {
		this.read = read;
	}	
	public boolean getStarred() {
		return starred;
	}
	public void setStarred(boolean starred) {
		this.starred = starred;
	}	
	public boolean getKeptUnread() {
		return keptUnread;
	}
	public void setKeptUnread(boolean keptUnread) {
		this.keptUnread = keptUnread;
	}	
	public List<String> getTags() {
		if (tags == null) {
			tags = new ArrayList<String>();
		}
		return tags;
	}
	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	public String getFeedId() {
		return feedId;
	}
	public void setFeedId(String feedId) {
		this.feedId = feedId;
	}	
}