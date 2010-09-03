package vn.evolus.droidnews.content;

import java.util.Date;

import vn.evolus.droidnews.model.Channel;
import vn.evolus.droidnews.model.Item;
import vn.evolus.droidnews.model.Item.Items;
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
				Items.CHANNEL_ID
			};
	}
	public Item load(Cursor cursor) {
		// using magic numbers !!!
		Item item = new Item();
		item.id = cursor.getLong(0);		
		item.title = cursor.getString(1);//cursor.getColumnIndex(Items.TITLE));
		item.description = cursor.getString(2);//cursor.getColumnIndex(Items.DESCRIPTION));		
		item.pubDate = new Date(cursor.getLong(3));//cursor.getColumnIndex(Items.DESCRIPTION));
		item.link = cursor.getString(4);//cursor.getColumnIndex(Items.PUB_DATE));
		item.imageUrl = cursor.getString(5);//cursor.getColumnIndex(Items.IMAGE_URL));			
		item.read = (cursor.getInt(6) != 0);//cursor.getColumnIndex(Items.READ));
		item.channel = new Channel(cursor.getInt(7));
		return item;
	}
}
