package vn.evolus.droidreader.util;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

public class UrlUtils {
	public static String getRedirectedUrl(String url) {
		DefaultHttpClient client = new DefaultHttpClient();		
		HttpGet get = new HttpGet(url);
		try {
			HttpContext context = new BasicHttpContext(); 
	        HttpResponse response = client.execute(get, context); 
	        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
	            throw new IOException(response.getStatusLine().toString());
	        HttpUriRequest currentReq = (HttpUriRequest)context.getAttribute( 
	                ExecutionContext.HTTP_REQUEST);
	        HttpHost currentHost = (HttpHost)context.getAttribute( 
	                ExecutionContext.HTTP_TARGET_HOST);	        
	        return currentHost.toURI() + currentReq.getURI();			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return url;
	}
}
