package vn.evolus.droidnews.content;

import java.util.Date;

import vn.evolus.droidnews.model.Channel;
import vn.evolus.droidnews.model.Item;
import vn.evolus.droidnews.model.Item.Items;
import android.database.Cursor;

public class LightweightItemLoader implements ItemLoader {
	private final String[] projection = new String[] {
			Items.ID,
			Items.TITLE,
			Items.PUB_DATE,					
			Items.IMAGE_URL,
			Items.READ,
			Items.CHANNEL_ID
		};
	public String[] getProjection() {
		return projection;
	}
	public Item load(Cursor cursor) {
		// using magic numbers !!!
		Item item = new Item();
		item.id = cursor.getLong(0);//cursor.getColumnIndex(Items.ID));
		item.title = cursor.getString(1);//cursor.getColumnIndex(Items.TITLE));
		item.pubDate = new Date(cursor.getLong(2));//cursor.getColumnIndex(Items.PUB_DATE));
		item.imageUrl = cursor.getString(3);//cursor.getColumnIndex(Items.IMAGE_URL));
		item.read = (cursor.getInt(4) != 0);//cursor.getColumnIndex(Items.READ));
		item.channel = new Channel(cursor.getInt(5));
		return item;
	}	
}
