package vn.evolus.droidreader.content.processor;

import java.util.regex.Pattern;

import vn.evolus.droidreader.content.ItemProcessor;
import vn.evolus.droidreader.model.Item;

public class IframeItemProcessor implements ItemProcessor {
	private static final Pattern iframePattern = Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE);
	
	@Override
	public void process(Item item) {
		if (item.description == null) return;
		item.description = iframePattern.matcher(item.description).replaceAll("");
	}
}
