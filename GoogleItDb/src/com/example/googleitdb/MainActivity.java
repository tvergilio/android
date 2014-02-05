package com.example.googleitdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String DEBUG_TAG = "MainActivity";
	private TextView serviceTextMain;
	private String wordName;
	private int wordID;
	private List<String> result;
	private Map<String, Integer> resultPoints;
	private ListView listview;
//	private TextView listTextView;
	private ArrayAdapter<String> adapter;
	public static final int RESULTS_MAX = 8;
	public static final int LOW = 1;
	public static final int HIGH = 97; // 2260 number of word names in the
										// names.xml resource
	public static final String TABLE_NAME = "table_word";
	public static final int POINTS_FIRST = 10;
	public static final int POINTS_SECOND = 7;
	public static final int POINTS_THIRD = 5;
	public static final int POINTS_FOURTH = 3;
	public static final int POINTS_FIFTH = 0;

	SQLiteDatabase db;
	DatabaseHelper myDbHelper;

	public List<String> getResult() {
		return result;
	}

	public void setResult(List<String> result) {
		this.result = result;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		result = new ArrayList<String>();
		resultPoints = new HashMap<String, Integer>();		
		listview = (ListView) findViewById(R.id.listview);
//		listTextView = (TextView) findViewById(R.id.list_text_view);
		adapter = new ArrayAdapter<String>(MainActivity.this,
				android.R.layout.simple_list_item_1, result);

		listview.setAdapter(adapter);
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {
				final String item = (String) parent.getItemAtPosition(position);

			}

		});

		serviceTextMain = (TextView) findViewById(R.id.service_text_main);

		// db = openOrCreateDatabase("words.db",
		// SQLiteDatabase.CREATE_IF_NECESSARY, null);
		// db.setVersion(1);
		// db.setLocale(Locale.getDefault());
		// db.setLockingEnabled(true);
		// // comment this out after initial load, just to avoid duplicates
		// whilst
		// // debugging
		// //db.execSQL("DROP TABLE IF EXISTS table_word");
		//
		// final String CREATE_TABLE_WORD =
		// "CREATE TABLE IF NOT EXISTS table_word ("
		// + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
		// +
		// "name TEXT UNIQUE, first_suggestion TEXT, second_suggestion TEXT, third_suggestion TEXT, fourth_suggestion TEXT, "
		// + "fifth_suggestion TEXT);";
		//
		// db.execSQL(CREATE_TABLE_WORD);
		// this needs to be run only once, at installation
		// populateDBInitial();
		myDbHelper = new DatabaseHelper(this);
		try {
			myDbHelper.createDataBase();
			myDbHelper.openDataBase();
		} catch (IOException ioe) {
			throw new Error("Unable to create database");
		} catch (SQLException sqle) {
			throw new Error("SQL Exception");
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (wordName != null && result != null && !result.isEmpty()) {
			SharedPreferences sharedPref = getSharedPreferences(
					getString(R.string.preference_file_key), MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putString(getString(R.string.saved_word_name), wordName);
			editor.putStringSet(getString(R.string.saved_word_suggestions),
					new HashSet<String>(result));
			editor.commit();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences sharedPref = getSharedPreferences(
				getString(R.string.preference_file_key), MODE_PRIVATE);
		wordName = sharedPref.getString(getString(R.string.saved_word_name),
				wordName);
		Set<String> resultSet = sharedPref.getStringSet(
				getString(R.string.saved_word_suggestions), null);
		if (resultSet != null) {
			result = new ArrayList<String>(resultSet);
		}
	}

	public void buttonActivity(View view) {
		wordID = getRandomID(HIGH, LOW);

		String[] selectionArgs = { String.valueOf(wordID) };

		Cursor c = myDbHelper.rawQuery("select * from " + TABLE_NAME
				+ " where CAST(_id AS TEXT) = ?", selectionArgs);

		if (c.moveToFirst()) {
			wordName = c.getString(1);
			serviceTextMain.setText(wordName);
			resultPoints.clear();
			resultPoints.put(c.getString(2), POINTS_FIRST);
			resultPoints.put(c.getString(3), POINTS_SECOND);
			resultPoints.put(c.getString(4), POINTS_THIRD);
			resultPoints.put(c.getString(5), POINTS_FOURTH);
			resultPoints.put(c.getString(6), POINTS_FIFTH);
			populateResultsList();
			
		}
	}

	private void populateResultsList() {
		result.clear();
		while (!resultPoints.isEmpty()) {
			int index = resultPoints.size() == 1 ? 0 : getRandomID(resultPoints.size() - 1, 0);
			Object[] keys = resultPoints.keySet().toArray();
			String key = (String) keys[index]; 
			result.add(key);
			resultPoints.remove(key);
		}

		adapter.clear();
		adapter.addAll(result);
		adapter.notifyDataSetChanged();
		
	}

	private int getRandomID(int high, int low) {
		Random r = new Random();
		return r.nextInt(high - low) + low;
	}

}
