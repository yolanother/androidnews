package vn.evolus.droidnews.content.processors;

import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vn.evolus.droidnews.content.ContentManager;
import vn.evolus.droidnews.content.ItemProcessor;
import vn.evolus.droidnews.model.Item;

public class ImageItemProcessor implements ItemProcessor {
	private Pattern imagePattern = Pattern.compile("<img[^>]*src=[\"']([^\"']*)[^>]*>", Pattern.CASE_INSENSITIVE);
	private Pattern blackListImagePattern;
	private static final String IMAGE_RESIZER_SERVICE_URL = "http://droidfeed.appspot.com/resize/";
	
	public ImageItemProcessor() {
		String[] blackList = new String[] {
			"www.engadget.com/media/post_label",
			"feeds.wordpress.com",
			"stats.wordpress.com",
			"feedads",
			"feedburner",
			"api.tweetmeme.com",
			"creatives.commindo-media.de",
			"ads.pheedo.com",
			"images.pheedo.com/images/mm",
			"cdn.stumble-upon.com",
			"vietnamnet.gif",
			"digg-badge-custom-1.gif",
			"http://a.gigaom.com/feed-injector/img",
			"openx.com",
			"imp.constantcontact.com",
			"commindo-media-ressourcen.de"
		};
		
		//
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
		Matcher matcher = imagePattern.matcher(description);
		boolean found = false;
		Set<String> images = new HashSet<String>();
		while (matcher.find()) {
			String imageUrl = matcher.group(1);
			String foundImageTag = matcher.group();
			if (!blackListImagePattern.matcher(imageUrl).find()) {				
				String cachedImageUrl = IMAGE_RESIZER_SERVICE_URL + "?width=300&height=0&op=resize&url=" + URLEncoder.encode(imageUrl);
				if (!images.contains(cachedImageUrl)) {
					images.add(cachedImageUrl);
					long imageId = ContentManager.queueImage(cachedImageUrl);
					description = description.replace(foundImageTag,
							"<img src=\"" + cachedImageUrl + "#" + imageId + "\" />");
				}
				if (!found) {
					item.imageUrl = IMAGE_RESIZER_SERVICE_URL.concat("?width=60&height=60&op=crop&url=")
							.concat(URLEncoder.encode(imageUrl));
					images.add(item.imageUrl);
					ContentManager.queueImage(item.imageUrl);
					found = true;
				}
			} else {
				description = description.replace(foundImageTag, "");
			}
		}		
		item.description = description;
	}
}
