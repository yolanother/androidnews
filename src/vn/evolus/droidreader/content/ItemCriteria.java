package vn.evolus.droidreader.content;

import android.net.Uri;

public interface ItemCriteria {
	Uri getContentUri();
	String getSelection();
	String[] getSelectionArgs();
	String getOrderBy();
}
