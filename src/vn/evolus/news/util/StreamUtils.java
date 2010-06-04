package vn.evolus.news.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class StreamUtils {
	public static final int BUFFER_SIZE = 8192;
	public static String readAllText(InputStream inputStream) {
		return readAllText(inputStream, "UTF-8");
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
