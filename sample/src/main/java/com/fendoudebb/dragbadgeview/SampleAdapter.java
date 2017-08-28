package com.fendoudebb.dragbadgeview;

import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.fendoudebb.view.DragBadgeView;

import java.util.List;

public class SampleAdapter extends BaseAdapter {
    private SparseBooleanArray isDisappear;
    public List<String> mList;

    public SampleAdapter(List<String> list) {
        mList = list;
        isDisappear = new SparseBooleanArray();
    }

    @Override
    public int getCount() {
        return mList == null ? 0 : mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList == null ? null : mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = View.inflate(parent.getContext(), R.layout.listview_item, null);
            holder = new ViewHolder();
            holder.mTextView = (TextView) convertView.findViewById(R.id.text_name);
            holder.mDragBadgeView = (DragBadgeView) convertView.findViewById(R.id.drag_view);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String text = mList.get(position);

        if (position % 2 == 0) {
            holder.mDragBadgeView.setBgColor(Color.BLUE);
        } else {
            holder.mDragBadgeView.setBgColor(Color.RED);
        }

        holder.mTextView.setText(text);
        holder.mDragBadgeView.setText(String.valueOf(position));

        Boolean isDisappeared = isDisappear.get(position);
        if (isDisappeared) {
            holder.mDragBadgeView.setVisibility(View.INVISIBLE);
        } else {
            holder.mDragBadgeView.setVisibility(View.VISIBLE);
        }

        if (position % 2 == 0) {
            holder.mDragBadgeView.setDragEnable(true);
        } else {
            holder.mDragBadgeView.setDragEnable(false);
        }

        holder.mDragBadgeView.setOnDragBadgeViewListener(new DragBadgeView.OnDragBadgeViewListener() {

            @Override
            public void onDisappear(String text) {
                isDisappear.put(position, true);
                Toast.makeText(parent.getContext().getApplicationContext(), text + "条信息隐藏!", Toast
                        .LENGTH_SHORT).show();
            }
        });

        return convertView;
    }

    private class ViewHolder {
        TextView      mTextView;
        DragBadgeView mDragBadgeView;
    }



}
