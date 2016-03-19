package com.softrangers.sonarcloudmobile.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.Group;

import java.util.ArrayList;

/**
 * Created by eduard on 3/19/16.
 */
public class GroupsListAdapter extends RecyclerView.Adapter<GroupsListAdapter.ViewHolder> {

    private ArrayList<Group> mGroups;
    private OnGroupClickListener mOnGroupClickListener;

    public GroupsListAdapter(ArrayList<Group> groups) {
        mGroups = groups;
    }

    public void setOnGroupClickListener(OnGroupClickListener onGroupClickListener) {
        mOnGroupClickListener = onGroupClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_list_item,
                parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mGroup = mGroups.get(position);
        holder.mCheckCircle.setImageResource(
                holder.mGroup.isSelected() ? R.mipmap.ic_circle_checked : R.mipmap.ic_circle
        );
        holder.mGroupTitle.setText(
                "[" + holder.mGroup.getLoginID() + "] " + holder.mGroup.getName()
        );
        holder.mGroupDescription.setText(
                holder.mGroup.getReceivers().size() + " receivers"
        );
    }

    @Override
    public int getItemCount() {
        return mGroups.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView mCheckCircle;
        final TextView mGroupTitle;
        final TextView mGroupDescription;
        final ImageButton mEditButton;
        Group mGroup;
        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mCheckCircle = (ImageView) itemView.findViewById(R.id.check_unCheck_groupItem);
            mGroupTitle = (TextView) itemView.findViewById(R.id.group_item_titleText);
            mGroupDescription = (TextView) itemView.findViewById(R.id.group_item_receiversCount);
            mEditButton = (ImageButton) itemView.findViewById(R.id.group_edit_button);
            mEditButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.group_edit_button:
                    if (mOnGroupClickListener != null) {
                        mOnGroupClickListener.onEditButtonClicked(mGroup, getAdapterPosition());
                    }
                    break;
                default:
                    if (mOnGroupClickListener != null) {
                        mOnGroupClickListener.onGroupClicked(mGroup, getAdapterPosition());
                    }
                    break;
            }
        }
    }

    public interface OnGroupClickListener {
        void onGroupClicked(Group group, int position);
        void onEditButtonClicked(Group group, int position);
    }
}
