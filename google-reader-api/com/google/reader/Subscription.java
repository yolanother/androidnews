package com.google.reader;

import org.json.JSONException;
import org.json.JSONObject;

public class Subscription {
	private String id;
	private String url;
	private String title;
		
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	public static Subscription fromJSON(JSONObject jsonObject) {
		Subscription subscription = new Subscription();
		try {
			subscription.id = jsonObject.getString("id");
			subscription.title = jsonObject.getString("title");
			subscription.url = subscription.id.substring(5);
		} catch (JSONException e) {
			e.printStackTrace();
		}		
		return subscription;
	}
}
