package vn.evolus.droidreader.model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SubscriptionGroup {
	private String title;
	private List<Subscription> subscriptions;
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public List<Subscription> getSubscriptions() {
		return subscriptions;
	}
	public void setSubscriptions(List<Subscription> subscriptions) {
		this.subscriptions = subscriptions;
	}
	
	public static SubscriptionGroup fromJSON(JSONObject jsonObject) {
		SubscriptionGroup subscriptionGroup = new SubscriptionGroup();
		try {			
			subscriptionGroup.title = jsonObject.getString("title");
			JSONArray channelsArray = jsonObject.getJSONArray("channels");
			subscriptionGroup.subscriptions = new ArrayList<Subscription>();
			for (int i = 0; i < channelsArray.length(); i++) {
				JSONObject subscriptionObject = channelsArray.getJSONObject(i);
				Subscription subscription = new Subscription();
				subscription.url = subscriptionObject.getString("url");
				subscription.title = subscriptionObject.getString("title");
				subscriptionGroup.subscriptions.add(subscription);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}		
		return subscriptionGroup;
	}
}
