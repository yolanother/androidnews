package vn.evolus.droidnews;

import vn.evolus.droidnews.content.ContentManager;
import vn.evolus.droidnews.model.Channel;
import vn.evolus.droidnews.model.Item;
import vn.evolus.droidnews.util.ImageLoader;
import vn.evolus.droidnews.widget.ItemListView;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.OnItemClickListener;

import com.github.droidfu.concurrent.BetterAsyncTask;
import com.github.droidfu.concurrent.BetterAsyncTaskCallable;

public class ChannelActivity extends Activity {		
		
	private Channel channel;	
	
	TextView channelName;
	ItemListView itemListView;	
	ViewSwitcher refreshOrProgress;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		ImageLoader.initialize(this);		
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.channel_view);
		
		ImageButton markAllAsReadButton = (ImageButton)findViewById(R.id.markAllAsRead);        
		markAllAsReadButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				markAllAsRead();
			}        	
        });
		
		refreshOrProgress = (ViewSwitcher)findViewById(R.id.refreshOrProgress);
		ImageButton refresh = (ImageButton)findViewById(R.id.refresh);
		refresh.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				refresh();
			}			
		});
		
		channelName = (TextView)findViewById(R.id.channelName);
        itemListView = (ItemListView)findViewById(R.id.itemListView);
        itemListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Item item = (Item)adapterView.getItemAtPosition(position);				
				showItem(item);
			}
        });
        
        long channelId = getIntent().getLongExtra("ChannelId", 0);
        channelName.setText(getIntent().getStringExtra("ChannelTitle"));
        channel = new Channel(channelId);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		showChannel(channel.id);
	}
		
	private void setBusy() {
		refreshOrProgress.setDisplayedChild(1);
	}
	
	private void setIdle() {
		refreshOrProgress.setDisplayedChild(0);
	}	
	
	private void showChannel(final long channelId) {
		this.setBusy();
		
		BetterAsyncTask<Channel, Void, Channel> task = new BetterAsyncTask<Channel, Void, Channel>(this) {			
			protected void after(Context context, Channel channel) {				
				itemListView.setItems(channel.getItems());
				onChannelUpdated(channel);
			}			
			protected void handleError(Context context, Exception e) {
				e.printStackTrace();
				Toast.makeText(context, "Cannot load the feed: " + e.getMessage(), 5).show();
				setIdle();
			}
		};
		task.setCallable(new BetterAsyncTaskCallable<Channel, Void, Channel>() {
			public Channel call(BetterAsyncTask<Channel, Void, Channel> task) throws Exception {
				channel = ContentManager.loadChannel(channelId, ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
				channel.loadLightweightItems();
				return channel;
			}    			
		});
		task.disableDialog();
		task.execute();		       
	}
	
	private void markAllAsRead() {
		if (this.channel != null) {
			ContentManager.markAllItemsOfChannelAsRead(this.channel);			
			this.itemListView.refresh();
		}
	}
	
	private void refresh() {
		this.setBusy();
		final int maxItemsPerChannel = Settings.getMaxItemsPerChannel(this);
		BetterAsyncTask<Void, Void, Void> task = new BetterAsyncTask<Void, Void, Void>(this) {			
			protected void after(Context context, Void args) {				
				onChannelUpdated(channel);				
			}			
			protected void handleError(Context context, Exception e) {
				Toast.makeText(context, "Cannot load the feed: " + e.getMessage(), 5).show();
				setIdle();
			}			
		};
		task.setCallable(new BetterAsyncTaskCallable<Void, Void, Void>() {
			public Void call(BetterAsyncTask<Void, Void, Void> task) throws Exception {
				channel.update(maxItemsPerChannel);
				return null;
			}    			
		});
		task.disableDialog();
		task.execute();		       
	}
	
	private void onChannelUpdated(Channel channel) {
		this.channel = channel;
		channelName.setText(channel.title);
		this.setIdle();
	}

	private void showItem(Item item) { 
		Intent intent = new Intent(this, ItemActivity.class);		
		intent.putExtra("ItemId", item.id);
		intent.putExtra("ChannelId", this.channel.id);		
		startActivity(intent);		
	}	
}
