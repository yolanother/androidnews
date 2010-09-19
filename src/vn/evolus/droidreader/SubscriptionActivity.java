package vn.evolus.droidreader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

import vn.evolus.droidreader.adapter.SuggestedChannelsAdapter;
import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.model.Subscription;
import vn.evolus.droidreader.model.SubscriptionGroup;
import vn.evolus.droidreader.util.StreamUtils;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ExpandableListView.OnChildClickListener;

import com.github.droidfu.concurrent.BetterAsyncTask;
import com.github.droidfu.concurrent.BetterAsyncTaskCallable;

public class SubscriptionActivity extends LocalizedActivity {	
	private ExpandableListView listView;
	private SuggestedChannelsAdapter adapter;
	private Button saveButton;
	
	private Map<String, Channel> newSubscriptions = new HashMap<String, Channel>();
	private Map<String, Channel> removedSubscriptions = new HashMap<String, Channel>();
	Map<String, Channel> channelMap = new HashMap<String, Channel>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.subscription);
				
		ImageButton addButton = (ImageButton)findViewById(R.id.add);
		addButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				newSubscription();
			}
		});		
		
		listView = (ExpandableListView)findViewById(R.id.channels);				
		loadSubscriptions();		
		listView.setOnChildClickListener(new OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v, 
					int groupPosition, int childPosition, long id) {
				Subscription subscription = (Subscription)adapter.getChild(groupPosition, childPosition);
				subscription.subscribed = !subscription.subscribed;
				adapter.notifyDataSetChanged();
				changeSubscription(subscription);				
				return true;
			}			
		});
		
		saveButton = (Button)findViewById(R.id.save);
		saveButton.setEnabled(false);
		saveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				confirmBeforeSavingSubscriptions();				
			}
		});
		
		Button cancelButton = (Button)findViewById(R.id.cancel);
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	private void confirmBeforeSavingSubscriptions() {
		if (removedSubscriptions.size() == 0) {
			saveSubscriptions();
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (Channel channel : removedSubscriptions.values()) {
			sb.append("* ");
			sb.append(channel.title);
			sb.append("\n");
		}
		
		String confirmMessage = getString(R.string.unsubscribe_confirmation)
			.replace("{feeds}", sb.toString()); 
    	AlertDialog dialog = new AlertDialog.Builder(this)
			.setTitle(R.string.confirmation)
			.setMessage(confirmMessage)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					saveSubscriptions();
				}								
			})
			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.create();
		dialog.show();
	}
	
	private void saveSubscriptions() {
		final ProgressDialog progressDialog = new ProgressDialog(this);
    	progressDialog.setMessage(getString(R.string.saving_subscriptions));
    	BetterAsyncTask<Void, Void, Void> savingTask = new BetterAsyncTask<Void, Void, Void>(this) {
			@Override
			protected void after(Context context, Void arg1) {				
				progressDialog.dismiss();				
				loadSubscriptions();
				saveButton.setEnabled(false);
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
				save();
				return null;
			}    		
    	});
    	progressDialog.show();
    	savingTask.execute();
	}

	protected void save() throws Exception {
		boolean failed = false;
		StringBuffer failedToSave = new StringBuffer();
		failedToSave.append(getString(R.string.failed_to_save_subscriptions));
		
		for (String url : newSubscriptions.keySet()) {
			Channel channel = newSubscriptions.get(url);
			try {
				ContentManager.subscribe(channel);
			} catch (Exception e) {
				failed = true;
				failedToSave.append("\t" + channel.title + "\n");
				e.printStackTrace();
			}
		}
		newSubscriptions.clear();
		
		for (String url : removedSubscriptions.keySet()) {
			Channel channel = removedSubscriptions.get(url);
			try {
				ContentManager.unsubscribe(channel);
			} catch (Exception e) {
				failed = true;
				failedToSave.append(channel.title + "\n");
				e.printStackTrace();
			}
		}
		removedSubscriptions.clear();
		
		if (failed) {
			throw new Exception(failedToSave.toString());
		}
	}

	protected void changeSubscription(final Subscription subscription) {
		if (subscription.id == 0) {
			// new channel
			if (newSubscriptions.containsKey(subscription.url)) {
				newSubscriptions.remove(subscription.url);
			} else {
				newSubscriptions.put(subscription.url, new Channel(subscription.title, subscription.url));
			}
		} else {
			// existing channel
			if (removedSubscriptions.containsKey(subscription.url)) {
				removedSubscriptions.remove(subscription.url);
			} else {
				removedSubscriptions.put(subscription.url, channelMap.get(subscription.url));
			}
		}
		
		if (newSubscriptions.size() > 0 || removedSubscriptions.size() > 0) {
			saveButton.setEnabled(true);
		} else {
			saveButton.setEnabled(false);
		}
	}

	private void newSubscription() {
		Intent intent = new Intent(this, NewSubscriptionActivity.class);
    	startActivity(intent);
	}	
	
	private void loadSubscriptions() {
		List<SubscriptionGroup> subscriptionGroups = new ArrayList<SubscriptionGroup>();
		InputStream is = null;
		try {
			is = getResources().openRawResource(R.raw.subscriptions);
			JSONArray channelGroupsArray = new JSONArray(StreamUtils.readAllText(is));
			for (int i = 0; i < channelGroupsArray.length(); i++) {
				SubscriptionGroup subscriptionGroup = SubscriptionGroup.fromJSON(channelGroupsArray.getJSONObject(i));
				subscriptionGroups.add(subscriptionGroup);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}		
		
		List<Channel> subscribedChannels = ContentManager.loadAllChannels(ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
		if (subscribedChannels.size() > 0) {
			checkSubscritions(subscriptionGroups, subscribedChannels);
			
			SubscriptionGroup subscriptionGroup = new SubscriptionGroup();		
			subscriptionGroup.setTitle(getString(R.string.your_subscriptions));
			List<Subscription> subscriptions = new ArrayList<Subscription>();
			for (Channel channel : subscribedChannels) {
				Subscription subscription = new Subscription();
				subscription.id = channel.id;
				subscription.url = channel.url;
				subscription.title = channel.title;
				subscription.subscribed = true;				
				subscriptions.add(subscription);
			}
			subscriptionGroup.setSubscriptions(subscriptions);
			subscriptionGroups.add(0, subscriptionGroup);
		}
		
		adapter = new SuggestedChannelsAdapter(this, subscriptionGroups);
		listView.setAdapter(adapter);
	}
	
	private void checkSubscritions(List<SubscriptionGroup> subscriptionGroups,
			List<Channel> subscribedChannels) {
		channelMap.clear();
		for (Channel channel : subscribedChannels) {
			channelMap.put(channel.url, channel);
		}
		for (SubscriptionGroup subscriptionGroup : subscriptionGroups) {			
			for (Subscription subscription : subscriptionGroup.getSubscriptions()) {
				if (channelMap.containsKey(subscription.url)) {
					subscription.id = channelMap.get(subscription.url).id;
					subscription.subscribed = true;
				}
			}
		}
	}
}
