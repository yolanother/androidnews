package com.google.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import vn.evolus.droidreader.util.StreamUtils;

import com.google.reader.atom.AtomFeed;
import com.google.reader.atom.AtomHandler.OnNewEntryCallback;

public class GoogleReader implements Serializable {   
	private static final long serialVersionUID = 4174455638684699104L;
	
	private static final String scope =
        "http://www.google.com/reader/api%20http://www.google.com/reader/atom";
    private static final String reqtokenURL =
        "https://www.google.com/accounts/OAuthGetRequestToken";
    private static final String authorizeURL =
        "https://www.google.com/accounts/OAuthAuthorizeToken";
    private static final String accessTokenURL =
        "https://www.google.com/accounts/OAuthGetAccessToken";
    
    private static final String GOOGLE_READER_URL = "http://www.google.com/reader/";    
    private static final String GOOGLE_READER_API_URL = GOOGLE_READER_URL + "api/0/";
    
    private static final String ATOM_FEED_URL = 
    	GOOGLE_READER_URL + "atom/";
    
    private static final String GET_SUBSCRIPTION_LIST_URL = 
    	GOOGLE_READER_API_URL + "subscription/list?output=json";
    private static final String GET_TAG_LIST_URL = 
    	GOOGLE_READER_API_URL + "tag/list?output=json";
    private static final String GET_API_TOKEN_URL = 
    	GOOGLE_READER_API_URL + "token";
    private static final String SUBSCRIBTION_EDIT_URL = 
    	GOOGLE_READER_API_URL + "subscription/edit";
    private static final String GET_UNREAD_COUNT_URL = 
    	GOOGLE_READER_API_URL + "unread-count?all=true&output=json";
    private static final String EDIT_TAG_URL = 
    	GOOGLE_READER_API_URL + "edit-tag";
    
    private static final String ITEM_STATE = "user/-/state/com.google";
    public static final String ITEM_LABEL = "user/-/label";
    
    public static final String STARRED = "starred";
    public static final String SHARED = "broadcast";
    
    public static final String ITEM_STATE_KEPT_UNREAD = ITEM_STATE + "/kept-unread";
    public static final String ITEM_STATE_READ = ITEM_STATE + "/read";
    public static final String ITEM_STATE_STARRED = ITEM_STATE + "/" + STARRED;
    public static final String ITEM_STATE_SHARED = ITEM_STATE + "/" + SHARED;
    private static final String ITEM_STATE_READING = ITEM_STATE + "/reading-list";       
    
    private String accessToken = null;
    private String tokenSecret = null;
    private String apiToken = null;
    
    private CommonsHttpOAuthConsumer consumer;
    private CommonsHttpOAuthProvider provider;
    
    public GoogleReader(String consumerKey, String consumerSecret) {
    	this(consumerKey, consumerSecret, null);
    }
    	
    public GoogleReader(String consumerKey, String consumerSecret, String applicationName) {
    	consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
 
        provider = new CommonsHttpOAuthProvider(
            reqtokenURL + "?scope=" + scope + 
            	(applicationName != null ? "&xoauth_displayname=" + URLEncoder.encode(applicationName) : ""),
            accessTokenURL, 
            authorizeURL + "?hl=en");
    }
    
	public String getAuthorizationUrl(String callbackURL) 
		throws OAuthException {        
		return provider.retrieveRequestToken(consumer, callbackURL);		
	}
		
	public void authorize(String verificationCode) 
		throws OAuthException {
		// TRICK: walk around for serialization bug of CommonsHttpOAuthProvider
		provider.setHttpClient(new DefaultHttpClient());
		provider.retrieveAccessToken(consumer, verificationCode);
		authorize(consumer.getToken(), consumer.getTokenSecret());
	}
	
	public void authorize(String accessToken, String tokenSecret) 
		throws OAuthException {
		
		this.accessToken = accessToken;
		this.tokenSecret = tokenSecret;
		consumer.setTokenWithSecret(this.accessToken, this.tokenSecret);
	}
	
	public String getAccessToken() {
		return accessToken;
	}
	public String getTokenSecret() {
		return tokenSecret;
	}
	
	public List<Subscription> getSubscriptions() throws Exception {
		InputStream is = get(GET_SUBSCRIPTION_LIST_URL).getEntity().getContent();
		
        String result = StreamUtils.readAllText(is);
        JSONObject json = new JSONObject(result);
        JSONArray subscriptionArray = json.getJSONArray("subscriptions");
        
        List<Subscription> subscriptions = new ArrayList<Subscription>(subscriptionArray.length());
        for (int i = 0; i < subscriptionArray.length(); i++) {
        	subscriptions.add(Subscription.fromJSON(subscriptionArray.getJSONObject(i)));
        }
        return subscriptions;
    }
	
	public List<String> getTags() 
		throws OAuthException, Exception {
		InputStream is = get(GET_TAG_LIST_URL).getEntity().getContent();
		
        String result = StreamUtils.readAllText(is);
        
        JSONObject json = new JSONObject(result);
        JSONArray tagsArray = json.getJSONArray("tags");
        
        List<String> tags = new ArrayList<String>();
        for (int i = 0; i < tagsArray.length(); i++) {
        	JSONObject tagObject = tagsArray.getJSONObject(i);
        	tags.add(tagObject.getString("id"));
        }
        return tags;
	}
		
