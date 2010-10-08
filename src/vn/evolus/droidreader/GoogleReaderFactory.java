package vn.evolus.droidreader;

import oauth.signpost.exception.OAuthException;
import android.content.Context;

import com.google.reader.GoogleReader;

public class GoogleReaderFactory {
	private static final String CONSUMER_KEY = "droidreader.appspot.com";
	private static final String CONSUMER_SECRET = "ZlhExrqtn1xX9muIbygRCzZt";		
	
	private static Context context;
	static {
		context = Application.getInstance();
	}
	
	public static GoogleReader getGoogleReader() {
		GoogleReader reader = new GoogleReader(CONSUMER_KEY, CONSUMER_SECRET, 
				context.getString(R.string.applicationName));		

		String accessToken = Settings.getGoogleReaderAccessToken();		
		if (accessToken != null) {
			String tokenSecret = Settings.getGoogleReaderTokenSecret();
			try {
				reader.authorize(accessToken, tokenSecret);
			} catch (OAuthException e) {
				e.printStackTrace();
			}								
		}
		
		return reader;
	}
}
