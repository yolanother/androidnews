package vn.evolus.droidreader.content.loader;

import java.util.Date;

import vn.evolus.droidreader.content.ItemLoader;
import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.model.Item;
import vn.evolus.droidreader.model.Item.Items;
import android.database.Cursor;

public class LightweightItemLoader implements ItemLoader {
	private final String[] projection = new String[] {
			Items.ID,
			Items.TITLE,
			Items.PUB_DATE,					
			Items.IMAGE_URL,
			Items.READ,
			Items.STARRRED,
			Items.CHANNEL_ID,
			Items.UPDATE_TIME
		};
	public String[] getProjection() {
		return projection;
	}
	public Item load(Cursor cursor) {
		// using magic numbers !!!
		Item item = new Item();
		item.id = cursor.getInt(0);//cursor.getColumnIndex(Items.ID));
		item.title = cursor.getString(1);//cursor.getColumnIndex(Items.TITLE));
		item.pubDate = new Date(cursor.getLong(2));//cursor.getColumnIndex(Items.PUB_DATE));
		item.imageUrl = cursor.getString(3);//cursor.getColumnIndex(Items.IMAGE_URL));
		item.read = (cursor.getInt(4) != 0);//cursor.getColumnIndex(Items.READ));
		item.starred = (cursor.getInt(5) != 0);//cursor.getColumnIndex(Items.READ));
		item.channel = new Channel(cursor.getInt(6));
		item.updateTime = cursor.getLong(7);
		return item;
	}	
}
