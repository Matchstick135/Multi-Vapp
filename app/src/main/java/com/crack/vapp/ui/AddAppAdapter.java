package com.crack.vapp.ui;

import android.content.pm.ApplicationInfo;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import static com.crack.vapp.core.InstallApp.install;
import com.crack.vapp.BaseApplication;
import com.crack.vapp.R;
import com.crack.vapp.utils.AppInfoUtils;

import java.util.List;

public class AddAppAdapter extends RecyclerView.Adapter<AddAppAdapter.ViewHolder> {
    private List<ApplicationInfo> appList;

    public AddAppAdapter(List<ApplicationInfo> appList) {
        this.appList = appList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_add, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ApplicationInfo appInfo = appList.get(position);
        holder.appIcon.setImageDrawable(appInfo.loadIcon(holder.itemView.getContext().getPackageManager()));
        holder.appName.setText(appInfo.loadLabel(holder.itemView.getContext().getPackageManager()));
        holder.installButton.setOnClickListener(v -> {
            Toast toast1 = Toast.makeText(
                    holder.itemView.getContext(),
                    appInfo.loadLabel(holder.itemView.getContext().getPackageManager()) + " 开始安装",
                    Toast.LENGTH_SHORT
            );
            toast1.show();
            install(BaseApplication.baseActivity, appInfo);
            Toast toast2 = Toast.makeText(
                    holder.itemView.getContext(),
                    appInfo.loadLabel(holder.itemView.getContext().getPackageManager()) + " 安装完成",
                    Toast.LENGTH_SHORT
            );
            toast2.show();
            AppInfoUtils.saveString(BaseApplication.baseActivity, appInfo.packageName);
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        Button installButton;

        public ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            installButton = itemView.findViewById(R.id.install_button);
        }
    }
}