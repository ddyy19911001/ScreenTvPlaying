package com.zane.androidupnpdemo.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.zane.androidupnpdemo.entity.ClingDevice;
import com.zane.clinglibrary.R;

import org.fourthline.cling.model.meta.Device;

import yin.deng.superbase.activity.LogUtils;

/**
 * 说明：
 * 作者：zhouzhan
 * 日期：17/6/28 15:50
 */

public class DevicesAdapter extends ArrayAdapter<ClingDevice> {
    private Context context;

    public DevicesAdapter(Context context) {
        super(context, 0);
        this.context=context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.devices_items, null);

        ClingDevice item = getItem(position);
        if (item == null || item.getDevice() == null) {
            return convertView;
        }

        Device device = item.getDevice();

        ImageView imageView = (ImageView)convertView.findViewById(R.id.listview_item_image);
        LogUtils.w("设置图标");
        imageView.setImageResource(R.drawable.tp_icon);

        TextView textView = (TextView) convertView.findViewById(R.id.listview_item_line_one);
        textView.setText(device.getDetails().getFriendlyName());
        textView.setTextColor(Color.BLACK);
        LogUtils.w("设置颜色");
        return convertView;
    }
}