package vn.evolus.droidreader.content;

import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.model.Channel.Channels;
import android.database.Cursor;

public class LightweightChannelLoader implements ChannelLoader {
	private final String[] projection = new String[] {
			Channels.ID,
			Channels.TITLE,
			Channels.URL
		};
	@Override
	public String[] getProjection() {
		return projection;
	}

	@Override
	public Channel load(Cursor cursor) {
		Channel channel = new Channel();
		channel.id = cursor.getLong(0);//cursor.getColumnIndex(Channels.ID));
		channel.title = cursor.getString(1);//cursor.getColumnIndex(Channels.TITLE));
		channel.url = cursor.getString(2);//cursor.getColumnIndex(Channels.URL));
		return channel;
	}

}
