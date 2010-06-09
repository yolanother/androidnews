package vn.evolus.news;

import java.util.ArrayList;
import java.util.Map;

import vn.evolus.news.rss.Channel;
import vn.evolus.news.services.ContentsUpdatingService;
import vn.evolus.news.services.ImagesDownloadingService;
import vn.evolus.news.util.ActiveList;
import vn.evolus.news.util.ImageLoader;
import vn.evolus.news.widget.ChannelListView;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.github.droidfu.activities.BetterDefaultActivity;
import com.github.droidfu.concurrent.BetterAsyncTask;
import com.github.droidfu.concurrent.BetterAsyncTaskCallable;

public class AndroidNews extends BetterDefaultActivity {
	private static final String TAG = "DEBUG";
	
	private final int MENU_REFRESH = 0;
	private final int MENU_ADD_CHANNEL = 1;
	private final int MENU_SETTINGS = 2;
	
	private ArrayList<Channel> channels = null;
	private ChannelListView channelListView;
	private ContentResolver cr;
		
	public AndroidNews() {
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		ImageLoader.initialize(this);				
		cr = getContentResolver();
		
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        channelListView = (ChannelListView)findViewById(R.id.channelListView);               
        channelListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Channel channel = (Channel)adapterView.getItemAtPosition(position);
				AndroidNews.this.showChannel(channel);
			}
        });        
        channelListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
				Channel channel = (Channel)adapterView.getItemAtPosition(position);
				showChannelOptions(channel);
				return true;
			}        	
        });
                
        loadData();
        
        if (ConnectivityReceiver.hasGoodEnoughNetworkConnection(this)) {
        	Intent service = new Intent(this, ContentsUpdatingService.class);
        	startService(service);
        	
        	Intent downloadService = new Intent(this, ImagesDownloadingService.class);
        	startService(downloadService);
        }
    }	
	
	@Override
	protected void onResume() {
		super.onResume();
		refreshUnreadCounts();
	}	
	    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {    	    	
    	menu.add(0, MENU_REFRESH, 1,  getString(R.string.refresh)).setIcon(R.drawable.ic_menu_refresh);
    	menu.add(0, MENU_ADD_CHANNEL, 1,  getString(R.string.add_channel)).setIcon(android.R.drawable.ic_menu_add);
    	menu.add(0, MENU_SETTINGS, 1,  getString(R.string.settings)).setIcon(android.R.drawable.ic_menu_preferences);
    	return super.onCreateOptionsMenu(menu);
    }        
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == MENU_SETTINGS) {
    		showSettings();
    	} else if (item.getItemId() == MENU_REFRESH){
    		refresh();
    	} else if (item.getItemId() == MENU_ADD_CHANNEL){
    		addChannel();
    	}
		return true;
	}
    
	private void showSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	private void loadChannels() {
		long lastTicks = System.currentTimeMillis();
		channels = Channel.loadAllChannels(cr);
		Log.d(TAG, "Loading channels data from database in " + (System.currentTimeMillis() - lastTicks) + "ms");
		if (channels == null || channels.isEmpty()) {
			createDefaultChannels();
		}		
	}

	private void createDefaultChannels() {
		channels = new ActiveList<Channel>();
		channels.add(new Channel("Engadget", "http://feeds.feedburner.com/androidnews/engadget"));		
		channels.add(new Channel("Gizmodo", "http://feeds.feedburner.com/androidnews/gizmodo"));
		channels.add(new Channel("TechCrunch", "http://feeds.feedburner.com/TechCrunch"));
		channels.add(new Channel("Lifehacker", "http://feeds.feedburner.com/androidnews/lifehacker"));
		channels.add(new Channel("Download Squad", "http://feeds.feedburner.com/androidnews/downloadsquad"));
		channels.add(new Channel("GigaOM", "http://feeds.feedburner.com/ommalik"));
		channels.add(new Channel("Ars Technica", "http://feeds.arstechnica.com/arstechnica/everything"));
		channels.add(new Channel("Boy Genius Report", "http://feeds.feedburner.com/TheBoyGeniusReport"));
		channels.add(new Channel("ReadWriteWeb", "http://feeds.feedburner.com/readwriteweb"));
		channels.add(new Channel("The Design Blog", "http://feeds.feedburner.com/thedesignblog/ntLw"));
		channels.add(new Channel("Smashing Magazine", "http://feeds.feedburner.com/androidnews/smashingmagazine"));
		
		channels.add(new Channel("Android and Me", "http://feeds.feedburner.com/androidandme"));		
		channels.add(new Channel("AndroidGuys", "http://feeds.feedburner.com/androidguyscom"));
		channels.add(new Channel("Android Phone Fans", "http://feeds2.feedburner.com/AndroidPhoneFans"));
		channels.add(new Channel("Android Community", "http://feeds2.feedburner.com/AndroidCommunity"));
		channels.add(new Channel("AndroidSpin", "http://feeds.feedburner.com/androidspin"));
		channels.add(new Channel("Android Central", "http://feeds2.feedburner.com/androidcentral"));
		channels.add(new Channel("Google Android Blog", "http://feeds.feedburner.com/androinica"));
		
		channels.add(new Channel("Tinh tế", "http://feeds.feedburner.com/androidnews/tinhte"));		
		channels.add(new Channel("Số Hóa - Điện thoại", "http://feeds.feedburner.com/androidnews/sohoa/dienthoai"));
		channels.add(new Channel("Số Hóa - Máy tính", "http://feeds.feedburner.com/androidnews/sohoa/maytinh"));
		channels.add(new Channel("Số Hóa - Camera", "http://feeds.feedburner.com/androidnews/sohoa/camera"));
		channels.add(new Channel("Số Hóa - Hình ảnh", "http://feeds.feedburner.com/androidnews/sohoa/hinhanh"));
		channels.add(new Channel("Số Hóa - Âm thanh", "http://feeds.feedburner.com/androidnews/sohoa/amthanh"));
		channels.add(new Channel("Số Hóa - Đồ chơi số", "http://feeds.feedburner.com/androidnews/sohoa/dochoiso"));
		
		channels.add(new Channel("VnExpress - Xã hội", "http://feeds.feedburner.com/androidnews/vnexpress/xahoi"));
		channels.add(new Channel("VnExpress - Đời sống", "http://feeds.feedburner.com/androidnews/vnexpress/doisong"));
		channels.add(new Channel("VnExpress - Kinh doanh", "http://feeds.feedburner.com/androidnews/vnexpress/kinhdoanh"));
		channels.add(new Channel("VnExpress - Vi tính", "http://feeds.feedburner.com/androidnews/vnexpress/vitinh"));
		channels.add(new Channel("VnExpress - Ôtô & Xe máy", "http://feeds.feedburner.com/androidnews/vnexpress/oto-xemay"));
		channels.add(new Channel("VnExpress - Thế giới", "http://feeds.feedburner.com/androidnews/vnexpress/thegioi"));
		channels.add(new Channel("VnExpress - Thể thao", "http://feeds.feedburner.com/androidnews/vnexpress/thethao"));
		channels.add(new Channel("VnExpress - Văn hóa", "http://feeds.feedburner.com/androidnews/vnexpress/vanhoa"));
		channels.add(new Channel("VnExpress - Pháp luật", "http://feeds.feedburner.com/androidnews/vnexpress/phapluat"));
		channels.add(new Channel("VnExpress - Khoa học", "http://feeds.feedburner.com/androidnews/vnexpress/khoahoc"));			
		
		channels.add(new Channel("Tuổi Trẻ - Nhịp sống số", "http://feeds.feedburner.com/androidnews/tuoitre/nhipsongso"));
		channels.add(new Channel("Tuổi Trẻ - Chính trị - Xã hội", "http://feeds.feedburner.com/androidnews/tuoitre/chinhtri-xahoi"));		
		channels.add(new Channel("Tuổi Trẻ - Kinh tế", "http://feeds.feedburner.com/androidnews/tuoitre/kinhte"));
		channels.add(new Channel("Tuổi Trẻ - Văn hóa - Giải Trí", "http://feeds.feedburner.com/androidnews/tuoitre/vanhoa-giaitri"));
		channels.add(new Channel("Tuổi Trẻ - Thế giới", "http://feeds.feedburner.com/androidnews/tuoitre/thegioi"));
		channels.add(new Channel("Tuổi Trẻ - Giáo dục", "http://feeds.feedburner.com/androidnews/tuoitre/giaoduc"));
		channels.add(new Channel("Tuổi Trẻ - Thể thao", "http://feeds.feedburner.com/androidnews/tuoitre/thethao"));		
		channels.add(new Channel("Tuổi Trẻ - Nhịp sống trẻ", "http://feeds.feedburner.com/androidnews/tuoitre/nhipsongtre"));
		
		channels.add(new Channel("VietNamNet - CNTT - Viễn thông", "http://feeds.feedburner.com/androidnews/vietnamnet/cntt"));
		channels.add(new Channel("VietNamNet - Kinh tế", "http://feeds.feedburner.com/androidnews/vietnamnet/kinhte"));
		channels.add(new Channel("VietNamNet - Chính trị", "http://feeds.feedburner.com/androidnews/vietnamnet/chinhtri"));
		channels.add(new Channel("VietNamNet - Xã hội", "http://feeds.feedburner.com/androidnews/vietnamnet/xahoi"));
		channels.add(new Channel("VietNamNet - Khoa học", "http://feeds.feedburner.com/androidnews/vietnamnet/khoahoc"));
		channels.add(new Channel("VietNamNet - Văn hóa", "http://feeds.feedburner.com/androidnews/vietnamnet/vanhoa"));
		channels.add(new Channel("VietNamNet - Thế giới", "http://feeds.feedburner.com/androidnews/vietnamnet/thegioi"));		
		channels.add(new Channel("VietNamNet - Giáo dục", "http://feeds.feedburner.com/androidnews/vietnamnet/giaoduc"));
				
		channels.add(new Channel("Vietstock", "http://feeds.feedburner.com/androidnews/vietstock/chungkhoan"));
		channels.add(new Channel("CafeF", "http://feeds.feedburner.com/androidnews/cafef/chungkhoan"));
		
		saveChannels();
	}
		
	private void saveChannels() {
		try {
			for (Channel channel : this.channels) {
				channel.save(cr);
			}
			Log.d("DEBUG", "Successful saving channels to disk!");
		} catch (Exception e) {		
			e.printStackTrace();
		}
	}	        
        
	private void loadData() {
    	loadChannels();    	
    	channelListView.setChannels(channels);
    }
	
	private ArrayList<Channel> getUnreadChannels() {
		ArrayList<Channel> unreadChannels = new ActiveList<Channel>();
		if (this.channels != null) {
			for (Channel channel : channels) {
				if (channel.countUnreadItems() > 0) {
					unreadChannels.add(channel);
				}
			}
		}		
		return unreadChannels;
	}
	
	private void refreshUnreadCounts() {		
		BetterAsyncTask<Void, Void, Void> refreshTask = new BetterAsyncTask<Void, Void, Void>(this) {
			@Override
			protected void after(Context context, Void arg1) {
				if (Settings.getShowUpdatedChannels(context)) {
					Log.d("DEBUG", "Show updated channels only");
					ArrayList<Channel> unreadChannels = getUnreadChannels();				
					channelListView.setChannels(unreadChannels);
					if (unreadChannels.size() == 0) {
						Toast.makeText(context, context.getString(R.string.no_channel_has_new_items), 100)
							.show();
					}
				} else {
					Log.d("DEBUG", "Show all channels");
					channelListView.setChannels(channels);
					//channelListView.refresh();
				}
			}
			@Override
			protected void handleError(Context arg0, Exception arg1) {
			}    		
    	};
    	refreshTask.disableDialog();
    	refreshTask.setCallable(new BetterAsyncTaskCallable<Void, Void, Void>() {
			public Void call(BetterAsyncTask<Void, Void, Void> arg0)
					throws Exception {
				Map<Long, Integer> unreadCounts = Channel.countUnreadItems(cr);
				for (Channel channel : channels) {
					try {				
						if (unreadCounts.containsKey(channel.getId())) {							
							channel.setUnreadItems(unreadCounts.get(channel.getId()));
						} else {
							channel.setUnreadItems(0);
						}
					} catch (Exception e) {
						e.printStackTrace();
						Log.e("ERROR", e.getMessage());
					}
				}
				return null;
			}    		
    	});
    	refreshTask.execute();
	}      
	
	private void refresh() {
		final int maxItemsPerChannel = Settings.getMaxItemsPerChannel(this);
		BetterAsyncTask<Void, Void, Void> refreshTask = new BetterAsyncTask<Void, Void, Void>(this) {
			@Override
			protected void after(Context arg0, Void arg1) {
			}
			@Override
			protected void handleError(Context arg0, Exception arg1) {
			}    		
    	};
    	refreshTask.disableDialog();
    	refreshTask.setCallable(new BetterAsyncTaskCallable<Void, Void, Void>() {
			public Void call(BetterAsyncTask<Void, Void, Void> arg0)
					throws Exception {
				for (Channel channel : channels) {
					try {						
						channel.update(cr, maxItemsPerChannel);
						channelListView.refresh();
					} catch (Exception e) {
						Log.e("ERROR", e.getMessage());
					}
				}
				return null;
			}    		
    	});
    	refreshTask.execute();
	}
    
    private void showChannel(Channel channel) {
    	Intent intent = new Intent(this, ChannelActivity.class);
    	intent.putExtra("ChannelId", channel.getId());
    	intent.putExtra("ChannelTitle", channel.getTitle());
    	startActivity(intent);
    }
    
    private void showChannelOptions(final Channel channel) {
    	AlertDialog dialog = new AlertDialog.Builder(this)
    		.setTitle(channel.getTitle())
    		.setItems(new String[] {"Edit", "Delete"}, 
    			new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0: 
								dialog.dismiss();
								//editChannel(channel);
								break;
							case 1:
								dialog.dismiss();
								deleteChannel(channel);							
								break;
						}
					}    			
    			}
    		).create();
    	dialog.show();
	}
    
    private void deleteChannel(Channel channel) {
    	channel.delete(cr);
    	loadData();
    	refreshUnreadCounts();
    }
    
    private void addChannel() {
    	final EditText url = new EditText(this);
    	url.setHint(R.string.enter_your_rss_url_hint);
    	url.setText("http://");
    	url.setPadding(5, 0, 0, 5);
    	AlertDialog dialog = new AlertDialog.Builder(this)
    		.setTitle("Enter your RSS feed URL")
    		.setView(url)
    		.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {					
					startAddChannel(url.getText().toString());
					dialog.dismiss();
				}    			
    		})
    		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}    			
    		})
    		.create();
    	dialog.show();
    }
    
    private void startAddChannel(String url) {    	
    	final int maxItemsPerChannel = Settings.getMaxItemsPerChannel(this);
    	final Channel channel = new Channel(url);
    	if (Channel.exists(cr, channel)) {
    		String message = getString(R.string.channel_already_exists).replace("{url}", channel.getUrl());
			Toast.makeText(this, message, 1000).show();
    		return;
    	}
    	channel.save(cr);
    	
    	final ProgressDialog progressDialog = new ProgressDialog(this);
    	progressDialog.setMessage(getString(R.string.adding_channel));
    	BetterAsyncTask<Void, Void, Void> addChannelTask = new BetterAsyncTask<Void, Void, Void>(this) {
			@Override
			protected void after(Context context, Void arg1) {
				progressDialog.dismiss();
				if (channel.getItems().size() > 0) {
					channel.save(cr);
					String message = context.getString(R.string.add_channel_successfully).replace("{channel}", channel.getTitle());
					Toast.makeText(context, message, 1000).show();
					loadData();
				} else {
					channel.delete(cr);
					String message = context.getString(R.string.add_channel_failed).replace("{url}", channel.getUrl());
					Toast.makeText(context, message, 1000).show();					
				}				
			}
			@Override
			protected void handleError(Context arg0, Exception arg1) {
			}    		
    	};
    	addChannelTask.disableDialog();
    	addChannelTask.setCallable(new BetterAsyncTaskCallable<Void, Void, Void>() {
			public Void call(BetterAsyncTask<Void, Void, Void> arg0) throws Exception {
				channel.update(cr, maxItemsPerChannel);
				return null;
			}    		
    	});
    	progressDialog.show();
    	addChannelTask.execute();    	
    }
    
    @Override
    public void finish() {
    	super.finish();
    }
        
}