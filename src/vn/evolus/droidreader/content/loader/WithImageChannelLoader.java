package vn.evolus.droidreader.content.loader;

import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.model.Channel.Channels;
import android.database.Cursor;

public class WithImageChannelLoader implements ChannelLoader {
	private final String[] projection = new String[] {
			Channels.ID,
			Channels.TITLE,
			Channels.URL,			
			Channels.LATEST_ITEM_IMAGE_URL
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
		channel.imageUrl = cursor.getString(3);
		return channel;
	}

}
