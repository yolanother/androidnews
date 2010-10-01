package vn.evolus.droidreader.content.loader;

import java.util.Date;

import vn.evolus.droidreader.content.ItemLoader;
import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.model.Item;
import vn.evolus.droidreader.model.Item.Items;
import android.database.Cursor;

public class FullItemLoader implements ItemLoader {
	public String[] getProjection() {
		return new String[] {
				Items.ID,
				Items.TITLE,
				Items.DESCRIPTION,
				Items.PUB_DATE,
				Items.LINK,
				Items.IMAGE_URL,
				Items.READ,
				Items.STARRRED,
				Items.KEPT,
				Items.CHANNEL_ID,
				Items.UPDATE_TIME,
				Items.ORIGINAL_ID
			};
	}
	public Item load(Cursor cursor) {
		// using magic numbers !!!
		Item item = new Item();
		item.id = cursor.getInt(0);		
		item.title = cursor.getString(1);
		item.description = cursor.getString(2);		
		item.pubDate = new Date(cursor.getLong(3));
		item.link = cursor.getString(4);
		item.imageUrl = cursor.getString(5);			
		item.read = (cursor.getInt(6) != 0);
		item.starred = (cursor.getInt(7) != 0);
		item.kept = (cursor.getInt(8) != 0);
		item.channel = new Channel(cursor.getInt(9));
		item.updateTime = cursor.getLong(10);
		item.originalId = cursor.getString(11);
		return item;
	}
}
