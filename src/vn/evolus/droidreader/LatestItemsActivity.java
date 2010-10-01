package vn.evolus.droidreader;

import java.util.ArrayList;
import java.util.List;

import vn.evolus.droidreader.adapter.ItemAdapter;
import vn.evolus.droidreader.adapter.TagAdapter;
import vn.evolus.droidreader.adapter.ItemAdapter.OnItemRequestListener;
import vn.evolus.droidreader.adapter.TagAdapter.TagItem;
import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.content.criteria.ItemOfTag;
import vn.evolus.droidreader.model.Item;
import vn.evolus.droidreader.model.Tag;
import vn.evolus.droidreader.services.ContentSynchronizationService;
import vn.evolus.droidreader.services.DownloadingService;
import vn.evolus.droidreader.services.ImageDownloadingService;
import vn.evolus.droidreader.services.SynchronizationService;
import vn.evolus.droidreader.util.ActiveList;
import vn.evolus.droidreader.util.ImageCache;
import vn.evolus.droidreader.util.ImageLoader;
import vn.evolus.droidreader.widget.ActionItem;
import vn.evolus.droidreader.widget.ItemListView;
import vn.evolus.droidreader.widget.QuickAction;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.OnItemClickListener;

import com.github.droidfu.concurrent.BetterAsyncTask;
import com.github.droidfu.concurrent.BetterAsyncTaskCallable;
import com.google.reader.GoogleReader;

public class LatestItemsActivity extends LocalizedActivity {	
	private static final int MENU_LOGOUT = 1;
	private static final int MENU_SUBSCRIPTONS = 2;
	private static final int MENU_SETTINGS = 3;
	
	private boolean loading = false;
	
	private TextView title;
	private ItemListView itemListView;
	private ViewSwitcher refreshOrProgress;
	private ImageView tagsButton;
	
	private int tagId = ItemOfTag.ALL_TAGS;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		ImageLoader.initialize(this);
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.latest_items_view);
				
		title = (TextView)findViewById(R.id.title);
        title.setText(title.getText().toString().toUpperCase());
        
        tagsButton = (ImageButton)findViewById(R.id.tags);        
        tagsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTagsDialog();
//				QuickAction readingOptions = createReadingOptions(tagsButton);
//				readingOptions.show();
			}
        });
                
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
		
		ImageButton showReadToggle = (ImageButton)findViewById(R.id.showRead);
		showReadToggle.setSelected(Settings.getShowRead(this));
		showReadToggle.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				v.setSelected(!v.isSelected());
				Settings.saveShowRead(LatestItemsActivity.this, v.isSelected());
				loadItems();
			}			
		});		
				
        itemListView = (ItemListView)findViewById(R.id.itemListView);
        itemListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Item item = (Item)adapterView.getItemAtPosition(position);				
				showItem(item);
			}
        });        
        
        checkAndShowWhatsNew(); 
             
