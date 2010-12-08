package vn.evolus.droidreader.adapter;

import java.util.List;

import vn.evolus.droidreader.R;
import vn.evolus.droidreader.model.Subscription;
import vn.evolus.droidreader.model.SubscriptionGroup;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

public class SuggestedChannelsAdapter extends BaseExpandableListAdapter {
	private Context context;
	private List<SubscriptionGroup> groups;
	
	public SuggestedChannelsAdapter(Context context, List<SubscriptionGroup> groups) {
		this.context = context;
		this.groups = groups;
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return ((SubscriptionGroup)getGroup(groupPosition)).getSubscriptions().get(childPosition);		
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {		
		Subscription subscription = (Subscription)getChild(groupPosition, childPosition);
		CheckedTextView title;
		if (convertView == null) {
			convertView = View.inflate(context, R.layout.channel_subscription, null);
			title = (CheckedTextView)convertView.findViewById(R.id.title);
			convertView.setTag(title);
		} else {
			title = (CheckedTextView)convertView.getTag();
		}
        title.setText(subscription.title);
        title.setChecked(subscription.subscribed);
        
        return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return ((SubscriptionGroup)getGroup(groupPosition)).getSubscriptions().size();
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
		SubscriptionGroup group = (SubscriptionGroup)getGroup(groupPosition);
		TextView title;
		if (convertView == null) {
			convertView = View.inflate(context, R.layout.channel_group, null);
			title = (TextView)convertView.findViewById(R.id.title);
			convertView.setTag(title);
		} else {
			title = (TextView)convertView.getTag();
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
