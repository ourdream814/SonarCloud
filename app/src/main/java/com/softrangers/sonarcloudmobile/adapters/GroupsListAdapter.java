package com.softrangers.sonarcloudmobile.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
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

    private static final int NOT_SELECTED = -1;
    private static final int WAS_NOT_SELECTED = -2;

    private ArrayList<Group> mGroups;
    private OnGroupClickListener mOnGroupClickListener;
    private static int lastSelectedPosition;
    private static int currentSelectedPosition;

    public GroupsListAdapter(ArrayList<Group> groups) {
        mGroups = groups;
        lastSelectedPosition = WAS_NOT_SELECTED;
        currentSelectedPosition = NOT_SELECTED;
    }

    public void setOnGroupClickListener(OnGroupClickListener onGroupClickListener) {
        mOnGroupClickListener = onGroupClickListener;
    }

    public Group deleteItem(int position) {
        Group group = mGroups.get(position);
        mGroups.remove(position);
        group.setIsSelected(false);
        if (currentSelectedPosition == position) {
            currentSelectedPosition = NOT_SELECTED;
        }
        if (lastSelectedPosition == position) {
            lastSelectedPosition = WAS_NOT_SELECTED;
        }

        if (lastSelectedPosition == mGroups.size()) {
            lastSelectedPosition = mGroups.size() - 1;
        }
        notifyItemChanged(position);
        return group;
    }

    public void addItem(Group group, int position) {
        mGroups.add(position, group);
        notifyItemInserted(position);
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

        if (holder.mGroup.isSelected()) currentSelectedPosition = position;

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

    public ArrayList<Group> getList() {
        return mGroups;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView mCheckCircle;
        final TextView mGroupTitle;
        final TextView mGroupDescription;
        final ImageButton mEditButton;
        public Group mGroup;
        public View mRoot;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mRoot = itemView;
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
                    currentSelectedPosition = getAdapterPosition();
                    mGroup.setIsSelected(!mGroup.isSelected());
                    if (lastSelectedPosition != WAS_NOT_SELECTED &&
                            lastSelectedPosition != currentSelectedPosition) {
                        mGroups.get(lastSelectedPosition).setIsSelected(false);
                        notifyItemChanged(lastSelectedPosition);
                    }
                    notifyItemChanged(currentSelectedPosition);
                    lastSelectedPosition = currentSelectedPosition;
                    if (mOnGroupClickListener != null) {
                        mOnGroupClickListener.onGroupClicked(mGroup, currentSelectedPosition);
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
