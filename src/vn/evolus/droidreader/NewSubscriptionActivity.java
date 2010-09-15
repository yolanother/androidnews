package vn.evolus.droidreader;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import vn.evolus.droidreader.adapter.SearchResultAdapter;
import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.model.Subscription;
import vn.evolus.droidreader.util.Html;
import vn.evolus.droidreader.util.StreamUtils;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.OnItemClickListener;

import com.github.droidfu.concurrent.BetterAsyncTask;
import com.github.droidfu.concurrent.BetterAsyncTaskCallable;

public class NewSubscriptionActivity extends LocalizedActivity {
	//private static final String SEARCH_FEED_URL = "http://ajax.googleapis.com/ajax/services/feed/find?v=1.0&q=";
	
	private TextView channelUrl;
	private ListView resultListView;
	private SearchResultAdapter adapter;
	private ViewSwitcher searchOrProgress;
	private Button subscribeButton;	
	int numberOfSubscribed = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);				
		setContentView(R.layout.new_subscription);
		setTitle(R.string.subscribe_to);
		
		searchOrProgress = (ViewSwitcher)findViewById(R.id.searchOrProgress);
		
		channelUrl = (TextView)findViewById(R.id.channelUrl);		
		ImageButton searchButton = (ImageButton)findViewById(R.id.search);
		searchButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				search();
			}
		});
		
		adapter = new SearchResultAdapter(this);
		resultListView = (ListView)findViewById(R.id.results);		
		resultListView.setAdapter(adapter);
		resultListView.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Subscription subscription = (Subscription)adapterView.getItemAtPosition(position);
				subscription.subscribed = !subscription.subscribed;
				numberOfSubscribed += (subscription.subscribed ? 1 : -1);				
				subscribeButton.setEnabled(numberOfSubscribed > 0);				
				adapter.notifyDataSetChanged();
			}			
		});
		
		subscribeButton = (Button)findViewById(R.id.subscribe);
		subscribeButton.setEnabled(false);
		subscribeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				subscribe();
			}
		});
		
		Button cancelButton = (Button)findViewById(R.id.cancel);
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	private void setBusy() {
		searchOrProgress.setDisplayedChild(1);
	}
	
	private void setIdle() {
		searchOrProgress.setDisplayedChild(0);
	}
	
	private void clearSearchResults() {		
		adapter.setResults(new ArrayList<Subscription>());
		subscribeButton.setEnabled(false);
		numberOfSubscribed = 0;
	}
	
	private void subscribe() {
		final ProgressDialog progressDialog = new ProgressDialog(this);
    	progressDialog.setMessage(getString(R.string.saving_subscriptions));
    	BetterAsyncTask<Void, Void, Void> savingTask = new BetterAsyncTask<Void, Void, Void>(this) {
			@Override
			protected void after(Context context, Void arg1) {				
				progressDialog.dismiss();				
				clearSearchResults();
				channelUrl.setText("http://");
			}
			@Override
			protected void handleError(Context context, Exception arg1) {
				progressDialog.dismiss();				
				Toast.makeText(context, arg1.getMessage(), 1000).show();
			}    		
    	};
    	
    	savingTask.disableDialog();
    	savingTask.setCallable(new BetterAsyncTaskCallable<Void, Void, Void>() {
			public Void call(BetterAsyncTask<Void, Void, Void> arg0) throws Exception {				
				for (Subscription subscription : adapter.getResults()) {
					if (subscription.subscribed) {
						Channel channel = new Channel(subscription.title, subscription.url);
						ContentManager.subscribe(channel);
					}
				}
				return null;
			}    		
    	});
    	progressDialog.show();
    	savingTask.execute();		
	}

	protected void search() {		
		setBusy();
		clearSearchResults();
		
    	BetterAsyncTask<Void, Void, List<Subscription>> searchTask = 
    		new BetterAsyncTask<Void, Void, List<Subscription>>(this) {
			@Override
			protected void after(Context context, List<Subscription> results) {				
				setIdle();
				adapter.setResults(results);
			}
			@Override
			protected void handleError(Context context, Exception e) {
				e.printStackTrace();
				setIdle();
			}
    	};
    	searchTask.disableDialog();
    	searchTask.setCallable(new BetterAsyncTaskCallable<Void, Void, List<Subscription>>() {
			public List<Subscription> call(BetterAsyncTask<Void, Void, List<Subscription>> arg0) 
				throws Exception {
				return doSearch();
			}    		
    	});    	
    	searchTask.execute();
	}
	
	private static final Pattern feedTag = Pattern.compile("<link[^>]*type=\"application/(atom|rss)\\+xml\"[^>]*/>");
	
	private List<Subscription> doSearch() throws Exception {			
		List<Subscription> subscriptions = new ArrayList<Subscription>();
		
		String baseUrl = channelUrl.getText().toString();		
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(baseUrl.trim());
		httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; ru; rv:1.9.2.3) Gecko/20100401 Firefox/4.0 (.NET CLR 3.5.30729)");
		
		HttpResponse response = (HttpResponse)client.execute(httpGet);
		StatusLine statusLine = response.getStatusLine();
		if (statusLine.getStatusCode() == 200) {
			String responseText = StreamUtils.readAllText(response.getEntity().getContent());			
			if (response.getFirstHeader("Content-Type").getValue().startsWith("text/html"));
			
			Matcher matcher = feedTag.matcher(responseText);
			while (matcher.find()) {				
				String tag = matcher.group();
				// find title="Feed title"
				int pos = tag.indexOf("title=\"");
				if (pos < 0) continue;
				pos += 7;
				String title = tag.substring(pos, tag.indexOf("\"", pos + 1));
				
				// find href="url to rss/atom feed"
				pos = tag.indexOf("href=\"");
				if (pos < 0) continue;
				pos += 6;
				String url = tag.substring(pos, tag.indexOf("\"", pos + 1));				
				url = processUrl(baseUrl, url);
				
				Subscription entry = new Subscription();
				entry.title = Html.decode(title);
				entry.url = url;
				entry.subscribed = false;
				
				subscriptions.add(entry);
			}
			
			/*
			JSONObject responseObject = new JSONObject(responseText);
			JSONObject responseData = responseObject.getJSONObject("responseData");			
			JSONArray entries = responseData.getJSONArray("entries");
			for (int i = 0; i < entries.length(); i++) {
				JSONObject entryObject = entries.getJSONObject(i);
				Subscription entry = new Subscription();
				entry.title = Html.toText(Html.decode(entryObject.getString("title")));
				entry.url = entryObject.getString("url");
				entry.subscribed = false;
				
				subscriptions.add(entry);
			}
			*/		
		}
		
		return subscriptions;
	}

	private String processUrl(String baseUrl, String url) {
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			if (url.startsWith("/")) {
				url = getRootUrl(baseUrl) + url;
			} else {
				url = getRelativeUrl(baseUrl) + "/" + url;
			}
		}
		return url;
	}
	
	private String getRootUrl(String baseUrl) {
		if (baseUrl == null) return baseUrl;
		if (!baseUrl.startsWith("http://")) {
			baseUrl = "http://" + baseUrl;
		}
		int indexOfSlash = baseUrl.indexOf("/", 8);
		if (indexOfSlash < 0) indexOfSlash = baseUrl.length();
		return baseUrl.substring(0, indexOfSlash);
	}
	
	private String getRelativeUrl(String baseUrl) {
		if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
			baseUrl = "http://" + baseUrl;
		}
		int indexOfSlash = baseUrl.lastIndexOf("/");
		if (indexOfSlash < (baseUrl.startsWith("https://") ? 8 : 7)) {
			indexOfSlash = baseUrl.length();
		}
		return baseUrl.substring(0, indexOfSlash);
	}
}
