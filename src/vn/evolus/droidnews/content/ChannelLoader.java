package vn.evolus.droidnews.content;

import vn.evolus.droidnews.model.Channel;
import android.database.Cursor;

public interface ChannelLoader {
	String[] getProjection();
	Channel load(Cursor cursor);
}
