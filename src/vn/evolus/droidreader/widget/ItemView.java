package vn.evolus.droidreader.widget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vn.evolus.droidreader.Constants;
import vn.evolus.droidreader.R;
import vn.evolus.droidreader.Settings;
import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.model.Item;
import vn.evolus.droidreader.providers.ImagesProvider;
import vn.evolus.droidreader.util.ImageCache;
import android.app.ProgressDialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.github.droidfu.concurrent.BetterAsyncTask;
import com.github.droidfu.concurrent.BetterAsyncTaskCallable;

public class ItemView extends WebView {	
	private static final Pattern imagePattern = Pattern.compile("<img[^>]*src=[\"']([^\"']*)[^>]*>", Pattern.CASE_INSENSITIVE);
	
	private static DateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm a");
	private boolean nightMode = false;
	
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
	
	public boolean hasItem() {
		Item currentItem = (Item)this.getTag();
		return currentItem != null;
	}
	
	public Item getItem() {
		return (Item)this.getTag();
	}
	
	public void setItem(Item item) {
		Item currentItem = (Item)this.getTag();
		if (item.equals(currentItem)) {
			return;
		}		
		this.setTag(item);
		displayItem(item);
	}
	
	public void setNightMode(boolean nightMode) {
		if (this.nightMode != nightMode) {
			this.nightMode = nightMode;
			displayItem(getItem());
		}
	}
	
	public void mobilize() {
		final Item item = (Item)this.getTag();
		if (item == null) return;
		final ProgressDialog progress = new ProgressDialog(getContext());
		progress.setMessage(getResources().getString(R.string.mobilizing));
		BetterAsyncTask<Void, Void, Boolean> task = new BetterAsyncTask<Void, Void, Boolean>(getContext()) {			
			protected void after(Context context, Boolean result) {
				progress.dismiss();
				if (result) {
					displayItem(item);
				}
			}			
			protected void handleError(Context context, Exception e) {
				progress.dismiss();
				e.printStackTrace();
				Toast.makeText(context, "Cannot load mobilize this item.", 5).show();				
			}
		};
		task.setCallable(new BetterAsyncTaskCallable<Void, Void, Boolean>() {
			public Boolean call(BetterAsyncTask<Void, Void, Boolean> task) throws Exception {
				if (item.mobilize()) {
					ContentManager.saveItemDescription(item);
					return true;
				}
				return false;
			}    			
		});
		progress.show();
		task.disableDialog();
		task.execute();
										
	}
		
	private void displayItem(Item item) {
		if (item == null) return;
		
		StringBuffer sb = new StringBuffer();
		String fontFamily = Settings.getFont();
		sb.append("<html><head><style type=\"text/css\">body {font-family:");
		sb.append(fontFamily);
		String fontSize = Settings.getFontSize();
		sb.append(";font-size:");
		sb.append(fontSize);
		sb.append(";line-height: 1.6em;} img {max-width:300px; display:block}");		
		if (nightMode) {
			sb.append("body {background-color: #000; color: #FFF} a{color: #FFF;} img {border:solid 2px #111;}");
			sb.append("p.DRI { font-size: 0.8em; display: block; padding: 0.5em; border: solid 1px #222; }");
		} else {
			sb.append("a {color: #000;} img {border:solid 2px #EEE;}");
			sb.append("p.DRI { font-size: 0.8em; display: block; padding: 0.5em; background: #FAFAFA; }");
		}						
		sb.append("h3.DRT {margin: 0.3em 0; padding: 0;}");
		sb.append("</style></head><body>");		
		sb.append("<h3 class=\"DRT\">");
		sb.append(TextUtils.htmlEncode(item.title));
		sb.append("</h3>");
		sb.append("<p class=\"DRI\">");
		if (item.channel != null && item.channel.title != null) {			
			sb.append(getContext().getString(R.string.source)
					.replace("{source}", TextUtils.htmlEncode(item.channel.title)));
			sb.append("<br />");
		}		
		if (item.pubDate != null) {
			sb.append(getContext().getString(R.string.published)
					.replace("{on}", dateFormat.format(item.pubDate)));
		}
		sb.append("</p>");
		sb.append("<div id=\"drArticleContent\">");
		if (item.description != null) {
			sb.append(item.description);
		}
		sb.append("</div>");
		sb.append("</body></html>");
		String cleanDescription = processOfflineImages(sb.toString());
		this.loadDataWithBaseURL(item.link, cleanDescription, "text/html", "UTF-8", null);
	}	
	
	private String processOfflineImages(String description) {		
		Matcher matcher = imagePattern.matcher(description);		
		while (matcher.find()) {
			String imageUrl = matcher.group(1);
			if (Constants.DEBUG_MODE) Log.d(Constants.LOG_TAG, "Found image " + imageUrl);
			String foundImageTag = matcher.group();			
			if (ImageCache.isCached(imageUrl)) {
				String cachedImageUrl = ImagesProvider.constructUri(ImageCache.getCacheFileName(imageUrl));
				if (Constants.DEBUG_MODE) Log.d(Constants.LOG_TAG, "Replace " + imageUrl + " = " + cachedImageUrl);
				description = description.replace(foundImageTag,
						"<img src=\"" + cachedImageUrl + "\" />");				
			}
		}		
		return description;
	}
}
