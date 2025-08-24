package com.crack.vapp.ui;

import android.content.pm.ApplicationInfo;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.crack.vapp.R;
import com.crack.vapp.core.Begin;

import java.util.List;

class HomeAppAdapter extends RecyclerView.Adapter<HomeAppAdapter.ViewHolder> {
    private List<ApplicationInfo> appList;

    public HomeAppAdapter(List<ApplicationInfo> appList) {
        this.appList = appList;
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ApplicationInfo appInfo = appList.get(position);
        holder.appIcon.setImageDrawable(appInfo.loadIcon(holder.itemView.getContext().getPackageManager()));
        holder.appName.setText(appInfo.loadLabel(holder.itemView.getContext().getPackageManager()));
        holder.itemView.setOnClickListener(v -> {
            Begin.begin(appInfo);
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public void updateData(List<ApplicationInfo> newData) {
        this.appList.clear();
        this.appList.addAll(newData);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        android.widget.ImageView appIcon;
        android.widget.TextView appName;

        public ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
        }
    }
}