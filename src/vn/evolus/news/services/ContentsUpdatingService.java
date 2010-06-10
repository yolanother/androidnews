package vn.evolus.news.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import vn.evolus.news.AndroidNews;
import vn.evolus.news.ConnectivityReceiver;
import vn.evolus.news.R;
import vn.evolus.news.Settings;
import vn.evolus.news.model.Channel;
import vn.evolus.news.util.ImageLoader;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ContentsUpdatingService extends Service {
	private static final String TAG = "ContentsService";
	private static final int NOTIFICATION_ID = 0;
		
	private Object synRoot = new Object();
	private boolean updating = false;	
	private Timer timer = new Timer();
	private ContentResolver cr;
		
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();		
		cr = getContentResolver();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		ImageLoader.initialize(this);
		timer.schedule(new TimerTask() {
			public void run() {
				updateFeeds();
			}
		}, 100);	
	}
	
	@Override
	public void onDestroy() {
		stopUpdatingFeeds();
		super.onDestroy();
	}
	
	private void stopUpdatingFeeds() {
		Log.d(TAG, "Stop updating feeds at " + new Date());
		synchronized (synRoot) {
			updating = false;			
		}
		if (timer != null) {
			timer.cancel();
			timer = null;
		}		
	}
	
	protected void updateFeeds() {
		synchronized (synRoot) {
			if (updating) return;
			updating = true;
		}
		
		Log.d(TAG, "Start updating feeds at " + new Date());
		
		int totalNewItems = 0;
		int maxItemsPerChannel = Settings.getMaxItemsPerChannel(this);
		if (ConnectivityReceiver.hasGoodEnoughNetworkConnection(this)) {
			ArrayList<Channel> channels = Channel.loadAllChannels(cr);
			for (Channel channel : channels) {
				synchronized (synRoot) {
					if (!updating) break;
				}
								
				try {
					int newItems = channel.update(cr, maxItemsPerChannel);
					if (newItems > 0) {
						channel.clean(cr, maxItemsPerChannel);
					}
					totalNewItems += newItems;
					channel.getItems().clear();
					Thread.sleep(100);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			// clean up memory
			channels.clear();
			channels = null;
		}
		
		synchronized (synRoot) {
			updating = false;
		}		
		
		scheduleNextUpdate();
		
		notifyNewItems(totalNewItems);
		
		stopSelf();
	}	
	
	private void notifyNewItems(int totalNewItems) {		
		if (totalNewItems == 0) return;
		
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
        Intent notifyIntent = new Intent(this, AndroidNews.class);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notifyIntent, android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        
        String ticker = getString(R.string.new_items_notification).replace("{total}", String.valueOf(totalNewItems));
        Notification notification = new Notification(R.drawable.icon, ticker, System.currentTimeMillis());
        notification.setLatestEventInfo(this, getString(R.string.applicationName), ticker, intent);
        
		notificationManager.notify(NOTIFICATION_ID, notification);
	}

	private void scheduleNextUpdate() {
		AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(this, ContentsUpdatingService.class);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);		
		int updateInterval = Settings.getUpdateInterval(this) * 1000 * 60;
		long firstWake = System.currentTimeMillis() + updateInterval;
		am.setRepeating(AlarmManager.RTC, firstWake, updateInterval, pendingIntent);
	}

}
