package vn.evolus.droidreader.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Html {
	private static final Pattern bodyPattern = Pattern.compile("<body[^>]*?>([\\s\\S]*?)</body>", Pattern.CASE_INSENSITIVE);
	
	public static String getBodyContent(String dirtyHtml) {
		Matcher matcher = bodyPattern.matcher(dirtyHtml);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}
	
	public static String toText(String html) {
		if (html == null) return html;		
		return html.replaceAll("</?[^>]+>", "").trim();
	}
	
	public static String decode(String html) {
		if (html == null) return html;
        return html
        	.replace("&lt;", "<")
        	.replace("&gt;", ">")
        	.replace("&quot;", "\"")
        	.replace("&apos;", "'")
        	.replace("&nbsp;", " ")
        	.replace("&amp;", "&")
        	.replace("&mdash;", "—")
        	.replace("&#39;", "'");
	}
}
