package com.zfy.networkplayer;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    ListView channelList, filterList, filterList2;
    String secondTableName = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        channelList = findViewById(R.id.channelList);
        filterList = findViewById(R.id.filterlList);
        filterList2 = findViewById(R.id.filterlList2);
        filterList.setVisibility(View.GONE);
        filterList2.setVisibility(View.GONE);
        channelList.setOnItemClickListener(this);
        filterList.setOnItemClickListener(this);
        filterList2.setOnItemClickListener(this);
        flushList(channelList,"channelinfo");
        DrawerLayout drawerLayout = findViewById(R.id.drawlayout);
        drawerLayout.addDrawerListener(drawerListener);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()){
            case R.id.channelList:
                secondTableName = getNextTableName("channelinfo",position+1);
                Log.d("zhangfy",secondTableName);
                flushList(filterList, secondTableName);
                if(filterList.getVisibility() != View.VISIBLE)
                filterList.setVisibility(View.VISIBLE);
                break;
            case R.id.filterlList:
                flushList(filterList2,getNextTableName(secondTableName,position+1));
                if(filterList2.getVisibility() != View.VISIBLE)
                filterList2.setVisibility(View.VISIBLE);
                break;
        }
    }

    DrawerLayout.DrawerListener drawerListener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerSlide(@NonNull View view, float v) {

        }

        @Override
        public void onDrawerOpened(@NonNull View view) {

        }

        @Override
        public void onDrawerClosed(@NonNull View view) {
            filterList.setVisibility(View.GONE);
            filterList2.setVisibility(View.GONE);
        }

        @Override
        public void onDrawerStateChanged(int i) {

        }
    };

    void flushList(ListView listView, String tableName){
        SQLiteDatabase db = (new MyDatabaseHelper(this,"channelInfo.db",null,1)).getReadableDatabase();
        Cursor cursor = db.query(tableName,null,null,null,null,null,null);
        List<String> list = new ArrayList<>();
        if(cursor.moveToFirst()){
            do{
                list.add(cursor.getString(cursor.getColumnIndex("NAME")));
            }while (cursor.moveToNext());
        }
        cursor.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,list);
        listView.setAdapter(adapter);
    }

    String getNextTableName(String currentTableName,int position){
        SQLiteDatabase db = (new MyDatabaseHelper(this,"channelInfo.db",null,1)).getReadableDatabase();
        Cursor cursor = db.query(currentTableName,null,"id="+position,null,null,null,null);
        String nextTableName = null;
        if(cursor.moveToFirst())
            nextTableName = cursor.getString(cursor.getColumnIndex("FILTERNAME"));
        cursor.close();
        return  nextTableName;
    }

}
