package vn.evolus.droidreader.widget;

import java.util.ArrayList;

import vn.evolus.droidreader.R;
import vn.evolus.droidreader.adapter.ChannelAdapter;
import vn.evolus.droidreader.model.Channel;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ListView;

public class ChannelListView extends ListView {
	private static final int REFRESH_MESSAGE = 1;
	private Handler handler;
	private ChannelAdapter adapter = null;
	
	public ChannelListView(Context context) {
		this(context, null, 0);
	}
	
	public ChannelListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);			
	}
	
	public ChannelListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);		
		init();
	}
	
	private void init() {
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == REFRESH_MESSAGE && adapter != null) {
					adapter.notifyDataSetChanged();
				}
				super.handleMessage(msg);
			}
		};
		this.setBackgroundColor(getResources().getColor(R.color.itemBackground));
		this.setCacheColorHint(getResources().getColor(R.color.itemBackground));
		this.setDivider(getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
	}
	
	public void setChannels(ArrayList<Channel> channels) {		   
		adapter = new ChannelAdapter(getContext(), channels);
        this.setAdapter(adapter);
	}	
	
	public void refresh() {		
		if (adapter != null) {			
			handler.sendMessage(handler.obtainMessage(REFRESH_MESSAGE));
		}
	}
}
