package com.example.googleitdb;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

public class DatabaseHelper extends SQLiteOpenHelper {

	public static final String DB_NAME_UK = "uk_words.db"; //change it to use the US one
	public static final String DB_NAME_US = "uk_words.db";
	public static final String TABLE_NAME_WORD = "table_word";
	public static final String TABLE_NAME_FORENAME = "table_forename";
	public static final String TABLE_NAME_SENTENCE = "table_sentence";
	public static final int DATABASE_VERSION = 1;
	public static final String DEBUG_TAG = "DatabaseHelper";
	public static final String RESOURCE_PREFIX_WORDS = "WN";
	public static final String RESOURCE_PREFIX_FORENAMES = "WFN";
	public static final String RESOURCE_PREFIX_SENTENCES = "SGN";
	public static final int POPULATE_DB_START = 0;
	public static final int POPULATE_DB_END = 0;
	private String dbPath;
	private Context context;
	private SQLiteDatabase myDataBase;
	private static final String ns = null;
	private String tableName;
	private String resourcePrefix;
	private String dbName;

	public DatabaseHelper(Context context, String dbName, String resourcePrefix) {
		super(context, dbName, null, DATABASE_VERSION);
		this.context = context;
		this.dbName = dbName;
		this.resourcePrefix = resourcePrefix;
		if (resourcePrefix.equals(RESOURCE_PREFIX_WORDS)) {
			this.tableName = TABLE_NAME_WORD;
		} else if (resourcePrefix.equals(RESOURCE_PREFIX_FORENAMES)) {
			this.tableName = TABLE_NAME_FORENAME;
		} else if (resourcePrefix.equals(RESOURCE_PREFIX_SENTENCES)) {
			this.tableName = TABLE_NAME_SENTENCE;
		} 
		dbPath = context.getDatabasePath(dbName).toString();
	}

	/**
	 * Creates a empty database on the system and rewrites it with your own
	 * database.
	 * */
	public void createDataBase() throws IOException {

		boolean dbExist = checkDataBase();

		if (dbExist) {
			// do nothing - database already exist
		} else {

			// By calling this method and empty database will be created into
			// the default system path
			// of your application so we are gonna be able to overwrite that
			// database with our database.
			this.getReadableDatabase();

			try {

				copyDataBase();

			} catch (IOException e) {

				throw new Error("Error copying database");

			}
		}

	}

	/**
	 * Check if the database already exist to avoid re-copying the file each
	 * time you open the application.
	 * 
	 * @return true if it exists, false if it doesn't
	 */
	private boolean checkDataBase() {

		SQLiteDatabase checkDB = null;

		try {
			String myPath = dbPath;
			checkDB = SQLiteDatabase.openDatabase(myPath, null,
					SQLiteDatabase.OPEN_READONLY);

		} catch (SQLiteException e) {

			// database does't exist yet.

		}

		if (checkDB != null) {

			checkDB.close();

		}

		return checkDB != null ? true : false;
	}

	/**
	 * Copies your database from your local assets-folder to the just created
	 * empty database in the system folder, from where it can be accessed and
	 * handled. This is done by transfering bytestream.
	 * */
	private void copyDataBase() throws IOException {

		// Open your local db as the input stream
		InputStream myInput = context.getAssets().open(dbName);

		// Path to the just created empty db
		String outFileName = dbPath;

		// Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(outFileName);

		// transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer)) > 0) {
			myOutput.write(buffer, 0, length);
		}

		// Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();

	}

	public void openDataBase() throws SQLException {

		// Open the database
		String myPath = dbPath;
		myDataBase = SQLiteDatabase.openDatabase(myPath, null,
				SQLiteDatabase.OPEN_READONLY);
		//OPEN_READWRITE for populating the DB

	}

	@Override
	public synchronized void close() {

		if (myDataBase != null)
			myDataBase.close();

		super.close();

	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	public Cursor rawQuery(String string, String[] selectionArgs) {
		return myDataBase.rawQuery(string, selectionArgs);
	}

	public void populateDBInitial() {

		ConnectivityManager connMgr = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			for (int i = POPULATE_DB_START; i <= POPULATE_DB_END; i++) {
				String resourceName = resourcePrefix + String.format("%04d", i);
				String resourceID = getResourceID();
				int identifier = getStringIdentifier(context, resourceName);
				if (identifier != 0) {
					String name = context.getString(identifier);
					if (name != null) {
						name = name.replace(" ", "%20");
						DownloadWebpageTask task = new DownloadWebpageTask();
						task.setTableName(tableName);
						task.execute(resourceID + name);
					}
				}
			}

		} else {
			System.out.println("No network connection available.");
		}
	}

	private String getResourceID() {
		String resourceID = dbName.equals(DB_NAME_UK) ? context
				.getString(R.string.string_url_uk) : context
				.getString(R.string.string_url);
		return resourceID;
	}

	public static int getStringIdentifier(Context context, String name) {
		return context.getResources().getIdentifier(name, "string",
				context.getPackageName());
	}

	private class DownloadWebpageTask extends
			AsyncTask<String, Void, List<String>> {
		
		private String name;
		private String tableName;
		
		public void setTableName(String tableName) {
			this.tableName = tableName;
		}		

		@Override
		protected List<String> doInBackground(String... urls) {
			// params comes from the execute() call: params[0] is the url.
			try {
				String resourceID = getResourceID();
				if (resourceID != null) {
					name = urls[0].substring(resourceID.length());
					name = name.replace("%20", " ");
				}
				return downloadUrl(urls[0] + "%20");

			} catch (IOException e) {
				Log.d(DEBUG_TAG,
						"Unable to retrieve web page. URL may be invalid.");
				return null;
			}
		}

		// onPostExecute processes the results of the AsyncTask.
		@Override
		protected void onPostExecute(List<String> result) {
			if (name != null && result != null && !result.isEmpty()) {
				dbInsert(tableName, name, result);
			}

		}

		private void dbInsert(String tableName, String wordName, List<String> result) {
			ContentValues values = new ContentValues();
			if (result != null) {
				values.put("name", wordName);
				if (result.size() > 0) {
					values.put("first_suggestion", result.get(0));
				}
				if (result.size() > 1) {
					values.put("second_suggestion", result.get(1));
				}
				if (result.size() > 2) {
					values.put("third_suggestion", result.get(2));
				}
				if (result.size() > 3) {
					values.put("fourth_suggestion", result.get(3));
				}
				if (result.size() > 4) {
					values.put("fifth_suggestion", result.get(4));
				}

				try {
					myDataBase.insertOrThrow(tableName, null, values);
				} catch (Exception e) {
					// catch code
				}
			}
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