//        try {
//			FileUtils.copyFile(new File("/data/data/vn.evolus.droidreader/databases/droidnews.db"), 
//					new File("/sdcard/droidnews.db"));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}        
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
			
			onFirstItem();
		}
		
		cancelNotification();				
		startSynchronizationService();				
	}

	private void onFirstItem() {
		ImageCache.clearCacheFolder();
		
		Intent intent = new Intent(this, SubscriptionActivity.class);
		startActivity(intent);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {	
		super.onNewIntent(intent);		
		tagId = intent.getIntExtra("TagId", ItemOfTag.ALL_TAGS);	
	}
	
	@Override
	protected void onResume() {
		super.onResume();
										
		if (tagId == ItemOfTag.ALL_TAGS) {
			tagId = getIntent().getIntExtra("TagId", ItemOfTag.ALL_TAGS);
		}
		if (tagId != ItemOfTag.ALL_TAGS) {
			Tag tag = ContentManager.loadTag(tagId);
			title.setText(tag.name.toUpperCase());
		}
		loadItems();
		
		Application.getInstance().registerOnNewItemsListener(onNewItemsListener);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Application.getInstance().unregisterOnNewItemsListener(onNewItemsListener);
	}

	@Override
	protected void onDestroy() {	
		super.onDestroy();
		ImageCache.clearCacheIfNecessary();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_LOGOUT, 0, R.string.logout).setIcon(R.drawable.ic_menu_logout);
		menu.add(0, MENU_SUBSCRIPTONS, 0, R.string.subscriptions).setIcon(android.R.drawable.ic_menu_edit);
		menu.add(0, MENU_SETTINGS, 0, R.string.settings).setIcon(android.R.drawable.ic_menu_preferences);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == MENU_LOGOUT) {
			logout();
		} else if (item.getItemId() == MENU_SUBSCRIPTONS) {
			showSubscriptions();
		} else if (item.getItemId() == MENU_SETTINGS) {
			showSettings();
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void cancelNotification() {
		NotificationManager notificationManager = 
			(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(Constants.NOTIFICATION_ID);
	}

	private void startSynchronizationService() {
		if (ConnectivityReceiver.hasGoodEnoughNetworkConnection(this)) {
        	Intent service = new Intent(this, SynchronizationService.class);
        	startService(service);
        	
        	Intent downloadService = new Intent(this, DownloadingService.class);
        	startService(downloadService);
        }
	}
	
	private void checkAndShowWhatsNew() {		
		int versionCode = Settings.getVersion(this);
		try {
			ComponentName comp = new ComponentName(this, LatestItemsActivity.class);
			PackageInfo pinfo = this.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			if (pinfo.versionCode > versionCode) {
				Settings.saveVersion(this, versionCode);
				showWhatsNew();
			}
		} catch (android.content.pm.PackageManager.NameNotFoundException e) {			
		}
	}
	
	private void showWhatsNew() {
		try {
			AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(this, ImageDownloadingService.class);
			PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
			am.cancel(pendingIntent);
			
			intent = new Intent(this, ContentSynchronizationService.class);
			pendingIntent = PendingIntent.getService(this, 0, intent, 0);
			am.cancel(pendingIntent);
		} catch (Throwable t) {
			t.printStackTrace();
		}
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
	
	protected void loadMoreItems(final Item lastItem) {
		if (loading) return;
		
		loading = true;		
		this.setBusy();
		BetterAsyncTask<Void, Void, List<Item>> loadMoreItemsTask = 
			new BetterAsyncTask<Void, Void, List<Item>>(this) {			
			protected void after(Context context, List<Item> items) {				
				ItemAdapter adapter = (ItemAdapter)itemListView.getAdapter();				
				adapter.addItems(items);
				if (items.size() < Constants.MAX_ITEMS) {
					adapter.setItemRequestListener(null);
				}
				setIdle();
				loading = false;
			}			
			protected void handleError(Context context, Exception e) {
				e.printStackTrace();
				setIdle();
				loading = false;
			}
		};
		loadMoreItemsTask.setCallable(new BetterAsyncTaskCallable<Void, Void, List<Item>>() {
			public List<Item> call(BetterAsyncTask<Void, Void, List<Item>> task) 
				throws Exception {							
					return ContentManager.loadItems(
							new ItemOfTag(
									tagId, 
									!Settings.getShowRead(LatestItemsActivity.this), 
									lastItem,
									ItemOfTag.OLDER,
									Constants.MAX_ITEMS), 
							ContentManager.LIGHTWEIGHT_ITEM_LOADER,
							ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);				
			}
		});
		loadMoreItemsTask.disableDialog();
		loadMoreItemsTask.execute();
	}
	
	private void loadItems() {
		this.setBusy();
		BetterAsyncTask<Void, Void, ActiveList<Item>> task = 
				new BetterAsyncTask<Void, Void, ActiveList<Item>>(this) {			
			protected void after(Context context, ActiveList<Item> items) {
				ItemAdapter adapter = (ItemAdapter)itemListView.getAdapter();
		        adapter.setItemRequestListener(onItemRequestListener);
				itemListView.setItems(items);				
				
				onItemsUpdated();
			}			
			protected void handleError(Context context, Exception e) {
				e.printStackTrace();
				setIdle();
			}
		};
		task.setCallable(new BetterAsyncTaskCallable<Void, Void, ActiveList<Item>>() {
			public ActiveList<Item> call(BetterAsyncTask<Void, Void, ActiveList<Item>> task) 
				throws Exception {
				List<Item> items = ContentManager.loadItems(
						new ItemOfTag(tagId, !Settings.getShowRead(LatestItemsActivity.this)), 
						ContentManager.LIGHTWEIGHT_ITEM_LOADER,
						ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);				
				ActiveList<Item> result = new ActiveList<Item>();
				result.addAll(items);
				return result;
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
				setIdle();
			}			
		};
		task.setCallable(new BetterAsyncTaskCallable<Void, Void, Void>() {
			public Void call(BetterAsyncTask<Void, Void, Void> task) throws Exception {
				startSynchronizationService();				
				loadItems();
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
	
	private void showSubscriptions() {
		Intent intent = new Intent(this, SubscriptionActivity.class);
		startActivity(intent);
	}
	
	private void showItem(Item item) { 
		Intent intent = new Intent(this, ItemActivity.class);		
		intent.putExtra("ItemId", item.id);		
		startActivity(intent);		
	}		

	private OnItemRequestListener onItemRequestListener = new OnItemRequestListener() {
		public void onRequest(Item lastItem) {
			loadMoreItems(lastItem);
		}
    };
    
    private OnNewItemsListener onNewItemsListener = new OnNewItemsListener() {
		public void onNewItems() {			
			runOnUiThread(new Runnable() {
				public void run() {
					loadItems();
				}
			});			
		}    	
    };        

    private QuickAction createReadingOptions(View anchor) {
    	final QuickAction quickAction = new QuickAction(anchor);
    	
    	OnClickListener onTagClickListener = new OnClickListener() {
    		@Override
    		public void onClick(View v) {
    			Tag tag = (Tag)v.getTag();
    			tagId = ItemOfTag.ALL_TAGS;
    			if (tag != null) {
    				tagId = tag.id;
    				title.setText(tag.name.toUpperCase());
    			} else {
    				title.setText(getResources().getString(R.string.latest).toUpperCase());
    			}
    			quickAction.dismiss();
    			loadItems();
    		}
    	};
    	
    	ActionItem latest = new ActionItem();		
		latest.setTitle(getResources().getString(R.string.latest));
		latest.setIcon(getResources().getDrawable(R.drawable.latest));
		latest.setOnClickListener(onTagClickListener);		
		
		ActionItem starred = new ActionItem();		
		starred.setTitle(getResources().getString(R.string.starred));
		starred.setIcon(getResources().getDrawable(R.drawable.star));
		starred.setOnClickListener(onTagClickListener);
		
		ActionItem shared = new ActionItem();		
		shared.setTitle(getResources().getString(R.string.shared));
		shared.setIcon(getResources().getDrawable(R.drawable.star));
		shared.setOnClickListener(onTagClickListener);
				
		quickAction.addActionItem(latest);
		quickAction.addActionItem(starred);
		quickAction.addActionItem(shared);
		
		List<Tag> tags = ContentManager.loadAllTags();
		for (Tag tag : tags) {
			if (tag.type == Tag.STATE) {
				if (GoogleReader.STARRED.equals(tag.name)) {
					tag.name = starred.getTitle();
					starred.setTag(tag);
				} else if (GoogleReader.SHARED.equals(tag.name)) {
					tag.name = shared.getTitle();
					shared.setTag(tag);
				}
				continue;
			}
			
			ActionItem tagItem = new ActionItem();
			tagItem.setTitle(tag.name);
			tagItem.setIcon(getResources().getDrawable(R.drawable.rss_tag));
			tagItem.setOnClickListener(onTagClickListener);
			tagItem.setTag(tag);
						
			quickAction.addActionItem(tagItem);			
		}
		
		quickAction.setAnimStyle(QuickAction.ANIM_AUTO);
		
		return quickAction;
    }
    
    private void showTagsDialog() {
    	final Dialog dialog = new Dialog(this);
    	dialog.setTitle(R.string.select_tag_dialog_title);
    	ListView tagListView = new ListView(this);
    	tagListView.setBackgroundColor(getResources().getColor(R.color.itemBackground));
    	tagListView.setCacheColorHint(getResources().getColor(R.color.itemBackground));
    	tagListView.setDivider(getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
    	tagListView.setOnItemClickListener(new OnItemClickListener() {
    		public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				TagItem tagItem = (TagItem)adapterView.getItemAtPosition(position);
				tagId = tagItem.id;
				title.setText(tag.name.toUpperCase());
				
				loadItems();
				dialog.dismiss();
			}    		
    	});
		
    	TagAdapter tagAdapter = new TagAdapter(this, loadTags());
    	tagListView.setAdapter(tagAdapter);
    	dialog.setContentView(tagListView);
    	dialog.show();
    }
    
    private ArrayList<TagItem> loadTags() {
		List<Tag> tags = ContentManager.loadAllTags();
		ArrayList<TagItem> tagItems = new ArrayList<TagItem>();
		
		// Starred items
		TagItem item = new TagItem();
		item.title = getString(R.string.starred);
		item.unreadCount = 100;			
		tagItems.add(item);
		
		// Shared items
		item = new TagItem();
		item.title = getString(R.string.shared);
		item.unreadCount = 100;			
		tagItems.add(item);
		
		for (Tag tag : tags) {
			if (tag.type == Tag.STATE) {
				if (GoogleReader.STARRED.equals(tag.name)) {
					tagItems.get(0).id = tag.id;
				} else if (GoogleReader.SHARED.equals(tag.name)) {
					tagItems.get(1).id = tag.id;
				}
				continue;
			}
			
			item = new TagItem();
			item.id = tag.id;
			item.title = tag.name;
			item.unreadCount = 1000;			
			tagItems.add(item);			
		}
		return tagItems;
	}
}
