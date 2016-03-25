package com.softrangers.sonarcloudmobile.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.Day;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Eduard Albu on 25 03 2016
 * project sonarcloud-android
 *
 * @author eduard.albu@gmail.com
 */
public class DaysAdapter extends RecyclerView.Adapter<DaysAdapter.ViewHolder> {
    private static final int WAS_NOT_SELECTED = -2;

    private ArrayList<Day> mDays = Day.getYearDays();
    private StringBuilder mStringBuilder = new StringBuilder();
    private OnDayClickListener mOnDayClickListener;
    private static int currentPosition;
    private static int lastPosition;

    public DaysAdapter() {
        currentPosition = getCurentDatePosition();
        lastPosition = currentPosition;
    }

    public ArrayList<Day> getDays() {
        return mDays;
    }

    private int getCurentDatePosition() {
        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int dayPosition = 0;
        for (int i = 0; i < mDays.size(); i++) {
            Day day = mDays.get(i);
            Date dayDate = day.getDate();
            int month = dayDate.getMonth();
            int dayOfMonth = dayDate.getDate();
            int year = dayDate.getYear() + 1900;
            if (dayOfMonth == currentDay && month == currentMonth && year == currentYear) {
                day.setSelected(true);
                dayPosition = i;
            }
        }
        return dayPosition;
    }

    public int getSelectedPostion() {
        return currentPosition;
    }

    public void setOnDayClickListener(OnDayClickListener onDayClickListener) {
        mOnDayClickListener = onDayClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.day_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Day day = mDays.get(position);
        holder.mDay = day;
        mStringBuilder.delete(0, mStringBuilder.capacity());
        String[] date = day.getMonthAndDay().split(" ");
        mStringBuilder.append(date[0]).append("\n").append(date[1]);
        holder.mDateText.setText(mStringBuilder.toString());
        holder.mRoot.setBackgroundResource(holder.mDay.isSelected() ? R.drawable.bottom_bright_gradient : R.color.colorAccent);
    }

    @Override
    public int getItemCount() {
        return mDays.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView mDateText;
        final View mRoot;
        Day mDay;

        public ViewHolder(View itemView) {
            super(itemView);
            mRoot = itemView;
            itemView.setOnClickListener(this);
            mDateText = (TextView) itemView.findViewById(R.id.days_list_textView);
        }

        @Override
        public void onClick(View v) {
            currentPosition = getAdapterPosition();
            mDay.setSelected(true);
            notifyItemChanged(currentPosition);

            mDays.get(lastPosition).setSelected(false);
            notifyItemChanged(lastPosition);

            lastPosition = currentPosition;
            if (mOnDayClickListener != null) {
                mOnDayClickListener.onDayClick(mDay, currentPosition);
            }
        }
    }

    public interface OnDayClickListener {
        void onDayClick(Day day, int position);
    }
}
