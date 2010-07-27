package vn.evolus.news.model;

import java.util.List;

public class ChannelGroup {
	private String title;
	private List<Channel> channels;
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public List<Channel> getChannels() {
		return channels;
	}
	public void setChannels(List<Channel> channels) {
		this.channels = channels;
	}		
}
