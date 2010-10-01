package vn.evolus.droidreader.content.loader;

import vn.evolus.droidreader.model.Channel;
import android.database.Cursor;

public interface ChannelLoader {
	String[] getProjection();
	Channel load(Cursor cursor);
}
