package vn.evolus.android.news.html;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

public class Html {		
	private static HtmlCleaner cleaner = new HtmlCleaner();
	//private static CompactXmlSerializer serializer = new CompactXmlSerializer(new CleanerProperties());
	
	public static String toXhtml(String dirtyHtml) {
		try {
			TagNode node = cleaner.clean(dirtyHtml);			
			return cleaner.getInnerHtml(node);
			//return serializer.getXmlAsString(node);			
		} catch (Exception e) {
			//Log.e("ERROR", e.getMessage());
			return null;
		}
	}
}
