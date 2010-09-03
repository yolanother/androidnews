package vn.evolus.droidnews;

import vn.evolus.droidnews.content.ContentManager;
import vn.evolus.droidnews.model.Item;
import vn.evolus.droidnews.services.ContentsUpdatingService;
import vn.evolus.droidnews.util.ActiveList;
import vn.evolus.droidnews.util.ImageLoader;
import vn.evolus.droidnews.widget.ItemListView;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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

public class HomeActivity extends Activity {		
	private final int MENU_BACK = 0;
	private final int MENU_REFRESH = 1;
	
	TextView channelName;
	ItemListView itemListView;	
	ViewSwitcher refreshOrProgress;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		ImageLoader.initialize(this);
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.latest_items_view);
				
		TextView title = (TextView)findViewById(R.id.title);
        title.setText(title.getText().toString().toUpperCase());
        
        ImageButton viewChannelsButton = (ImageButton)findViewById(R.id.channels);        
        viewChannelsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				viewChannels();
			}        	
        });
		
		refreshOrProgress = (ViewSwitcher)findViewById(R.id.refreshOrProgress);
		ImageButton refresh = (ImageButton)findViewById(R.id.refresh);
		refresh.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				refresh();
			}
		});
				
        itemListView = (ItemListView)findViewById(R.id.itemListView);
        itemListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Item item = (Item)adapterView.getItemAtPosition(position);				
				showItem(item);
			}
        });               
	}		
	
	@Override
	protected void onStart() {	
		super.onStart();
		
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(ContentsUpdatingService.NOTIFICATION_ID);
		
		showLatestItems(30);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
	
	private void showLatestItems(final int maxItems) {
		this.setBusy();		
		BetterAsyncTask<Void, Void, ActiveList<Item>> task = 
				new BetterAsyncTask<Void, Void, ActiveList<Item>>(this) {			
			protected void after(Context context, ActiveList<Item> items) {				
				itemListView.setItems(items);
				onItemsUpdated();
			}			
			protected void handleError(Context context, Exception e) {
				e.printStackTrace();
				Toast.makeText(context, "Cannot load the feed: " + e.getMessage(), 5).show();
				setIdle();
			}
		};
		task.setCallable(new BetterAsyncTaskCallable<Void, Void, ActiveList<Item>>() {
			public ActiveList<Item> call(BetterAsyncTask<Void, Void, ActiveList<Item>> task) 
				throws Exception {				
				return ContentManager.loadLatestItems(maxItems, 
						ContentManager.LIGHTWEIGHT_ITEM_LOADER, 
						ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);				
			}
		});
		task.disableDialog();
		task.execute();		       
	}		
	
	private void refresh() {
		this.setBusy();		
		BetterAsyncTask<Void, Void, Void> task = new BetterAsyncTask<Void, Void, Void>(this) {			
			protected void after(Context context, Void args) {				
				onItemsUpdated();				
			}			
			protected void handleError(Context context, Exception e) {
				Toast.makeText(context, "Cannot load the feed: " + e.getMessage(), 5).show();
				setIdle();
			}			
		};
		task.setCallable(new BetterAsyncTaskCallable<Void, Void, Void>() {
			public Void call(BetterAsyncTask<Void, Void, Void> task) throws Exception {				
				return null;
			}    			
		});
		task.disableDialog();
		task.execute();		       
	}
	
	private void onItemsUpdated() {		
		this.setIdle();
	}

	private void viewChannels() {
		Intent intent = new Intent(this, AndroidNews.class);
		startActivity(intent);
	}
	
	private void showItem(Item item) { 
		Intent intent = new Intent(this, ItemActivity.class);		
		intent.putExtra("ItemId", item.id);		
		startActivity(intent);		
	}	
}
