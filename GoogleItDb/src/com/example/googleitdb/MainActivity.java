package com.example.googleitdb;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
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
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String DEBUG_TAG = "MainActivity";
	private TextView serviceText;
	private TextView serviceText2;
	private TextView serviceText3;
	private static final String ns = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		serviceText = (TextView) findViewById(R.id.service_text);
		serviceText2 = (TextView) findViewById(R.id.service_text2);
		serviceText3 = (TextView) findViewById(R.id.service_text3);
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

	public void switchActivity(View view) {
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			new DownloadWebpageTask().execute(getString(R.string.string_url));
			// Intent intent = new Intent(this, ReadActivity.class);
			// startActivity(intent);
		} else {
			System.out.println("No network connection available.");
		}

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
			serviceText.setText(result.get(0));
			serviceText2.setText(result.get(1));
			serviceText3.setText(result.get(2));

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
			// Reader reader = null;
			// reader = new InputStreamReader(stream, "UTF-8");
			// char[] buffer = new char[len];
			// reader.read(buffer);
			// return new String(buffer);
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
