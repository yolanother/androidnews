package vn.evolus.droidnews.content;

import vn.evolus.droidnews.model.Item;
import android.database.Cursor;

public interface ItemLoader {
	String[] getProjection();
	Item load(Cursor cursor);
}
