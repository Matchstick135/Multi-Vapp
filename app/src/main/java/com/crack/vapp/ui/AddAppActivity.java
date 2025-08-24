package com.crack.vapp.ui;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import static com.crack.vapp.utils.AppInfoUtils.getAllNonSystemApps;
import com.crack.vapp.R;

import java.util.List;

public class AddAppActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private AddAppAdapter appAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_app);

        recyclerView = findViewById(R.id.recycler_view_add_app);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        List<ApplicationInfo> appInfoList = getAllNonSystemApps(this);

        appAdapter = new AddAppAdapter(appInfoList);
        recyclerView.setAdapter(appAdapter);
    }
}