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
		SQLiteDatabase db;
		db = openOrCreateDatabase("words.db",
				SQLiteDatabase.CREATE_IF_NECESSARY, null);
		db.setVersion(1);
		db.setLocale(Locale.getDefault());
		db.setLockingEnabled(true);

		Cursor cur = db.query("table_word", null, null, null, null, null, null);
		cur.moveToFirst();
		while (cur.isAfterLast() == false) {
			view.append(" " + cur.getString(cur.getColumnIndex("name")) + " " 
					+ cur.getString(cur.getColumnIndex("first_suggestion"))	+ " " 
					+ cur.getString(cur.getColumnIndex("second_suggestion")) + " " 
					+ cur.getString(cur.getColumnIndex("third_suggestion")));
			cur.moveToNext();
		}
		cur.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.read, menu);
		return true;
	}

}
