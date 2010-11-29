package vn.evolus.droidreader.content.loader;

import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.model.Channel.Channels;
import android.database.Cursor;

public class LightweightChannelLoader implements ChannelLoader {
	private final String[] projection = new String[] {
			Channels.ID,
			Channels.TITLE,
			Channels.URL,
			Channels.OPTIONS
		};
	@Override
	public String[] getProjection() {
		return projection;
	}

	@Override
	public Channel load(Cursor cursor) {
		Channel channel = new Channel();
		channel.id = cursor.getInt(0);//cursor.getColumnIndex(Channels.ID));
		channel.title = cursor.getString(1);//cursor.getColumnIndex(Channels.TITLE));
		channel.url = cursor.getString(2);
		channel.options = cursor.getLong(3);
		return channel;
	}

}
