package vn.evolus.news;

import java.util.Observable;
import java.util.Observer;

import vn.evolus.news.rss.Channel;
import vn.evolus.news.rss.Item;
import vn.evolus.news.util.ImageLoader;
import vn.evolus.news.widget.ItemListView;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.OnItemClickListener;

import com.github.droidfu.concurrent.BetterAsyncTask;
import com.github.droidfu.concurrent.BetterAsyncTaskCallable;

public class ChannelActivity extends Activity implements Observer {		
	private final int MENU_BACK = 0;
	private final int MENU_REFRESH = 1;
	
	private Channel channel;
	
	TextView channelName;	
	ItemListView itemListView;	
	ViewSwitcher refreshOrProgress;
	
	private Handler handler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		ImageLoader.initialize(this);
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.channel_view);		
		
		this.handler = new Handler();
		
		Button back = (Button)findViewById(R.id.back);
		back.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
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
        
        String channelUrl = getIntent().getStringExtra("ChannelUrl");
        String channelTitle = getIntent().getStringExtra("ChannelTitle");
        showChannel(new Channel(channelTitle, channelUrl));
	}		
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_BACK, 0, getString(R.string.back)).setIcon(R.drawable.ic_menu_back);
    	menu.add(0, MENU_REFRESH, 1,  getString(R.string.refresh)).setIcon(R.drawable.ic_menu_refresh);
		return super.onCreateOptionsMenu(menu);
	}	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == MENU_BACK) {
    		finish();
    	} else if (item.getItemId() == MENU_REFRESH){
    		refresh();
    	}
		return true;
	}
	
	private void setBusy() {
		refreshOrProgress.setDisplayedChild(1);
	}
	
	private void setIdle() {
		refreshOrProgress.setDisplayedChild(0);
	}	
	
	private void showChannel(Channel channel) {
		if (this.channel != channel) {
			if (this.channel != null) {
				this.channel.deleteObserver(this);
			}
			
			this.channel = channel;
			this.channel.addObserver(this);
			channelName.setText(channel.getTitle());
			//itemListView.setItems(channel.getItems());
			load();			
		}
	}
	
	private void load() {
		this.setBusy();
		
		BetterAsyncTask<Channel, Void, Channel> task = new BetterAsyncTask<Channel, Void, Channel>(this) {			
			protected void after(Context context, Channel channel) {
				itemListView.setItems(channel.getItems());
				onChannelUpdated(channel);				
			}			
			protected void handleError(Context context, Exception e) {
				Toast.makeText(context, "Cannot load the feed: " + e.getMessage(), 5).show();
				setIdle();
			}
		};
		task.setCallable(new BetterAsyncTaskCallable<Channel, Void, Channel>() {
			public Channel call(BetterAsyncTask<Channel, Void, Channel> task) throws Exception {
				channel.load();				
				return channel;
			}    			
		});
		task.disableDialog();
		task.execute();		       
	}
	
	private void refresh() {
		this.setBusy();
		
		BetterAsyncTask<Channel, Void, Channel> task = new BetterAsyncTask<Channel, Void, Channel>(this) {			
			protected void after(Context context, Channel channel) {				
				ChannelActivity.this.onChannelUpdated(channel);				
			}			
			protected void handleError(Context context, Exception e) {
				Toast.makeText(context, "Cannot load the feed: " + e.getMessage(), 5).show();
				ChannelActivity.this.setIdle();
			}			
		};
		task.setCallable(new BetterAsyncTaskCallable<Channel, Void, Channel>() {
			public Channel call(BetterAsyncTask<Channel, Void, Channel> task) throws Exception {
				ChannelActivity.this.channel.update();
				return ChannelActivity.this.channel;
			}    			
		});
		task.disableDialog();
		task.execute();		       
	}
	
	private void onChannelUpdated(Channel channel) {
		this.channel = channel;
		channelName.setText(channel.getTitle());
		this.setIdle();
	}

	private void showItem(Item item) { 
		Intent intent = new Intent(this, ItemActivity.class);		
		intent.putExtra("ItemLink", item.getLink());
		intent.putExtra("ChannelUrl", this.channel.getUrl());		
		startActivity(intent);		
	}		

	@Override
	public void update(Observable observable, Object data) {
		if (observable instanceof Channel) {
			Boolean updated = !(Boolean)data;
			if (updated) {
				this.handler.post(new Runnable() {
					public void run() {
						ChannelActivity.this.setIdle();			
					}					
				});			
			}
		}
	}
}
