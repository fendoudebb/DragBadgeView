package com.fendoudebb.dragbadgeview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.fendoudebb.view.DragBadgeView;

import java.util.Arrays;

import static com.fendoudebb.dragbadgeview.Utils.NAMES;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.listview);

        listView.setAdapter(new SampleAdapter(Arrays.asList(NAMES)));

        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DragBadgeView bdv = (DragBadgeView) view.findViewById(R.id.drag_view);
        String stringText = bdv.getStringText();
        int intText = bdv.getIntText();
        Toast.makeText(getApplicationContext(), "String: " + stringText + ", int: " + intText,
                Toast.LENGTH_SHORT).show();
    }
}
