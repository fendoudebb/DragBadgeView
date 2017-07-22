package com.fendoudebb.dragbadgeview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import java.util.Arrays;

import static com.fendoudebb.dragbadgeview.Utils.NAMES;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.listview);

        listView.setAdapter(new SampleAdapter(Arrays.asList(NAMES)));
    }
}
