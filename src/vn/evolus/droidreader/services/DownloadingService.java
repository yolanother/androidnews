package vn.evolus.droidreader.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import vn.evolus.droidreader.Application;
import vn.evolus.droidreader.ConnectivityReceiver;
import vn.evolus.droidreader.Constants;
import vn.evolus.droidreader.Settings;
import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.model.Image;
import vn.evolus.droidreader.util.ImageCache;
import vn.evolus.droidreader.util.ImageLoader;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DownloadingService extends Service {
	private static final String TAG = "DownloadingService";	
	
	private static final int DEFAULT_POOL_SIZE = 2;	
	private static final int UPDATE_INTERVAL = 1000 * 30;
	
	private Object synRoot = new Object();
	private Object synDownload = new Object();
	private boolean downloading = false;
	private int totalDownloads = 0;
	private ThreadPoolExecutor executor;
		
	@Override
	public IBinder onBind(Intent intent) {
		return null;
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
		super.onDestroy();
		stopDownloadingImages();
	}
	
	private void stopDownloadingImages() {		
		synchronized (synRoot) {
			downloading = false;
		}		
		if (executor != null) {
			executor.shutdownNow();
		}
	}
	
	protected void startDownloadingImages() {
		if (!Settings.getDownloadImages()) {
			return;
		}
		
		synchronized (synRoot) {
			if (downloading) return;
			downloading = true;
		}
		
		if (Constants.DEBUG_MODE) Log.d(TAG, "Start downloading service at " + new Date());
		if (ConnectivityReceiver.hasGoodEnoughNetworkConnection() &&
				Settings.getDownloadImages()) {
			try {
				downloadImages();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		synchronized (synRoot) {
			downloading = false;
		}		
		if (Constants.DEBUG_MODE) Log.d(TAG, "Stop downloading service at " + new Date());
		
		if (Settings.getDownloadImages()) {
			scheduleNextDownload();
		}
		stopSelf();
	}

	private void downloadImages() {
		ArrayList<Image> queuedImages = ContentManager.loadAllQueuedImages();
		totalDownloads = queuedImages.size();
		
		for (final Image image: queuedImages) {
			if (!downloading) return;
			
			try {
				image.status = Image.IMAGE_STATUS_DOWNLOADING;
				ContentManager.saveImage(image);
								
				downloadImage(image.url, image.id, new DownloadCallback() {
					public void onComplete() {
						synchronized (synDownload) {
							totalDownloads--;
						}
						image.status = Image.IMAGE_STATUS_DOWNLOADED;
						ContentManager.saveImage(image);
					}
					
					public void onSkip() {
						synchronized (synDownload) {
							totalDownloads--;
						}
					}
					public void onFail() {
						synchronized (synDownload) {
							totalDownloads--;
						}
						if (image.retries == Image.MAX_RETRIES) {
							image.status = Image.IMAGE_STATUS_FAILED;								
							ContentManager.saveImage(image);
						} else {
							image.status = Image.IMAGE_STATUS_QUEUED;
							image.increaseRetries();
							ContentManager.saveImage(image);
						}
					}						
				});
			} catch (Exception e) {
				synchronized (synDownload) {
					totalDownloads--;
				}
				e.printStackTrace();
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
	}		
	
	private void downloadImage(final String imageUrl, final int imageId, final DownloadCallback callback) {
		if (ImageCache.isCached(imageUrl)) {
			callback.onComplete();
			return;
		}
		
		try {
			executor.execute(new Runnable() {
				public void run() {
		            try {
		            	if (downloading && ConnectivityReceiver.hasGoodEnoughNetworkConnection()) {
		            		if (Constants.DEBUG_MODE) Log.d(TAG, "Download #" + imageId + "(" + imageUrl + ")");
		            		ImageCache.downloadImage(imageUrl);
		            		callback.onComplete();
		            	} else {
		            		callback.onSkip();
		            	}
		            } catch (Throwable e) {
		            	e.printStackTrace();
		            	callback.onFail();
		            }
				}
			});
		} catch (Throwable t) {
			callback.onFail();
		}
    }

	private void scheduleNextDownload() {
		AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(this, DownloadingService.class);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);		
		long firstWake = System.currentTimeMillis() + UPDATE_INTERVAL;
		am.set(AlarmManager.RTC, firstWake, pendingIntent);
	}
	
	public static void cancelScheduledDownloads() {
		Context context = Application.getInstance();
		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, DownloadingService.class);
		PendingIntent pendingIntent = PendingIntent.getService(Application.getInstance(), 0, intent, 0);
		am.cancel(pendingIntent);
	}
	
	private interface DownloadCallback {
		void onComplete();
		void onSkip();
		void onFail();
	}
}
