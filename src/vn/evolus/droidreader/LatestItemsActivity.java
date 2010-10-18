package vn.evolus.droidreader;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import vn.evolus.droidreader.adapter.ItemAdapter;
import vn.evolus.droidreader.adapter.TagAdapter;
import vn.evolus.droidreader.adapter.ItemAdapter.OnItemRequestListener;
import vn.evolus.droidreader.adapter.TagAdapter.TagItem;
import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.content.SynchronizationListener;
import vn.evolus.droidreader.content.SynchronizationManager;
import vn.evolus.droidreader.content.criteria.LatestItems;
import vn.evolus.droidreader.model.Item;
import vn.evolus.droidreader.model.Tag;
import vn.evolus.droidreader.services.ContentSynchronizationService;
import vn.evolus.droidreader.services.DownloadingService;
import vn.evolus.droidreader.services.ImageDownloadingService;
import vn.evolus.droidreader.services.SynchronizationService;
import vn.evolus.droidreader.util.ActiveList;
import vn.evolus.droidreader.util.ImageCache;
import vn.evolus.droidreader.util.ImageLoader;
import vn.evolus.droidreader.widget.ItemListView;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
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
	
	private ViewSwitcher viewSwitcher;
	private TextView title;
	private TextView status;
	private ItemListView itemListView;
	private ViewSwitcher refreshOrProgress;
	
	private int tagId = LatestItems.ALL_TAGS;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ImageLoader.initialize(this);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.latest_items_view);
						
		title = (TextView)findViewById(R.id.title);
		title.setText(title.getText().toString().toUpperCase());
		
		status = (TextView)findViewById(R.id.status);
		        
        viewSwitcher = (ViewSwitcher)findViewById(R.id.viewSwitcher);
        
        ImageButton tagsButton = (ImageButton)findViewById(R.id.tags);        
        tagsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTagsDialog();
			}
        });
        
        ImageButton feedsButton = (ImageButton)findViewById(R.id.feeds);        
        feedsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showChannels();
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
		showReadToggle.setSelected(Settings.getShowRead());
		showReadToggle.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				v.setSelected(!v.isSelected());
				Settings.saveShowRead(v.isSelected());
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
        
        startServices();
        checkAndShowWhatsNew();
	}
	
	@Override
	protected void onStart() {	
		super.onStart();
		
		if (!Settings.isAuthenticated()) {			
			Intent intent = new Intent(this, AuthorizationActivity.class);
			startActivity(intent);			
			finish();
		} else if (Settings.getFirstTime()) {
			Settings.saveFirstTime();
			
			onFirstTime();
		}
		
		cancelNotification();
	}

	private void onFirstTime() {
		ImageCache.clearCacheFolder();
		
		Intent intent = new Intent(this, SubscriptionActivity.class);
		startActivity(intent);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);		
		tagId = intent.getIntExtra("TagId", LatestItems.ALL_TAGS);	
	}
	
	@Override
	protected void onResume() {
		super.onResume();
										
		if (tagId == LatestItems.ALL_TAGS) {
			tagId = getIntent().getIntExtra("TagId", LatestItems.ALL_TAGS);
		}
		if (tagId != LatestItems.ALL_TAGS) {
			Tag tag = ContentManager.loadTag(tagId);
			title.setText(tag.name.toUpperCase());
		}
		loadItems();
		
		SynchronizationManager.getInstance().registerSynchronizationListener(synchronizationListener);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		SynchronizationManager.getInstance().unregisterSynchronizationListener(synchronizationListener);
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

	private void startServices() {
		if (ConnectivityReceiver.hasGoodEnoughNetworkConnection()) {
			if (Constants.DEBUG_MODE) Log.d(Constants.LOG_TAG, "Begin startServices " + new Date());
        	Intent service = new Intent(this, SynchronizationService.class);
        	startService(service);
        	
        	Intent downloadService = new Intent(this, DownloadingService.class);
        	startService(downloadService);
        	if (Constants.DEBUG_MODE) Log.d(Constants.LOG_TAG, "End startServices " + new Date());
        }
	}
	
	private void checkAndShowWhatsNew() {		
		int versionCode = Settings.getVersion();
		try {
			ComponentName comp = new ComponentName(this, LatestItemsActivity.class);
			PackageInfo pinfo = this.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			if (pinfo.versionCode > versionCode) {
				Settings.saveVersion(versionCode);
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
		final ProgressDialog progress = new ProgressDialog(this);
		progress.setMessage(getString(R.string.logout));
		BetterAsyncTask<Void, Void, Void> logoutTask = 
			new BetterAsyncTask<Void, Void, Void>(this) {			
			protected void after(Context context, Void arg) {
				itemListView.setItems(new ActiveList<Item>());
				progress.dismiss();
			}			
			protected void handleError(Context context, Exception e) {
				progress.dismiss();
			}
		};
		logoutTask.setCallable(new BetterAsyncTaskCallable<Void, Void, Void>() {
			public Void call(BetterAsyncTask<Void, Void, Void> task) {
				SynchronizationManager.getInstance().stopSynchronizing();
				Settings.clearGoogleReaderAccessTokenAndSecret();				
				ContentManager.clearDatabase();
				ImageCache.clearCacheFolder();
				return null;
			}
		});
		logoutTask.disableDialog();
		progress.show();
		logoutTask.execute();		
	}
	
	private void showSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	private void setBusy() {
		refreshOrProgress.setDisplayedChild(1);
	}
	
	private void setIdle() {
		if (SynchronizationManager.getInstance().isSynchronizing()) return;
		
		refreshOrProgress.setDisplayedChild(0);
		hideProgress();
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
							new LatestItems(
									tagId, 
									!Settings.getShowRead(), 
									lastItem,
									LatestItems.OLDER,
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
				if (items == null || items.size() == 0) {
					viewSwitcher.setDisplayedChild(1);
				} else {
					viewSwitcher.setDisplayedChild(0);
				}
				
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
						new LatestItems(tagId, !Settings.getShowRead()), 
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
				if (!SynchronizationManager.getInstance().isSynchronizing()) {
					onItemsUpdated();
				}
			}			
			protected void handleError(Context context, Exception e) {
				setIdle();
			}			
		};
		task.setCallable(new BetterAsyncTaskCallable<Void, Void, Void>() {
			public Void call(BetterAsyncTask<Void, Void, Void> task) throws Exception {
				setBusy();
				SynchronizationManager.getInstance().startSynchronizing();				
				return null;
			}    			
		});
		task.disableDialog();
		task.execute();		       
	}
	
	private void onItemsUpdated() {
		this.setIdle();
	}

	private void showChannels() {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}
	
	private void showSubscriptions() {
		Intent intent = new Intent(this, SubscriptionActivity.class);
		startActivity(intent);
	}
	
	private void showItem(Item item) { 
		Intent intent = new Intent(this, ItemActivity.class);		
		intent.putExtra(ItemActivity.ITEM_ID_PARAM, item.id);
		intent.putExtra(ItemActivity.TAG_ID_PARAM, this.tagId);
		startActivity(intent);		
	}		

	private OnItemRequestListener onItemRequestListener = new OnItemRequestListener() {
		public void onRequest(Item lastItem) {
			loadMoreItems(lastItem);
		}
    };
    
    private SynchronizationListener synchronizationListener = new SynchronizationListener() {
    	public void onStart() {			
			runOnUiThread(new Runnable() {
				public void run() {
					setBusy();
				}
			});			
		}
    	
    	public void onProgress(String progressText) {
    		final String text = progressText;
			runOnUiThread(new Runnable() {
				public void run() {
					showProgress(text);
				}				
			});			
		}
    	
		public void onFinish(int totalNewItems) {			
			runOnUiThread(new Runnable() {
				public void run() {
					loadItems();
				}
			});			
		}
    };
    
    private void showProgress(String text) {
    	status.setVisibility(View.VISIBLE);
		status.setText(text);
	}
    
    private void hideProgress() {
    	status.setVisibility(View.GONE);
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
				title.setText(tagItem.title.toUpperCase());				
				loadItems();
				dialog.dismiss();
			}    		
    	});
		
    	TagAdapter tagAdapter = new TagAdapter(this, loadTags(this));
    	tagListView.setAdapter(tagAdapter);
    	dialog.setContentView(tagListView);
    	dialog.show();
    }
    
    public static ArrayList<TagItem> loadTags(Context context) {
		List<Tag> tags = ContentManager.loadAllTags();
		ArrayList<TagItem> tagItems = new ArrayList<TagItem>();
		
		// Starred items
		TagItem item = new TagItem();
		item.id = LatestItems.ALL_TAGS;
		item.title = context.getString(R.string.latest);
		item.icon = "" + R.drawable.latest;
		item.unreadCount = ContentManager.countUnreadItems();
		tagItems.add(item);
		
		// Starred items
		item = new TagItem();
		item.title = context.getString(R.string.starred);
		item.icon = "" + R.drawable.starred;		
		tagItems.add(item);
		
		// Shared items
		item = new TagItem();
		item.title = context.getString(R.string.shared);
		item.icon = "" + R.drawable.shared;
		tagItems.add(item);
		
		for (Tag tag : tags) {
			if (tag.type == Tag.STATE) {
				if (GoogleReader.STARRED.equals(tag.name)) {
					TagItem tagItem = tagItems.get(1);
					tagItem.id = tag.id;
					tagItem.unreadCount = tag.unreadCount;
				} else if (GoogleReader.SHARED.equals(tag.name)) {
					TagItem tagItem = tagItems.get(2);
					tagItem.id = tag.id;
					tagItem.unreadCount = tag.unreadCount;
				}
				continue;
			}
			
			item = new TagItem();
			item.id = tag.id;
			item.title = tag.name;
			item.unreadCount = tag.unreadCount;			
			tagItems.add(item);			
		}
		return tagItems;
	}
}