	public String getUnreadCount() 
		throws OAuthException, Exception {
		InputStream is = get(GET_UNREAD_COUNT_URL).getEntity().getContent();		
	    return StreamUtils.readAllText(is);
	}
	
	public AtomFeed fetchFeed(String feedId, int maxItems) throws Exception {
		return fetchFeed(feedId, maxItems, null);
	}

	public AtomFeed fetchFeed(String feedId, int maxItems,
			OnNewEntryCallback callback) throws Exception {
		if (!feedId.startsWith("feed/")) {
			feedId = "feed/" + feedId;
		}		
        InputStream is = get(ATOM_FEED_URL + URLEncoder.encode(feedId))
        	.getEntity().getContent();
        return AtomFeed.parse(is, maxItems, callback);
	}
	
	public AtomFeed getReadingList(int maxItems, String continution, 
			OnNewEntryCallback callback) throws Exception {		
        InputStream is = get(ATOM_FEED_URL + "user/-" + ITEM_STATE_READING + "?n=" + maxItems
        		+ (continution != null ? "&c=" + continution : ""))
        	.getEntity().getContent();
        return AtomFeed.parse(is, maxItems, callback);
	}
	
	public void markItemAsRead(String itemId) 
		throws OAuthException, Exception {
		
		addTagToItem(itemId, ITEM_STATE_READ);
	}
	
	public void markItemAsUnread(String itemId) 
		throws OAuthException, Exception {
		
		addTagToItem(itemId, ITEM_STATE_KEPT_UNREAD);
	}
	
	public void markItemAsStarred(String itemId) 
		throws OAuthException, Exception {
		
		addTagToItem(itemId, ITEM_STATE_STARRED);
	}
	
	public void addTagToItem(String itemId, String tag) 
		throws OAuthException, Exception {				
		ensureApiTokenAvailable();
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("i", itemId));		
		params.add(new BasicNameValuePair("a", tag));
		params.add(new BasicNameValuePair("ac", "edit"));
		params.add(new BasicNameValuePair("T", apiToken));
	
		post(EDIT_TAG_URL, params);
	}
	
	public void removeTagFromItem(String itemId, String tag) 
		throws OAuthException, Exception {				
		ensureApiTokenAvailable();
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("i", itemId));		
		params.add(new BasicNameValuePair("r", tag));
		params.add(new BasicNameValuePair("ac", "edit"));
		params.add(new BasicNameValuePair("T", apiToken));
	
		post(EDIT_TAG_URL, params);
	}
	
	public void subscribe(String feedUrl)
		throws GoogleReaderException, Exception {
		ensureApiTokenAvailable();
		
		if (!feedUrl.startsWith("feed/")) {
			feedUrl = "feed/" + feedUrl;
		}
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("s", feedUrl));
		params.add(new BasicNameValuePair("ac", "subscribe"));		
		params.add(new BasicNameValuePair("T", apiToken));
        
        post(SUBSCRIBTION_EDIT_URL, params);
	}
	
	public void unsubscribe(String feedUrl) 
		throws GoogleReaderException, Exception {
		ensureApiTokenAvailable();
		
		if (!feedUrl.startsWith("feed/")) {
			feedUrl = "feed/" + feedUrl;
		}
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("s", feedUrl));
		params.add(new BasicNameValuePair("ac", "unsubscribe"));
		params.add(new BasicNameValuePair("T", apiToken));
	    
	    post(SUBSCRIBTION_EDIT_URL, params);
	}

	private void ensureApiTokenAvailable()
		throws Exception {
		if (apiToken == null) {			
	        InputStream is = get(GET_API_TOKEN_URL).getEntity().getContent();
	        apiToken = StreamUtils.readAllText(is);
		}
	}
	
	private HttpResponse post(String url, List<NameValuePair> params) 
		throws GoogleReaderException, OAuthException, 
			ClientProtocolException, IOException {
		HttpClient client = new DefaultHttpClient();			
		HttpPost httpPost = new HttpPost(url);
		httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));		
        consumer.sign(httpPost);
        
        HttpResponse response = (HttpResponse)client.execute(httpPost);        
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() != 200) {
            throw new GoogleReaderException(statusLine.getStatusCode(), statusLine.getReasonPhrase());            
        }
        
        return response;
	}
	
	private HttpResponse get(String uri) 
		throws GoogleReaderException, OAuthException,
		IOException, ClientProtocolException, Exception {
		HttpClient client = new DefaultHttpClient();
		
		HttpGet httpGet = new HttpGet(uri);
		consumer.sign(httpGet);
		
		HttpResponse response = (HttpResponse)client.execute(httpGet);        
		StatusLine statusLine = response.getStatusLine();
		if (statusLine.getStatusCode() != 200) {
		    throw new GoogleReaderException(statusLine.getStatusCode(), statusLine.getReasonPhrase() + "(" + uri + ")");            
		}		
		return response;
	}
}
