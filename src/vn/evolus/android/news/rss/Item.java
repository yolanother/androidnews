package vn.evolus.android.news.rss;

import java.util.Date;

public class Item {
	private String title;
	private String description;
	private Date pubDate;
	private String link;
	
	public Item() {		
	}
		
	public Item(String title, String description, Date pubDate, String link) {	
		this.title = title;
		this.description = description;
		this.pubDate = pubDate;
		this.link = link;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Date getPubDate() {
		return pubDate;
	}
	public void setPubDate(Date pubDate) {
		this.pubDate = pubDate;
	}
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}	
}
