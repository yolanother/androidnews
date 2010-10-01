package vn.evolus.droidreader.content.loader;

import vn.evolus.droidreader.content.ItemLoader;
import vn.evolus.droidreader.model.Item;
import vn.evolus.droidreader.model.Item.Items;
import android.database.Cursor;

public class IdOnlyItemLoader implements ItemLoader {
	private final String[] projection = new String[] {
			Items.ID
		};
	public String[] getProjection() {
		return projection;
	}
	public Item load(Cursor cursor) {
		Item item = new Item();
		item.id = cursor.getInt(0);		
		return item;
	}	
}
