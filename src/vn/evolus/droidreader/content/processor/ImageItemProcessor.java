package vn.evolus.droidreader.content.processor;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.content.ItemProcessor;
import vn.evolus.droidreader.model.Item;

public class ImageItemProcessor implements ItemProcessor {
	private static final Pattern imagePattern = Pattern.compile("<img[^>]*src=[\"']([^\"']*)[^>]*>", Pattern.CASE_INSENSITIVE);
	private static Pattern blackListImagePattern;
	private static final String IMAGE_RESIZER_SERVICE_URL = "http://droidreader.appspot.com/resize/";
	
	public ImageItemProcessor(String[] blackList) {
		StringBuilder sb = new StringBuilder();		
		for (String item : blackList) {
			sb.append("|(");
			sb.append(Pattern.quote(item));
			sb.append(")");
		}
		blackListImagePattern = Pattern.compile(sb.substring(1));
	}
	
	@Override
	public void process(Item item) {
		String description = item.description;
		if (description == null) return;
		
		Matcher matcher = imagePattern.matcher(description);
		boolean found = false;
		HashMap<String, Long> images = new HashMap<String, Long>();
		while (matcher.find()) {
			String imageUrl = matcher.group(1);
			String foundImageTag = matcher.group();
			if (!blackListImagePattern.matcher(imageUrl).find()) {				
				String cachedImageUrl = IMAGE_RESIZER_SERVICE_URL + "?width=300&height=0&op=resize&url=" + URLEncoder.encode(imageUrl);
				if (!images.containsKey(cachedImageUrl)) {					
					long imageId = ContentManager.queueImage(cachedImageUrl, item.updateTime);
					images.put(cachedImageUrl, imageId);
				}
				cachedImageUrl += "#" + images.get(cachedImageUrl);
				
				description = description.replace(foundImageTag,
						"<img src=\"" + cachedImageUrl + "\" />");
				if (!found) {
					item.imageUrl = IMAGE_RESIZER_SERVICE_URL.concat("?width=60&height=60&op=crop&url=")
							.concat(URLEncoder.encode(imageUrl));
					long imageId = ContentManager.queueImage(item.imageUrl, item.updateTime);
					item.imageUrl = item.imageUrl + "#" + imageId;
					found = true;
				}
			} else {
				description = description.replace(foundImageTag, "");
			}
		}
		item.description = description;
	}
}
