package com.indevelopment.sock.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.indevelopment.sock.R;
import com.indevelopment.sock.model.License;

import java.util.ArrayList;
import java.util.List;

public class MyArrayAdapter extends ArrayAdapter<License> {
    private Context mContext;
    private List<License> mList;
    private int mLayout;

    public MyArrayAdapter(Context context, int layout, ArrayList<License> list) {
        super(context, layout, list);
        mContext = context;
        mList = list;
        mLayout = layout;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(mLayout, parent, false);
        }

        License license = mList.get(position);

        TextView libraryName = convertView.findViewById(R.id.library_name);
        TextView libraryLicense = convertView.findViewById(R.id.library_license);

        libraryName.setText(license.getLibraryName());
        libraryLicense.setText(license.getLibraryLicense());

        return convertView;
    }
}
