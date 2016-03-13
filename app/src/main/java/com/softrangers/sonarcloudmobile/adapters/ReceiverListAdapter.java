package com.softrangers.sonarcloudmobile.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.PASystem;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.widgets.AnimatedExpandableListView;

import java.util.ArrayList;


public class ReceiverListAdapter extends AnimatedExpandableListView.AnimatedExpandableListAdapter {

    private Context mContext;
    private ArrayList<PASystem> mPASystems;

    public ReceiverListAdapter(@NonNull Context context, @NonNull ArrayList<PASystem> paSystems) {
        mContext = context;
        mPASystems = paSystems;
    }

    @Override
    public View getRealChildView(int groupPosition, int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent)
    {
        View row = convertView;
        ChildViewHolder holder;

        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.receivers_list_item, null);
            holder = new ChildViewHolder();

            holder.mChildeTitle = (TextView) row.findViewById(R.id.child_title);
            holder.mChildeTitle.setTypeface(SonarCloudApp.avenirBook);
            holder.mCheckBox = (ImageView) row.findViewById(R.id.check_box_image);

            row.setTag(holder);
        } else {
            holder = (ChildViewHolder) row.getTag();
        }

        Receiver receiver = mPASystems.get(groupPosition).getReceivers().get(childPosition);
        holder.mCheckBox.setImageResource(receiver.isSelected() ? R.mipmap.ic_circle_checked : R.mipmap.ic_circle);
        holder.mChildeTitle.setText(receiver.getName());
        return row;
    }

    @Override
    public int getRealChildrenCount(int groupPosition) {
        return mPASystems.get(groupPosition).getReceivers().size();
    }

    @Override
    public int getGroupCount() {
        return mPASystems.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mPASystems.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mPASystems.get(groupPosition).getReceivers().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent)
    {
        View row = convertView;
        HeaderViewHolder holder;

        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.receivers_list_header, null);
            holder = new HeaderViewHolder();

            holder.mHeaderTitle = (TextView) row.findViewById(R.id.receiver_list_header_title);
            holder.mHeaderTitle.setTypeface(SonarCloudApp.avenirMedium);
            holder.mIndicator = (ImageView) row.findViewById(R.id.expanded_indicator_image);

            row.setTag(holder);
        } else {
            holder = (HeaderViewHolder) row.getTag();
        }

        PASystem system = mPASystems.get(groupPosition);

        holder.mHeaderTitle.setText(system.getName());
        holder.mIndicator.setImageResource(isExpanded ? R.mipmap.ic_indicator_up : R.mipmap.ic_indicator_down);
        return row;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private class ChildViewHolder {
        public ImageView mCheckBox;
        public TextView mChildeTitle;
    }

    private class HeaderViewHolder {
        public ImageView mIndicator;
        public TextView mHeaderTitle;
    }
}
