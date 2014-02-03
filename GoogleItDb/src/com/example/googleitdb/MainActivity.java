package com.example.googleitdb;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String DEBUG_TAG = "MainActivity";
	private TextView serviceTextMain;
	private String wordName;
	private List<String> result;
	private static final String ns = null;
	private ListView listview;
	private TextView listTextView;
	private ArrayAdapter<String> adapter;

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
		listview = (ListView) findViewById(R.id.listview);
		listTextView = (TextView) findViewById(R.id.list_text_view);
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
		SQLiteDatabase db;
		db = openOrCreateDatabase("words.db",
				SQLiteDatabase.CREATE_IF_NECESSARY, null);
		db.setVersion(1);
		db.setLocale(Locale.getDefault());
		db.setLockingEnabled(true);
		db.execSQL("DROP TABLE IF EXISTS table_word");

		final String CREATE_TABLE_WORD = "CREATE TABLE IF NOT EXISTS table_word ("
				+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ "name TEXT, first_suggestion TEXT, second_suggestion TEXT, third_suggestion TEXT);";

		db.execSQL(CREATE_TABLE_WORD);

		ContentValues values = new ContentValues();
		values.put("name", "rainy");
		values.put("first_suggestion", "rainy mood");
		values.put("second_suggestion", "rainy days and mondays");
		values.put("third_suggestion", "rainy night in Georgia");

		try {
			db.insertOrThrow("table_word", null, values);
		} catch (Exception e) {
			// catch code
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void buttonActivity(View view) {
		wordName = getRandomWord();
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			new DownloadWebpageTask().execute(getString(R.string.string_url)
					+ wordName + "%20");
			// Intent intent = new Intent(this, ReadActivity.class);
			// startActivity(intent);
			
			
		} else {
			System.out.println("No network connection available.");
		}

	}

	private String getRandomWord() {
		Random r = new Random();
		int Low = 1;
		int High = 2260;
		int R = r.nextInt(High - Low) + Low;
		String resourceName = "WN" + String.format("%04d", R);
		return getString(getStringIdentifier(this, resourceName));
	}

	public static int getStringIdentifier(Context context, String name) {
		return context.getResources().getIdentifier(name, "string",
				context.getPackageName());
	}

	private class DownloadWebpageTask extends
			AsyncTask<String, Void, List<String>> {
		@Override
		protected List<String> doInBackground(String... urls) {

			// params comes from the execute() call: params[0] is the url.
			try {
				return downloadUrl(urls[0]);

			} catch (IOException e) {
				Log.d(DEBUG_TAG,
						"Unable to retrieve web page. URL may be invalid.");
				return null;
			}
		}

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(List<String> result) {
			serviceTextMain.setText(wordName);
			MainActivity.this.result = result;
			adapter.clear();
			adapter.addAll(result);
			adapter.notifyDataSetChanged();
			
		}

		// Given a URL, establishes an HttpUrlConnection and retrieves
		// the web page content as a InputStream, which it returns as
		// a string.
		private List<String> downloadUrl(String myurl) throws IOException {
			InputStream is = null;
			// Only display the first 500 characters of the retrieved
			// web page content.
			int len = 500;

			try {
				URL url = new URL(myurl);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setReadTimeout(10000 /* milliseconds */);
				conn.setConnectTimeout(15000 /* milliseconds */);
				conn.setRequestMethod("GET");
				conn.setDoInput(true);
				// Starts the query
				conn.connect();
				int response = conn.getResponseCode();
				Log.d(DEBUG_TAG, "The response is: " + response);
				is = conn.getInputStream();

				// Return a List<String>
				return readIt(is, len);

				// Makes sure that the InputStream is closed after the app is
				// finished using it.
			} finally {
				if (is != null) {
					is.close();
				}
			}
		}

		// Reads an InputStream and converts it to a String.
		public List<String> readIt(InputStream stream, int len)
				throws IOException, UnsupportedEncodingException {

			List<String> result = null;
			try {
				XmlPullParser parser = Xml.newPullParser();
				parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,
						false);
				parser.setInput(stream, null);
				parser.nextTag();
				result = readFeed(parser);
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				stream.close();
			}
			return result;
		}

		private List<String> readFeed(XmlPullParser parser)
				throws XmlPullParserException, IOException {
			List<String> entries = new ArrayList<String>();
			parser.require(XmlPullParser.START_TAG, ns, "toplevel");
			while (parser.next() != XmlPullParser.END_DOCUMENT) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				// Starts by looking for the entry tag
				if (name.equals("CompleteSuggestion")) {
					parser.require(XmlPullParser.START_TAG, ns,
							"CompleteSuggestion");
					while (parser.next() != XmlPullParser.END_TAG) {
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}
						String name1 = parser.getName();
						// Starts by looking for the entry tag
						if (name1.equals("suggestion")) {
							String suggestion = readAttribute(parser);
							entries.add(suggestion);
						}

					}
				} else {
					skip(parser);
				}
			}
			return entries;
		}

		// For the suggestions, extracts their attributes.
		private String readAttribute(XmlPullParser parser) throws IOException,
				XmlPullParserException {
			return parser.getAttributeValue(ns, "data");
		}

		private void skip(XmlPullParser parser) throws XmlPullParserException,
				IOException {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				throw new IllegalStateException();
			}
			int depth = 1;
			while (depth != 0) {
				switch (parser.next()) {
				case XmlPullParser.END_TAG:
					depth--;
					break;
				case XmlPullParser.START_TAG:
					depth++;
					break;
				}
			}
		}

	}

	

}
