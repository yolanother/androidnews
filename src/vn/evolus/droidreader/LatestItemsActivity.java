package vn.evolus.droidreader;

import java.util.List;

import vn.evolus.droidreader.adapter.ItemAdapter;
import vn.evolus.droidreader.adapter.ItemAdapter.OnItemRequestListener;
import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.model.Item;
import vn.evolus.droidreader.services.ContentSynchronizationService;
import vn.evolus.droidreader.services.ImageDownloadingService;
import vn.evolus.droidreader.util.ActiveList;
import vn.evolus.droidreader.util.ImageCache;
import vn.evolus.droidreader.util.ImageLoader;
import vn.evolus.droidreader.widget.ItemListView;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

public class LatestItemsActivity extends LocalizedActivity {
	private static final int MAX_ITEMS = 15;
	
	private static final int MENU_LOGOUT = 1;
	
	private boolean loading = false;
	private ItemListView itemListView;
	private ViewSwitcher refreshOrProgress;
	
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
		
		ImageButton settingsButton = (ImageButton)findViewById(R.id.settings);
        settingsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showSettings();
			}        	
        });
				
        itemListView = (ItemListView)findViewById(R.id.itemListView);
        itemListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Item item = (Item)adapterView.getItemAtPosition(position);				
				showItem(item);
			}
        });
        ItemAdapter adapter = (ItemAdapter)itemListView.getAdapter();
        adapter.setItemRequestListener(new OnItemRequestListener() {
			public void onRequest(Item lastItem) {
				requestMoreItems(lastItem);
			}
        });
        
        if (ConnectivityReceiver.hasGoodEnoughNetworkConnection(this)) {
        	Intent service = new Intent(this, ContentSynchronizationService.class);
        	startService(service);
        	
        	Intent downloadService = new Intent(this, ImageDownloadingService.class);
        	startService(downloadService);
        }
	}
	
	@Override
	protected void onStart() {	
		super.onStart();
		
		if (!Settings.isAuthenticated(this)) {			
			Intent intent = new Intent(this, AuthorizationActivity.class);
			startActivity(intent);
			
			finish();			
		} else if (Settings.getFirstTime(this)) {
			Settings.saveFirstTime(this);
			
			Intent intent = new Intent(this, SubscriptionActivity.class);
			startActivity(intent);
		}
		
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(ContentSynchronizationService.NOTIFICATION_ID);
		
		showLatestItems(MAX_ITEMS);
	}
	
	@Override
	protected void onDestroy() {	
		super.onDestroy();
		ImageCache.clearCacheIfNecessary();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_LOGOUT, 0, R.string.logout);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == MENU_LOGOUT) {
			logout();
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void logout() {
		Settings.clearGoogleReaderAccessTokenAndSecret(this);
	}

	private void showSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}
	
	private void setBusy() {
		refreshOrProgress.setDisplayedChild(1);
	}
	
	private void setIdle() {
		refreshOrProgress.setDisplayedChild(0);
	}
	
	protected void requestMoreItems(final Item lastItem) {
		if (loading) return;
		
		loading = true;
		Log.d("DEBUG", "Loading more item later than " + lastItem.title);
		this.setBusy();
		BetterAsyncTask<Void, Void, List<Item>> loadMoreItemsTask = 
			new BetterAsyncTask<Void, Void, List<Item>>(this) {			
			protected void after(Context context, List<Item> items) {				
				ItemAdapter adapter = (ItemAdapter)itemListView.getAdapter();				
				adapter.addItems(items);
				if (items.size() < MAX_ITEMS) {
					adapter.setItemRequestListener(null);
				}
				setIdle();
				loading = false;
			}			
			protected void handleError(Context context, Exception e) {
				e.printStackTrace();
				Toast.makeText(context, "Cannot load more items: " + e.getMessage(), 5).show();
				setIdle();
				loading = false;
			}
		};
		loadMoreItemsTask.setCallable(new BetterAsyncTaskCallable<Void, Void, List<Item>>() {
			public List<Item> call(BetterAsyncTask<Void, Void, List<Item>> task) 
				throws Exception {				
				return ContentManager.loadOlderItems(lastItem, MAX_ITEMS, 
						ContentManager.LIGHTWEIGHT_ITEM_LOADER, 
						ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);				
			}
		});
		loadMoreItemsTask.disableDialog();
		loadMoreItemsTask.execute();
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
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}
	
	private void showItem(Item item) { 
		Intent intent = new Intent(this, ItemActivity.class);		
		intent.putExtra("ItemId", item.id);		
		startActivity(intent);		
	}		
}
