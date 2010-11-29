package vn.evolus.droidreader.content.loader;

import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.model.Channel.Channels;
import android.database.Cursor;

public class WithImageChannelLoader implements ChannelLoader {
	private final String[] projection = new String[] {
			Channels.ID,
			Channels.TITLE,
			Channels.URL,			
			Channels.LATEST_ITEM_IMAGE_URL,
			Channels.OPTIONS
		};
	@Override
	public String[] getProjection() {
		return projection;
	}

	@Override
	public Channel load(Cursor cursor) {
		Channel channel = new Channel();
		channel.id = cursor.getInt(0);
		channel.title = cursor.getString(1);
		channel.url = cursor.getString(2);
		channel.imageUrl = cursor.getString(3);
		channel.options = cursor.getLong(4);
		return channel;
	}

}
