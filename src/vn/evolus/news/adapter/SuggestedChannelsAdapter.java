package vn.evolus.news.adapter;

import java.util.List;

import vn.evolus.news.R;
import vn.evolus.news.model.Channel;
import vn.evolus.news.model.ChannelGroup;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;

public class SuggestedChannelsAdapter extends BaseExpandableListAdapter {
	private Context context;
	private List<ChannelGroup> groups;
	
	public SuggestedChannelsAdapter(Context context, List<ChannelGroup> groups) {
		this.context = context;
		this.groups = groups;
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return ((ChannelGroup)getGroup(groupPosition)).getChannels().get(childPosition);		
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {		
		Channel channel = (Channel)getChild(groupPosition, childPosition);
		CheckedTextView title;
		if (convertView == null) {
			convertView = View.inflate(context, R.layout.channel_subscription, null);
			title = (CheckedTextView)convertView.findViewById(R.id.title);
			convertView.setTag(title);
		} else {
			title = (CheckedTextView)convertView.getTag();
		}				
        title.setText(channel.getTitle());
        
        return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return ((ChannelGroup)getGroup(groupPosition)).getChannels().size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return groups.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return groups.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
            ViewGroup parent) {		
		ChannelGroup group = (ChannelGroup)getGroup(groupPosition);
		CheckedTextView title;
		if (convertView == null) {
			convertView = View.inflate(context, R.layout.channel_group, null);
			title = (CheckedTextView)convertView.findViewById(R.id.title);
			convertView.setTag(title);
		} else {
			title = (CheckedTextView)convertView.getTag();
		}				
        title.setText(group.getTitle());
        return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}		
}
