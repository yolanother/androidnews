package vn.evolus.android.news.rss;

import java.io.Serializable;
import java.util.Date;

public class Item implements Serializable {	
	private static final long serialVersionUID = 1783666956248831428L;
	
	private String title;
	private String description;
	private Date pubDate;
	private String link;
	private String imageUrl;	
	private boolean read;
	
	public Item() {		
		this.read = false;
		this.pubDate = new Date(System.currentTimeMillis());
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
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {		
		this.imageUrl = imageUrl;
	}		
	public boolean getRead() {
		return read;
	}
	public void setRead(boolean read) {
		this.read = read;
	}

	@Override
	public int hashCode() {		
		return 31 + ((link == null) ? 0 : link.hashCode());		
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Item other = (Item) obj;
		if (link == null) {
			if (other.link != null)
				return false;
		} else if (!link.equals(other.link))
			return false;
		return true;
	}	
}
