package vn.evolus.news.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import vn.evolus.news.rss.Channel;
import vn.evolus.news.util.ImageLoader;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ContentsService extends Service {
	private static final String TAG = "ContentsService";
	
	private static final int UPDATE_INTERVAL = 1000 * 60 * 5;
	private Object synRoot = new Object();
	private boolean updating = false;
	private Timer timer = null;
	
	private TimerTask updateFeedsTask = new TimerTask() {
		@Override
		public void run() {
			updateFeeds();
		}		
	};
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);	
		ImageLoader.initialize(this);
		if (timer == null) {
			timer = new Timer();			
			timer.schedule(updateFeedsTask, 500);			
		}
	}
	
	@Override
	public void onDestroy() {
		stopUpdatingFeeds();
		super.onDestroy();
	}
	
	public boolean isUpdatingFeed() {
		synchronized (synRoot) {
			return updating;
		}
	}
	
	protected void stopUpdatingFeeds() {
		synchronized (synRoot) {
			updating = false;
		}
		if (timer != null) timer.cancel();
	}
	
	protected void updateFeeds() {
		synchronized (synRoot) {
			if (updating) return;
			updating = true;
		}
		
		Log.d(TAG, "Start updating feeds at " + new Date());
		
		ContentResolver cr = getContentResolver();
		ArrayList<Channel> channels = Channel.loadAllChannels(cr);
		for (Channel channel : channels) {
			synchronized (synRoot) {
				if (!updating) break;
			}
			try {
				channel.update(cr);
				Thread.sleep(100);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		synchronized (synRoot) {
			updating = false;
		}		
		
		Log.d(TAG, "Scheduling next updating...");
		// schedule next update
		timer.schedule(updateFeedsTask, UPDATE_INTERVAL);
	}
}
