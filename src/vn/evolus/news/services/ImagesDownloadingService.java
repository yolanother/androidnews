package vn.evolus.news.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import vn.evolus.news.ConnectivityReceiver;
import vn.evolus.news.model.Image;
import vn.evolus.news.util.ImageCache;
import vn.evolus.news.util.ImageLoader;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ImagesDownloadingService extends Service {
	private static final String TAG = "ContentsService";	
	
	private static final int DEFAULT_POOL_SIZE = 2;
	
	private static final int UPDATE_INTERVAL = 1000 * 60;
	
	private Object synRoot = new Object();
	private Object synDownload = new Object();
	private boolean downloading = false;	
	private Timer timer = new Timer();
	private ContentResolver cr;
	private int totalDownloads = 0;
	private static ThreadPoolExecutor executor;
		
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
		
		if (executor == null) {
            executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(DEFAULT_POOL_SIZE);
        }
		
		new Thread(new Runnable() {
			public void run() {
				startDownloadingImages();
			}			
		}).start();
	}
	
	@Override
	public void onDestroy() {
		stopDownloadingImages();
		super.onDestroy();
	}
	
	private void stopDownloadingImages() {
		Log.d(TAG, "Stop downloading images at " + new Date());
		synchronized (synRoot) {
			downloading = false;			
		}
		if (timer != null) {
			timer.cancel();
			timer = null;
		}		
	}
	
	protected void startDownloadingImages() {
		synchronized (synRoot) {
			if (downloading) return;
			downloading = true;
		}
		
		Log.d(TAG, "Start downloading images at " + new Date());
		if (ConnectivityReceiver.hasGoodEnoughNetworkConnection(this)) {
			ArrayList<Image> queuedImages = Image.loadAllQueuedImages(cr);
			totalDownloads = queuedImages.size();
			
			for (final Image image: queuedImages) {
				if (!downloading) break;
				
				try {								
					Log.d(TAG, "Start downloading image " + image.getUrl());
					image.setStatus(Image.IMAGE_STATUS_DOWNLOADING);
					image.save(cr);
					
					downloadImage(image.getUrl(), new DownloadCallback() {
						public void onComplete() {
							synchronized (synDownload) {
								totalDownloads--;
							}
							image.setStatus(Image.IMAGE_STATUS_DOWNLOADED);
							image.save(cr);
						}
						public void onFailed() {
							synchronized (synDownload) {
								totalDownloads--;
							}
							if (image.getRetries() == Image.MAX_RETRIES) {
								image.setStatus(Image.IMAGE_STATUS_FAILED);								
								image.save(cr);
							} else {
								image.setStatus(Image.IMAGE_STATUS_QUEUED);
								image.increaseRetries();
								image.save(cr);
							}
						}						
					});
				} catch (Exception e) {		
					Log.e(TAG, e.getMessage());
					e.printStackTrace();
				}						
			}
		}
		
		while (totalDownloads > 0 && downloading) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {				
				e.printStackTrace();
				break;
			}
		}
				
		synchronized (synRoot) {
			downloading = false;
		}		
		
		scheduleNextDownload();
		
		stopSelf();
	}		
	
	private void downloadImage(final String imageUrl, final DownloadCallback callback) {
		if (ImageCache.getInstance().containsKey(imageUrl)) {
			callback.onComplete();
			return;
		}
		
		executor.execute(new Runnable() {
			public void run() {
	            try {	            	
	                ImageCache.downloadImage(imageUrl);
	                callback.onComplete();
	            } catch (Throwable e) {
	            	e.printStackTrace();
	            	callback.onFailed();
	            }		        
			}
		});
    }

	private void scheduleNextDownload() {
		AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(this, ImagesDownloadingService.class);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);		
		long firstWake = System.currentTimeMillis() + UPDATE_INTERVAL;
		am.setRepeating(AlarmManager.RTC, firstWake, UPDATE_INTERVAL, pendingIntent);
	}
	
	private interface DownloadCallback {
		void onComplete();
		void onFailed();
	}
}
