package vn.evolus.droidreader.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class StreamUtils {
	private static final String DEFAULT_ENCODING = "UTF-8";
	public static final int BUFFER_SIZE = 8192;
	
	/**
	 * Read content of URL to string, using default UTF-8 encoding
	 * @param url
	 * @return
	 */
	public static String readFromUrl(String url) {
		String encoding = DEFAULT_ENCODING;
		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		try {
			HttpResponse response = client.execute(get);
			InputStream is = response.getEntity().getContent();
			encoding = guessEncoding(is);			
			is.close();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// should use auto-detect encoding code here
		return readFromUrl(url, encoding);
	}
	
	public static String guessEncoding(InputStream is) {		
		byte[] buf = new byte[BUFFER_SIZE];
		try {
			is.read(buf);
			return new CharsetToolkit(buf, Charset.forName("ISO-8859-1")).guessEncoding().name();
		} catch (IOException e) {
			e.printStackTrace();
		}						
		return DEFAULT_ENCODING;
	}
	
	/**
	 * Reading content of URL to a string using provided encoding
	 * @param url
	 * @param encoding
	 * @return
	 */
	public static String readFromUrl(String url, String encoding) {
		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		try {
			HttpResponse response = client.execute(get);
			InputStream is = response.getEntity().getContent();
			String result = readAllText(is, encoding);
			is.close();
			return result;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String readAllText(InputStream inputStream) {
		return readAllText(inputStream, DEFAULT_ENCODING);
	}
	
	public static String[] readLines(InputStream inputStream, String encoding) {
		List<String> lines = new ArrayList<String>();
		try {		
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, encoding), BUFFER_SIZE);
			String line = null;
			while ((line = reader.readLine()) != null) {				
				lines.add(line);
			}
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		return lines.toArray(new String[0]);
	}
	
	public static String readAllText(InputStream inputStream, String encoding) {
		StringBuffer sb = new StringBuffer();
		try {		
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, encoding), BUFFER_SIZE);
			String line = null;
			while ((line = reader.readLine()) != null) {				
				sb.append(line).append("\n");
			}
			inputStream.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}		
		return sb.toString();
	}
	
	public static void writeStream(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		int readBytes = 0;
		while ((readBytes = is.read(buffer)) > 0) {
			os.write(buffer, 0, readBytes);			
		}
	}
}
