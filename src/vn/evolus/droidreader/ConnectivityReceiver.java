package vn.evolus.droidreader;

import vn.evolus.droidreader.services.DownloadingService;
import vn.evolus.droidreader.services.SynchronizationService;
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
		Intent updatingService = new Intent(context, SynchronizationService.class);
		Intent downloadingService = new Intent(context, DownloadingService.class);
		
		NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
		if (hasGoodEnoughNetworkConnection(info, context)) {
			// should start background service to update
			if (Constants.DEBUG_MODE) Log.d(Constants.LOG_TAG, "Have WIFI or 3G connection, start background services...");
			context.startService(updatingService);
			context.startService(downloadingService);
		} else {
			if (Constants.DEBUG_MODE) Log.d(Constants.LOG_TAG, "No WIFI or 3G connection, stop background services...");
			context.stopService(updatingService);
			context.stopService(downloadingService);
		}
	}
	
	public static boolean hasGoodEnoughNetworkConnection() {
		Context context = Application.getInstance();
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
		        && (netSubtype == TelephonyManager.NETWORK_TYPE_UMTS
			        	|| netSubtype == TelephonyManager.NETWORK_TYPE_EDGE
			        	|| netSubtype == TelephonyManager.NETWORK_TYPE_HSDPA
			        	|| netSubtype == TelephonyManager.NETWORK_TYPE_HSDPA
			        	|| netSubtype == TelephonyManager.NETWORK_TYPE_HSPA
			        	|| netSubtype == TelephonyManager.NETWORK_TYPE_EVDO_0
			        	|| netSubtype == TelephonyManager.NETWORK_TYPE_EVDO_A)) {
		   TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		   if (!telephonyManager.isNetworkRoaming()) {
			   return info.isConnected();
		   }
		}
		
		return false;		
	}
}
