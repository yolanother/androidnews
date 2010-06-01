package vn.evolus.news.widget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import vn.evolus.news.rss.Item;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class ItemView extends WebView {
	private static DateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm a");
	
	public ItemView(Context context) {
		super(context);
		this.setInitialScale(0);
		this.setBackgroundColor(-1);
		this.setVerticalScrollBarEnabled(false);		
		this.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
		
		WebSettings settings = this.getSettings();
		settings.setNeedInitialFocus(false);
		settings.setSupportZoom(false);
	}	
	public ItemView(Context context,  AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setItem(Item item) {
		Item currentItem = (Item)this.getTag();
		if (item.equals(currentItem)) {
			return;
		}		
		this.setTag(item);
		item.setRead(true);
		StringBuffer sb = new StringBuffer();
		sb.append("<html><head><style type=\"text/css\">img{max-width:300px;border:0}</style></head><body>");
		sb.append("<h3>");
		sb.append(TextUtils.htmlEncode(item.getTitle()));
		sb.append("</h3>");
		if (item.getPubDate() != null) {
			sb.append("<p><em>");		
			sb.append(dateFormat.format(item.getPubDate()));
			sb.append("</em></p>");
		}
		sb.append(item.getDescription());
		sb.append("</body></html>");
		String cleanDescription = sb.toString();// Html.toXhtml(sb.toString());
		this.loadDataWithBaseURL(item.getLink(), cleanDescription, "text/html", "UTF-8", null);
	}
}
