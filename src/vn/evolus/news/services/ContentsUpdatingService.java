package vn.evolus.news.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import vn.evolus.news.ConnectivityReceiver;
import vn.evolus.news.rss.Channel;
import vn.evolus.news.util.ImageLoader;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ContentsUpdatingService extends Service {
	private static final String TAG = "ContentsService";
	
	private static final int UPDATE_INTERVAL = 1000 * 60 * 5;
	private Object synRoot = new Object();
	private boolean updating = false;
	private Timer timer = new Timer();
		
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();		
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
		
		if (ConnectivityReceiver.hasGoodEnoughNetworkConnection(this)) {
			ContentResolver cr = getContentResolver();			
			ArrayList<Channel> channels = Channel.loadAllChannels(cr);
			for (Channel channel : channels) {
				synchronized (synRoot) {
					if (!updating) break;
				}
								
				try {
					channel.update(cr);
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
		
		stopSelf();
	}	
	
	private void scheduleNextUpdate() {
		AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(this, ContentsUpdatingService.class);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);		
		long firstWake = System.currentTimeMillis() + UPDATE_INTERVAL;
		am.setRepeating(AlarmManager.RTC, firstWake, UPDATE_INTERVAL, pendingIntent);
	}
}
