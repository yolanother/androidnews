package vn.evolus.android.news.widget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import vn.evolus.android.news.html.Html;
import vn.evolus.android.news.rss.Item;
import android.content.Context;
import android.text.TextUtils;
import android.webkit.WebView;

public class ItemView extends WebView {
	private static DateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm a");
	
	ChannelView channelView;
	public ItemView(Context context, ChannelView channelView) {
		super(context);
		this.channelView = channelView;				
	}			
	
	public void setItem(Item item) {			
		item.setRead(true);
		StringBuffer sb = new StringBuffer();
		sb.append("<h3>");
		sb.append(TextUtils.htmlEncode(item.getTitle()));
		sb.append("</h3>");
		if (item.getPubDate() != null) {
			sb.append("<p><em>");		
			sb.append(dateFormat.format(item.getPubDate()));
			sb.append("</em></p>");
		}
		sb.append(item.getDescription());
		String cleanDescription = Html.toXhtml(sb.toString());		
		this.loadDataWithBaseURL(item.getLink(), cleanDescription, "text/html", "UTF-8", item.getLink());
	}
}
