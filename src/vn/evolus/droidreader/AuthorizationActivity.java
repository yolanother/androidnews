package vn.evolus.droidreader;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;

import oauth.signpost.OAuth;
import oauth.signpost.exception.OAuthException;
import vn.evolus.droidreader.services.SynchronizationService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.google.reader.GoogleReader;

public class AuthorizationActivity extends LocalizedActivity {
	private static final String CALLBACK_URL = "droidreader://authorization";
	
	private GoogleReader reader = GoogleReaderFactory.getGoogleReader();	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.authorization);
		
		Button authorizeButton = (Button)findViewById(R.id.authorize);
		authorizeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				startAuthorizationProcess();
			}
		});
		
		Button registerButton = (Button)findViewById(R.id.register);
		registerButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				startRegistrationProcess();
			}
		});
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
				
		Uri uri = intent.getData();
		if (uri != null && uri.toString().startsWith(CALLBACK_URL)) {
		    String verificationCode = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);		    
		    try {		    	
				GoogleReader savedReader = getReader();
				if (savedReader != null) {
					savedReader.authorize(verificationCode);
				}
				String accessToken = savedReader.getAccessToken();
				String tokenSecret = savedReader.getTokenSecret();
				Settings.saveGoogleReaderAccessTokenAndSecret(this, accessToken, tokenSecret);				

				startSyncProcess();
			} catch (Exception e) {
				Toast.makeText(this, e.getMessage(), 100).show();
				e.printStackTrace();
			}
		}
	}
	
	private void saveReader(GoogleReader reader) {
		FileOutputStream fis = null;
		try {
			fis = openFileOutput("oauth_consumer", MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fis);
			oos.writeObject(reader);			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private GoogleReader getReader() {				
		FileInputStream is = null;
		try {
			is = openFileInput("oauth_consumer");
			ObjectInputStream ois = new ObjectInputStream(is);
			return (GoogleReader)ois.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private void startSyncProcess() {
		Intent service = new Intent(this, SynchronizationService.class);
    	startService(service);    	
    	Toast.makeText(this, R.string.synchorization_started, 100).show();
    	
    	Intent intent = new Intent(this, LatestItemsActivity.class);
		startActivity(intent);
		
    	finish();
	}

	private void startAuthorizationProcess() {
		try {			
			String authorizationUrl = reader.getAuthorizationUrl(CALLBACK_URL);
			saveReader(reader);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl)));
		} catch (OAuthException e) {
			e.printStackTrace();
		}
	}
	
	protected void startRegistrationProcess() {
		try {
			String authorizationUrl = reader.getAuthorizationUrl(CALLBACK_URL);
			saveReader(reader);
			startActivity(new Intent(Intent.ACTION_VIEW, 
					Uri.parse("https://www.google.com/accounts/NewAccount?continue=" + 
							URLEncoder.encode(authorizationUrl))));
		} catch (OAuthException e) {
			e.printStackTrace();
		}		
	}
}
