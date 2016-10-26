package it.jaschke.alexandria.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import it.jaschke.alexandria.MainActivity;
import it.jaschke.alexandria.R;
import it.jaschke.alexandria.data.AlexandriaContract;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class BookService extends IntentService {

    private final String LOG_TAG = BookService.class.getSimpleName();

    public static final String FETCH_BOOK = "it.jaschke.alexandria.services.action.FETCH_BOOK";
    public static final String DELETE_BOOK = "it.jaschke.alexandria.services.action.DELETE_BOOK";
    public static final String DELETE_CACHED_BOOK = "it.jaschke.alexandria.services.action.DELETE_CACHED_BOOKS";
    public static final String SAVE_BOOK = "it.jaschke.alexandria.services.action.SAVE_BOOK";

    public static final String EAN = "it.jaschke.alexandria.services.extra.EAN";

    public BookService() {
        super("Alexandria");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (FETCH_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(EAN);
                fetchBook(ean);
            } else if (DELETE_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(EAN);
                deleteBook(ean);
            } else if (SAVE_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(EAN);
                if(saveBook(ean) > 0) {
                    Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
                    messageIntent.putExtra(MainActivity.MESSAGE_KEY, getResources().getString(R.string.message_book_successfully_saved));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
                }
            } else if(DELETE_CACHED_BOOK.equals(action)) {
                deleteCachedBooks();
            }
        }
    }

    /**
     * Handle book deletion
     * parameters.
     */
    private void deleteBook(String ean) {
        if(ean!=null) {
            getContentResolver().delete(
                    AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                    null,
                    null
            );
        }
    }

    /**
     * Handle all cached books deletion
     * parameters.
     */
    private void deleteCachedBooks() {
        getContentResolver().delete(
                AlexandriaContract.BookEntry.CONTENT_URI,
                AlexandriaContract.BookEntry.IS_SAVED + " = ?",
                new String[] {"0"}
        );
    }

    /**
     * Handle book fetch by given ean
     * parameters.
     */
    private boolean fetchBook(String ean) {

        if(ean.length()!=13){
            return false;
        }

        boolean isBookFound = false;
        boolean isBookSaved = false;

        Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);

        Cursor bookEntry = getContentResolver().query(
                AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        if(bookEntry != null) {     //check for null pointer exception
            if(bookEntry.getCount()>0){
                if(bookEntry.moveToFirst()) {
                    isBookFound = true;
                    isBookSaved = bookEntry.getInt(bookEntry.getColumnIndex(AlexandriaContract.BookEntry.IS_SAVED)) != 0;
                    if(isBookSaved) {
                        messageIntent.putExtra(MainActivity.MESSAGE_KEY, getResources().getString(R.string.message_book_is_already_saved));
                    }
                }
                bookEntry.close();
            }

            bookEntry.close();
        }

        if(!isBookFound) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String bookJsonString = null;

            try {
                final String GOOGLE_API_BASE_URL = getString(R.string.google_api_url);
                final String QUERY_PARAM = "q";

                final String ISBN_PARAM = "isbn:" + ean;

                Uri builtUri = Uri.parse(GOOGLE_API_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, ISBN_PARAM)
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return false;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                    buffer.append("\n");
                }

                if (buffer.length() == 0) {
                    return false;
                }
                bookJsonString = buffer.toString();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error ", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }

            }

            final String ITEMS = "items";

            final String VOLUME_INFO = "volumeInfo";

            final String TITLE = "title";
            final String SUBTITLE = "subtitle";
            final String AUTHORS = "authors";
            final String DESC = "description";
            final String CATEGORIES = "categories";
            final String IMG_URL_PATH = "imageLinks";
            final String IMG_URL = "thumbnail";

            try {
                JSONObject bookJson = new JSONObject(bookJsonString);
                JSONArray bookArray;
                if (!bookJson.has(ITEMS)) {
                    messageIntent.putExtra(MainActivity.MESSAGE_KEY, getResources().getString(R.string.not_found));
                } else {

                    bookArray = bookJson.getJSONArray(ITEMS);

                    JSONObject bookInfo = ((JSONObject) bookArray.get(0)).getJSONObject(VOLUME_INFO);

                    String title = bookInfo.getString(TITLE);

                    String subtitle = "";
                    if (bookInfo.has(SUBTITLE)) {
                        subtitle = bookInfo.getString(SUBTITLE);
                    }

                    String desc = "";
                    if (bookInfo.has(DESC)) {
                        desc = bookInfo.getString(DESC);
                    }

                    String imgUrl = "";
                    if (bookInfo.has(IMG_URL_PATH) && bookInfo.getJSONObject(IMG_URL_PATH).has(IMG_URL)) {
                        imgUrl = bookInfo.getJSONObject(IMG_URL_PATH).getString(IMG_URL);
                    }

                    writeBackBook(ean, title, subtitle, desc, imgUrl);

                    if (bookInfo.has(AUTHORS)) {
                        writeBackAuthors(ean, bookInfo.getJSONArray(AUTHORS));
                    }
                    if (bookInfo.has(CATEGORIES)) {
                        writeBackCategories(ean, bookInfo.getJSONArray(CATEGORIES));
                    }

                    isBookFound = true;
                }

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
            }
        }

        // send message back to the mainactivity about the result
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);

        return isBookFound;
    }

    /**
     * Handle book saving by updating is_saved value
     * @param ean
     * @return int
     */
    private int saveBook(String ean) {
        if (ean == null) {
            return 0;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(AlexandriaContract.BookEntry.IS_SAVED, 1);

        return getContentResolver().update(
                AlexandriaContract.BookEntry.CONTENT_URI,
                contentValues,
                AlexandriaContract.BookEntry._ID + " = ?",
                new String[] {ean}
        );
    }

    private void writeBackBook(String ean, String title, String subtitle, String desc, String imgUrl) {
        ContentValues values= new ContentValues();
        values.put(AlexandriaContract.BookEntry._ID, ean);
        values.put(AlexandriaContract.BookEntry.TITLE, title);
        values.put(AlexandriaContract.BookEntry.IMAGE_URL, imgUrl);
        values.put(AlexandriaContract.BookEntry.SUBTITLE, subtitle);
        values.put(AlexandriaContract.BookEntry.DESC, desc);
        getContentResolver().insert(AlexandriaContract.BookEntry.CONTENT_URI,values);
    }

    private void writeBackAuthors(String ean, JSONArray jsonArray) throws JSONException {
        ContentValues values= new ContentValues();
        for (int i = 0; i < jsonArray.length(); i++) {
            values.put(AlexandriaContract.AuthorEntry._ID, ean);
            values.put(AlexandriaContract.AuthorEntry.AUTHOR, jsonArray.getString(i));
            getContentResolver().insert(AlexandriaContract.AuthorEntry.CONTENT_URI, values);
            values= new ContentValues();
        }
    }

    private void writeBackCategories(String ean, JSONArray jsonArray) throws JSONException {
        ContentValues values= new ContentValues();
        for (int i = 0; i < jsonArray.length(); i++) {
            values.put(AlexandriaContract.CategoryEntry._ID, ean);
            values.put(AlexandriaContract.CategoryEntry.CATEGORY, jsonArray.getString(i));
            getContentResolver().insert(AlexandriaContract.CategoryEntry.CONTENT_URI, values);
            values= new ContentValues();
        }
    }
 }