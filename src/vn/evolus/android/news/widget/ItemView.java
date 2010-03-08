package vn.evolus.android.news.widget;

import vn.evolus.android.news.rss.Item;
import android.content.Context;
import android.webkit.WebView;

public class ItemView extends WebView {
	ChannelView channelView;
	public ItemView(Context context, ChannelView channelView) {
		super(context);
		this.channelView = channelView;				
	}			
	
	public void setItem(Item item) {		
		this.loadData(item.getDescription(), "text/html", "UTF-8");
	}
}
