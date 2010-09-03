package vn.evolus.droidnews.content.processors;

import java.util.regex.Pattern;

import vn.evolus.droidnews.content.ItemProcessor;
import vn.evolus.droidnews.model.Item;

public class IframeItemProcessor implements ItemProcessor {
	private static final Pattern iframePattern = Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE);
	
	@Override
	public void process(Item item) {
		item.description = iframePattern.matcher(item.description).replaceAll("");
	}
}
