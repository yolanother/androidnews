package vn.evolus.android.news.html;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CompactXmlSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

public class Html {		
	private static HtmlCleaner cleaner;
	private static CompactXmlSerializer xmlSerializer;
	static {
		cleaner = new HtmlCleaner();
		CleanerProperties props = cleaner.getProperties();
		props.setOmitComments(true);		
		props.setUseCdataForScriptAndStyle(false);
		props.setOmitXmlDeclaration(true);
		xmlSerializer = new CompactXmlSerializer(props);
	}
	
	public static String toXhtml(String dirtyHtml) {
		try {
			TagNode node = cleaner.clean(dirtyHtml);			
			return xmlSerializer.getXmlAsString(node);			
		} catch (Exception e) {
			return null;
		}
	}
}
