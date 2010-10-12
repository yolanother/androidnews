package vn.evolus.droidreader;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
	public static final String PREFS_NAME = "vn.evolus.droidreader_preferences";
	private static Context context;
	
	static {
		context = Application.getInstance();
	}
		
	public static boolean getFirstTime() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);		
		return prefs.getBoolean(context.getString(R.string.first_time_key), true);
	}
	
	public static void saveFirstTime() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(context.getString(R.string.first_time_key), false);
		
		editor.putString(context.getString(R.string.update_interval_key), "15");
		editor.putString(context.getString(R.string.max_items_per_channel_key), "50");
		editor.putBoolean(context.getString(R.string.auto_update_key), true);
		editor.putBoolean(context.getString(R.string.show_updated_channels_key), false);
		editor.putString(context.getString(R.string.language_key), "en");
		
		editor.putString("font", "sans");
		editor.putString("font_size", "1.0em");
		editor.putBoolean("notification_sound", false);
		editor.putBoolean("notification_vibrate", false);
		editor.putBoolean("notification_light", true);
		
		editor.putInt("keep_max_items", 1000);
		
		editor.commit();
	}
	
	public static int getUpdateInterval() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return Integer.parseInt(prefs.getString(context.getString(R.string.update_interval_key), "5"));	
	}
	
	public static int getMaxItemsPerChannel() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return Integer.parseInt(prefs.getString(context.getString(R.string.max_items_per_channel_key), "20"));	
	}
	
	public static boolean getAutoUpdate() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);		
		return prefs.getBoolean(context.getString(R.string.auto_update_key), true);
	}
	
	public static boolean getShowUpdatedChannels() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean(context.getString(R.string.show_updated_channels_key), false);
	}

	public static String getLocale() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getString(context.getString(R.string.language_key), "en");
	}
	
	public static String getGoogleReaderAccessToken() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getString("google_reader_access_token", null);
	}
	public static String getGoogleReaderTokenSecret() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getString("google_reader_token_secret", null);
	}
	public static void saveGoogleReaderAccessTokenAndSecret(
			String accessToken, String tokenSecret) {		
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.putString("google_reader_access_token", accessToken);
		editor.putString("google_reader_token_secret", tokenSecret);
		
		editor.commit();
	}
	public static void clearGoogleReaderAccessTokenAndSecret() {		
		saveGoogleReaderAccessTokenAndSecret(null, null);
	}
	
	public static String getGoogleReaderRequestToken() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getString("google_reader_request_token", null);
	}
	public static String getGoogleReaderRequestTokenSecret() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getString("google_reader_request_token_secret", null);
	}
	public static void saveGoogleReaderRequestTokenAndSecret(
			String requestToken, String requestTokenSecret) {		
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.putString("google_reader_request_token", requestToken);
		editor.putString("google_reader_request_token_secret", requestTokenSecret);
		
		editor.commit();
	}
	public static void clearGoogleReaderRequestTokenAndSecret() {		
		saveGoogleReaderRequestTokenAndSecret(null, null);
	}

	public static boolean isAuthenticated() {
		return getGoogleReaderAccessToken() != null;
	}
	
	public static int getVersion() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getInt("version", 0);
	}
	
	public static void saveVersion(int versionCode) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);		
		SharedPreferences.Editor editor = prefs.edit();		
		editor.putInt("version", versionCode);				
		editor.commit();
	}
	
	public static boolean getShowRead() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean("show_read", true);
	}

	public static void saveShowRead(boolean showRead) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);		
		SharedPreferences.Editor editor = prefs.edit();		
		editor.putBoolean("show_read", showRead);				
		editor.commit();
	}

	public static String getFont() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getString("font", "sans");
	}
	public static String getFontSize() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getString("font_size", "1.0em");
	}
	public static boolean getNightReadingMode() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean("night_mode", false);
	}
	public static void saveNightReadingMode(boolean nightMode) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);		
		SharedPreferences.Editor editor = prefs.edit();		
		editor.putBoolean("night_mode", nightMode);				
		editor.commit();
	}

	public static boolean getNotificationSound() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean("notification_sound", false);
	}	
	public static boolean getNotificationVibrate() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean("notification_vibrate", false);
	}	
	public static boolean getNotificationLight() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean("notification_light", false);
	}

	public static int getKeepMaxItems() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getInt("keep_max_items", 1000);
	}
}
