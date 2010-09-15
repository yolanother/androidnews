package vn.evolus.droidreader.content;

import vn.evolus.droidreader.model.Item;
import android.database.Cursor;

public interface ItemLoader {
	String[] getProjection();
	Item load(Cursor cursor);
}
