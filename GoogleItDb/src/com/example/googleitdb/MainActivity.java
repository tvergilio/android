package com.example.googleitdb;

import java.util.Locale;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		 SQLiteDatabase db;
	        db = openOrCreateDatabase(
	       		"words.db"
	       		, SQLiteDatabase.CREATE_IF_NECESSARY
	       		, null
	      		);	 
	        db.setVersion(1);
	        db.setLocale(Locale.getDefault());
	        db.setLockingEnabled(true);
	        
	        final String CREATE_TABLE_WORD =
	        	"CREATE TABLE table_word ("
	        	+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
	        	+ "name TEXT, first_suggestion TEXT, second_suggestion TEXT, third_suggestion TEXT);";
	       
	        //db.execSQL(CREATE_TABLE_WORD);
	        
	        ContentValues values = new ContentValues();
	        values.put("name", "rainy");
	        values.put("first_suggestion", "rainy mood");
	        values.put("second_suggestion", "rainy days and mondays");
	        values.put("third_suggestion", "rainy night in Georgia");
	        
	        try {
	            db.insertOrThrow("table_word", null, values);
	        } catch (Exception e) {
	            //catch code
	        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void switchActivity(View view) {
	    Intent intent = new Intent(this, ReadActivity.class);
	    startActivity(intent);
	}

}
