package vn.evolus.droidreader.content.loader;

import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.model.Channel.Channels;
import android.database.Cursor;

public class FullChannelLoader implements ChannelLoader {
	private final String[] projection = new String[] {
			Channels.ID,
			Channels.TITLE,
			Channels.URL,
			Channels.DESCRIPTION,
			Channels.LINK,
			Channels.IMAGE_URL,
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
		channel.url = cursor.getString(2);//cursor.getColumnIndex(Channels.URL));
		channel.description = cursor.getString(3);//cursor.getColumnIndex(Channels.DESCRIPTION));
		channel.link = cursor.getString(4);//cursor.getColumnIndex(Channels.LINK));
		channel.imageUrl = cursor.getString(5);//cursor.getColumnIndex(Channels.IMAGE_URL));
		channel.options = cursor.getLong(6);//cursor.getColumnIndex(Channels.IMAGE_URL));
		return channel;
	}

}
