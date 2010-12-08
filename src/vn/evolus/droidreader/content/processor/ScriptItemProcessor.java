package vn.evolus.droidreader.content.processor;

import java.util.regex.Pattern;

import vn.evolus.droidreader.content.ItemProcessor;
import vn.evolus.droidreader.model.Item;

public class ScriptItemProcessor implements ItemProcessor {
	private static final Pattern scriptPattern = Pattern.compile("(<script[^>]*?>[\\s\\S]*?</script>)|(<noscript>)|(</noscript>)", Pattern.CASE_INSENSITIVE);
	
	@Override
	public void process(Item item) {
		if (item.description == null) return;
		item.description = scriptPattern.matcher(item.description).replaceAll("");
	}
}
