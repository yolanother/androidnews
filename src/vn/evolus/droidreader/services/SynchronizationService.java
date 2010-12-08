package vn.evolus.droidreader.services;

import vn.evolus.droidreader.Application;
import vn.evolus.droidreader.Constants;
import vn.evolus.droidreader.LatestItemsActivity;
import vn.evolus.droidreader.R;
import vn.evolus.droidreader.Settings;
import vn.evolus.droidreader.content.SynchronizationManager;
import vn.evolus.droidreader.util.ImageLoader;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class SynchronizationService extends Service {	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
		
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);		
		ImageLoader.initialize(this);
		new Thread(new Runnable() {
			public void run() {
				startSynchronization();
			}			
		}).start();
	}
	
	@Override
	public void onLowMemory() {	
		super.onLowMemory();
		SynchronizationManager.getInstance().stopSynchronizing();
	}
		
	protected void startSynchronization() {
		if (!Settings.getAutoUpdate()) {
			return;
		}
				
		int totalNewItems = SynchronizationManager.getInstance().startSynchronizing();
		if (totalNewItems > 0) {
			notifyNewItems(totalNewItems);
		}
							
		scheduleNextUpdate();
		stopSelf();
	}
	
	private void notifyNewItems(int totalNewItems) {
		Context context = Application.getInstance();
        Intent notifyIntent = new Intent(context, LatestItemsActivity.class);
        PendingIntent intent = PendingIntent.getActivity(context, 0, notifyIntent, android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        
        String ticker = getString(R.string.new_items_notification).replace("{total}", String.valueOf(totalNewItems));
        Notification notification = new Notification(R.drawable.icon, ticker, System.currentTimeMillis());
        notification.setLatestEventInfo(Application.getInstance(), getString(R.string.applicationName), ticker, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        
        if (Settings.getNotificationSound()) {
        	notification.defaults |= Notification.DEFAULT_SOUND;
        }
        if (Settings.getNotificationVibrate()) {
        	notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        if (Settings.getNotificationLight()) {
        	notification.ledARGB = 0xff00ff00;
        	notification.ledOnMS = 300;
        	notification.ledOffMS = 1000;
        	notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        }
        
//        try {
//        	startForeground(Constants.NOTIFICATION_ID, notification);
//        } catch (Throwable t) {
        	NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        	notificationManager.notify(Constants.NOTIFICATION_ID, notification);
//        }
	}

	private void scheduleNextUpdate() {		
		AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(this, SynchronizationService.class);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);		
		int updateInterval = Settings.getUpdateInterval() * 1000 * 60;
		long firstWake = System.currentTimeMillis() + updateInterval;
		am.set(AlarmManager.RTC, firstWake, pendingIntent);
	}	
}
