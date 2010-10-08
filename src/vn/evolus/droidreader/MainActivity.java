package vn.evolus.droidreader;

import java.util.ArrayList;
import java.util.Map;

import vn.evolus.droidreader.adapter.ChannelAdapter;
import vn.evolus.droidreader.adapter.TagAdapter;
import vn.evolus.droidreader.adapter.TagAdapter.TagItem;
import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.util.ActiveList;
import vn.evolus.droidreader.util.ImageLoader;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.github.droidfu.concurrent.BetterAsyncTask;
import com.github.droidfu.concurrent.BetterAsyncTaskCallable;

public class MainActivity extends LocalizedActivity {
	private static final int GRID_MODE = 0;
	//private static final int LIST_MODE = 1;
	
	private ArrayList<Channel> channels = null;
	private ViewSwitcher viewSwitcher;
	private GridView channelGridView;
	private ListView channelListView;
	private ChannelAdapter channelAdapter;
	private TagAdapter tagAdapter = null;
			
	public MainActivity() {
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		ImageLoader.initialize(this);
		
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        TextView title = (TextView)findViewById(R.id.toolbarTitle);
        title.setText(title.getText().toString().toUpperCase());
        
        ImageButton viewModeButton = (ImageButton)findViewById(R.id.viewMode);        
        viewModeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				v.setSelected(!v.isSelected());
				toggleViewMode();
			}
        });
        
        ImageButton editButton = (ImageButton)findViewById(R.id.edit);        
        editButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				manageSubscriptions();
			}
        });
        
        ImageButton settingsButton = (ImageButton)findViewById(R.id.settings);
        settingsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startSettingsActivity();
			}        	
        });
        
        viewSwitcher = (ViewSwitcher)findViewById(R.id.viewSwitcher);        
        channelGridView = (GridView)findViewById(R.id.channelGridView);               
        channelGridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Channel channel = (Channel)adapterView.getItemAtPosition(position);
				MainActivity.this.showChannel(channel);
			}
        });
        channelGridView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
				Channel channel = (Channel)adapterView.getItemAtPosition(position);
				showChannelOptions(channel);
				return true;
			}        	
        });

        channelListView = (ListView)findViewById(R.id.channelListView);               
        channelListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				TagItem tagItem = (TagItem)adapterView.getItemAtPosition(position);
				Intent intent = new Intent(MainActivity.this, LatestItemsActivity.class);
				intent.putExtra("TagId", tagItem.id);
	        	startActivity(intent);
			}
        });
    }	
	
	@Override
	protected void onStart() {	
		super.onStart();
		loadData();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		refreshUnreadCounts();
	}	
	    
	private void startSettingsActivity() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	private void startSubscriptionActivity() {
		Intent intent = new Intent(this, SubscriptionActivity.class);
		startActivity(intent);
	}
        
	private void loadData() {
		if (getViewMode() == GRID_MODE) {
			if (channelAdapter == null) {
		    	loadChannels();    	
		    	channelAdapter = new ChannelAdapter(this, channels);
		    	channelGridView.setAdapter(channelAdapter);
			}
		} else {
			if (tagAdapter == null) {
				tagAdapter = new TagAdapter(this, LatestItemsActivity.loadTags(this));
				channelListView.setAdapter(tagAdapter);
			}
		}
    }
	
	private int getViewMode() {		
		return viewSwitcher.getDisplayedChild();
	}

	private void toggleViewMode() {		
		viewSwitcher.setDisplayedChild((viewSwitcher.getDisplayedChild() + 1) % 2);
		loadData();
	}
	
	private void loadChannels() {
		channels = ContentManager.loadAllChannels(ContentManager.WITH_IMAGE_CHANNEL_LOADER);
		if (channels == null || channels.isEmpty()) {
			channels = new ActiveList<Channel>();
			
			startSubscriptionActivity();
		}		
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
				if (Settings.getShowUpdatedChannels()) {					
					ArrayList<Channel> unreadChannels = getUnreadChannels();				
					channelAdapter.setChannels(unreadChannels);
					if (unreadChannels.size() == 0) {
						Toast.makeText(context, context.getString(R.string.no_channel_has_new_items), 100)
							.show();
					}
				} else {					
					channelAdapter.setChannels(channels);
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
				Map<Integer, Integer> unreadCounts = ContentManager.countUnreadItemsForEachChannel();
				for (Channel channel : channels) {
					try {				
						if (unreadCounts.containsKey(channel.id)) {							
							channel.setUnreadItems(unreadCounts.get(channel.id));
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
    
    private void showChannel(Channel channel) {
    	Intent intent = new Intent(this, ChannelActivity.class);
    	intent.putExtra("ChannelId", channel.id);
    	intent.putExtra("ChannelTitle", channel.title);
    	startActivity(intent);
    }
    
    private void showChannelOptions(final Channel channel) {
    	AlertDialog dialog = new AlertDialog.Builder(this)
    		.setTitle(channel.title)
    		.setItems(new String[] {getString(R.string.unsubscribe)}, 
    			new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0: 
								dialog.dismiss();
								confirmDeleteChannel(channel);							
								break;
						}
					}    			
    			}
    		).create();
    	dialog.show();
	}
    
    private void confirmDeleteChannel(final Channel channel) {
    	String confirmMessage = getString(R.string.unsubscribe_confirmation)
    		.replace("{feeds}", "* " + channel.title);
    	AlertDialog dialog = new AlertDialog.Builder(this)
			.setTitle(channel.title)
			.setMessage(confirmMessage)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					deleteChannel(channel);					
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
    
    private void deleteChannel(final Channel channel) {
    	final ProgressDialog progressDialog = new ProgressDialog(this);
    	progressDialog.setMessage(getString(R.string.unsubscribing_channel).replace("{channel}", channel.title));
    	BetterAsyncTask<Void, Void, Void> unsubscribingTask = new BetterAsyncTask<Void, Void, Void>(this) {
			@Override
			protected void after(Context context, Void arg1) {				
				progressDialog.dismiss();
				
				String message = getString(R.string.unsubscribe_successfully).replace("{channel}", channel.title);
				Toast.makeText(context, message, 1000).show();
				
				loadData();
		    	refreshUnreadCounts();
			}
			@Override
			protected void handleError(Context context, Exception arg1) {
				String message = getString(R.string.unsubscribe_failed).replace("{channel}", channel.title);
				Toast.makeText(context, message, 1000).show();
			}    		
    	};
    	
    	unsubscribingTask.disableDialog();
    	unsubscribingTask.setCallable(new BetterAsyncTaskCallable<Void, Void, Void>() {
			public Void call(BetterAsyncTask<Void, Void, Void> arg0) throws Exception {				
				ContentManager.unsubscribe(channel);
				return null;
			}    		
    	});
    	progressDialog.show();
    	unsubscribingTask.execute();     	    	
    }
    
    private void manageSubscriptions() {
    	Intent intent = new Intent(this, SubscriptionActivity.class);
    	startActivity(intent);
    }    
}