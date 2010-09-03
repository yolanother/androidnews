package vn.evolus.droidnews;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
	private static final String PREFS_NAME = "vn.evolus.droidnews_preferences";
	
	public static int getUpdateInterval(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return Integer.parseInt(prefs.getString(context.getString(R.string.update_interval_key), "5"));	
	}
	
	public static int getMaxItemsPerChannel(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return Integer.parseInt(prefs.getString(context.getString(R.string.max_items_per_channel_key), "20"));	
	}
	
	public static boolean getAutoUpdate(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);		
		return prefs.getBoolean(context.getString(R.string.auto_update_key), true);
	}
	
	public static boolean getShowUpdatedChannels(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean(context.getString(R.string.show_updated_channels_key), true);
	}
}
