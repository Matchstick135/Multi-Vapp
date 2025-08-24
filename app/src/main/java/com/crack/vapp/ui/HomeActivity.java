package com.crack.vapp.ui;

import android.content.pm.ApplicationInfo;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageButton;

import com.crack.vapp.BaseApplication;
import com.crack.vapp.R;
import com.crack.vapp.utils.AppInfoUtils;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private HomeAppAdapter homeAppAdapter;
    private ImageButton imageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        BaseApplication.baseActivity = this;

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        homeAppAdapter = new HomeAppAdapter(getApplicationInfoList());
        recyclerView.setAdapter(homeAppAdapter);

        imageButton = findViewById(R.id.imageButton);
        imageButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddAppActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRecyclerViewData();
    }

    private List<ApplicationInfo> getApplicationInfoList() {
        List<ApplicationInfo> applicationInfos = AppInfoUtils.getSavedAppInfos(this);
        return applicationInfos != null ? applicationInfos : new ArrayList<>();
    }

    private void refreshRecyclerViewData() {
        List<ApplicationInfo> updatedAppList = getApplicationInfoList();
        homeAppAdapter.updateData(updatedAppList);
    }
}

