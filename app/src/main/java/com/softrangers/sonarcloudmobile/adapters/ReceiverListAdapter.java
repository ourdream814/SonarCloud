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
    private OnItemClickListener mOnItemClickListener;
    private LayoutInflater mLayoutInflater;

    public ReceiverListAdapter(@NonNull Context context, @NonNull ArrayList<PASystem> paSystems) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mPASystems = paSystems;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public ArrayList<PASystem> getList() {
        return mPASystems;
    }

    public void addItem(PASystem system) {
        mPASystems.add(system);
        notifyDataSetChanged();
    }

    public void addItems(ArrayList<PASystem> systems) {
        for (PASystem system : systems) {
            mPASystems.add(system);
        }
        notifyDataSetChanged();
    }

    public void clearList() {
        mPASystems.clear();
        notifyDataSetChanged();
    }

    public void refreshList(ArrayList<PASystem> systems) {
        mPASystems.clear();
        for (PASystem system : systems) {
            mPASystems.add(system);
        }
        notifyDataSetChanged();
    }

    @Override
    public View getRealChildView(int groupPosition, final int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
        ChildViewHolder holder;
        View row = convertView;
        final Receiver receiver = mPASystems.get(groupPosition).getReceivers().get(childPosition);
        if (row == null) {
            row = mLayoutInflater.inflate(R.layout.receivers_list_item, parent, false);
            holder = new ChildViewHolder();

            holder.mChildTitle = (TextView) row.findViewById(R.id.child_title);
            holder.mChildTitle.setTypeface(SonarCloudApp.avenirBook);
            holder.mCheckBox = (ImageView) row.findViewById(R.id.check_box_image);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onChildClick(receiver, childPosition);
                    }
                }
            });
            row.setTag(holder);
        } else {
            holder = (ChildViewHolder) row.getTag();
        }
        holder.mCheckBox.setImageResource(receiver.isSelected() ? R.mipmap.ic_circle_checked : R.mipmap.ic_circle);
        holder.mChildTitle.setText(receiver.getName());
        return row;
    }

    @Override
    public int getRealChildrenCount(int groupPosition) {
        ArrayList<Receiver> receivers = mPASystems.get(groupPosition).getReceivers();
        if (receivers == null) receivers = new ArrayList<>();
        return receivers.size();
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
        ArrayList<Receiver> receivers = mPASystems.get(groupPosition).getReceivers();
        if (receivers == null) receivers = new ArrayList<>();
        return receivers.get(childPosition);
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
        return true;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        View row = convertView;
        if (row == null) {
            row = mLayoutInflater.inflate(R.layout.receivers_list_header, parent, false);
            holder = new HeaderViewHolder();
            holder.mHeaderTitle = (TextView) row.findViewById(R.id.receiver_list_header_title);
            holder.mHeaderTitle.setTypeface(SonarCloudApp.avenirMedium);
            holder.mIndicator = (ImageView) row.findViewById(R.id.expanded_indicator_image);
            row.setTag(holder);
        } else {
            holder = (HeaderViewHolder) row.getTag();
        }

        final PASystem system = mPASystems.get(groupPosition);

        holder.mHeaderTitle.setText(system.getName());
        holder.mIndicator.setImageResource(isExpanded ? R.mipmap.ic_indicator_up : R.mipmap.ic_indicator_down);

        return row;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public interface OnItemClickListener {
        void onChildClick(Receiver receiver, int position);
    }

    private class ChildViewHolder {
        public ImageView mCheckBox;
        public TextView mChildTitle;
    }

    private class HeaderViewHolder {
        public ImageView mIndicator;
        public TextView mHeaderTitle;
    }
}
