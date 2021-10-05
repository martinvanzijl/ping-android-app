package com.example.myfirstapp;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

//
// Based off this example:
// https://stackoverflow.com/questions/40584424/simple-android-recyclerview-example
//
public class WhitelistActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whitelist);

        RecyclerView view = findViewById(R.id.recyclerViewMain);
        view.setLayoutManager(new LinearLayoutManager(this));
        //view.setAdapter(new CustomAdapter(generateData()));
        view.setAdapter(new CustomAdapter(readFromDatabase()));
        view.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    private List<String> readFromDatabase() {
        PingDbHelper database = new PingDbHelper(this);
        Cursor cursor = database.getWhitelistContacts();
        List<String> contacts = new ArrayList<>();
        while(cursor.moveToNext()) {
            String phoneNumber = cursor.getString(
                    cursor.getColumnIndexOrThrow(PingDatabaseContract.WhitelistContactEntry.COLUMN_NAME_PHONE_NUMBER));
            contacts.add(phoneNumber);
        }
        cursor.close();
        return contacts;
    }

    private List<String> generateData() {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            data.add("Example Contact #" + i);
        }
        return data;
    }

    public void onButtonAddClick(View view) {
        // Add to database.
        PingDbHelper helper = new PingDbHelper(this);
        helper.addWhitelistContact("555 1234");

        // Update list view.
        RecyclerView listView = findViewById(R.id.recyclerViewMain);
        listView.setAdapter(new CustomAdapter(readFromDatabase()));
    }


}