package vn.evolus.news;

import vn.evolus.news.services.ContentsUpdatingService;
import vn.evolus.news.services.ImagesDownloadingService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

public class ConnectivityReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent updatingService = new Intent(context, ContentsUpdatingService.class);
		Intent downloadingService = new Intent(context, ImagesDownloadingService.class);
		
		NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
		if (hasGoodEnoughNetworkConnection(info, context)) {
			// should start background service to update
			Log.d("DEBUG", "Have WIFI or 3G connection, start updating feeds.");
			context.startService(updatingService);
			context.startService(downloadingService);
		} else {
			Log.d("DEBUG", "No WIFI or 3G connection, stop updating feeds.");
			context.stopService(updatingService);
			context.stopService(downloadingService);
		}
	}
	
	public static boolean hasGoodEnoughNetworkConnection(Context context) {
		NetworkInfo info = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();		
		return hasGoodEnoughNetworkConnection(info, context);
	}

	public static boolean hasGoodEnoughNetworkConnection(NetworkInfo info, Context context) {
		if (info == null) return false;
		// Only update if WiFi or 3G is connected and not roaming				
		int netType = info.getType();
		int netSubtype = info.getSubtype();
		if (netType == ConnectivityManager.TYPE_WIFI) {
		    return info.isConnected();
		} 
		if (netType == ConnectivityManager.TYPE_MOBILE
		        && netSubtype == TelephonyManager.NETWORK_TYPE_UMTS) {
		   TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		   if (!telephonyManager.isNetworkRoaming()) {
			   return info.isConnected();
		   }
		}
		
		return false;		
	}
}