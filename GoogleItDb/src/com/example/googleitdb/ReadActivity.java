package com.example.googleitdb;

import android.os.Bundle;
import java.util.Locale;

import android.widget.TextView;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.app.Activity;
import android.view.Menu;

public class ReadActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_read);
		TextView view = (TextView) findViewById(R.id.read_text);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.read, menu);
		return true;
	}

}
