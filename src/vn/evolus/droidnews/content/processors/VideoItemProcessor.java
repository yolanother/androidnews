package vn.evolus.droidnews.content.processors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vn.evolus.droidnews.content.ItemProcessor;
import vn.evolus.droidnews.model.Item;
import android.util.Log;

public class VideoItemProcessor implements ItemProcessor {
	private static final Pattern objectPattern = Pattern.compile("(<object[^>]*?>[^<]*?)?<embed[^>]*?src=[\"']([^\"']*)[^>]*?>[^<]*?</embed>([^<]*?</object>)?", Pattern.CASE_INSENSITIVE);
	
	public void process(Item item) {
		String description = item.description;
		Matcher matcher = objectPattern.matcher(item.description);
		while (matcher.find()) {
			Log.d("DEBUG", "Found object/embed tag: " + matcher.group());
			String videoUrl = matcher.group(2);
			String newVideoTag = "<div><a href=\"" + videoUrl + "\"><img src=\"file:///android_asset/video.png\" /></a></div>";			
			description = description.replace(matcher.group(), newVideoTag);
		}			
		item.description = description;
	}	
}
