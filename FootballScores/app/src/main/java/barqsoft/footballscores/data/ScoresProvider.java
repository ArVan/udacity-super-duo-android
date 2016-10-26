package barqsoft.footballscores.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;

/**
 * Created by yehya khaled on 2/25/2015.
 */
public class ScoresProvider extends ContentProvider {
    private static ScoresDBHelper mOpenHelper;
    private static final int MATCHES = 100;
    private static final int MATCHES_WITH_LEAGUE = 101;
    private static final int MATCHES_WITH_ID = 102;
    private static final int MATCHES_WITH_DATE = 103;
    private static final int MATCH_LATEST = 104;

    private UriMatcher muriMatcher = buildUriMatcher();

    private static final String SCORES_BY_LEAGUE = DatabaseContract.ScoreEntry.LEAGUE_COL + " = ?";
    private static final String SCORES_BY_DATE =
            DatabaseContract.ScoreEntry.DATE_COL + " LIKE ?";
    private static final String SCORES_BY_ID =
            DatabaseContract.ScoreEntry.MATCH_ID + " = ?";
    private static final String SCORES_LATEST =
            DatabaseContract.ScoreEntry.DATE_COL + " <= ? AND " + DatabaseContract.ScoreEntry.TIME_COL + " <= ?";


    public static final String ACTION_DATA_UPDATED = DatabaseContract.CONTENT_AUTHORITY + ".ACTION_DATA_UPDATED";

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = DatabaseContract.BASE_CONTENT_URI.toString();
        matcher.addURI(authority, null, MATCHES);
        matcher.addURI(authority, "league", MATCHES_WITH_LEAGUE);
        matcher.addURI(authority, "id", MATCHES_WITH_ID);
        matcher.addURI(authority, "date", MATCHES_WITH_DATE);
        matcher.addURI(authority, "recent", MATCH_LATEST);
        return matcher;
    }

    private int match_uri(Uri uri) {
        String link = uri.toString();
        {
            if (link.contentEquals(DatabaseContract.BASE_CONTENT_URI.toString())) {
                return MATCHES;
            } else if (link.contentEquals(DatabaseContract.ScoreEntry.buildScoreWithDate().toString())) {
                return MATCHES_WITH_DATE;
            } else if (link.contentEquals(DatabaseContract.ScoreEntry.buildScoreWithId().toString())) {
                return MATCHES_WITH_ID;
            } else if (link.contentEquals(DatabaseContract.ScoreEntry.buildScoreWithLeague().toString())) {
                return MATCHES_WITH_LEAGUE;
            } else if (link.contentEquals(DatabaseContract.ScoreEntry.buildLatestScore().toString())) {
                return MATCH_LATEST;
            }
        }
        return -1;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new ScoresDBHelper(getContext());
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        final int match = muriMatcher.match(uri);
        switch (match) {
            case MATCHES:
                return DatabaseContract.ScoreEntry.CONTENT_TYPE;
            case MATCHES_WITH_LEAGUE:
                return DatabaseContract.ScoreEntry.CONTENT_TYPE;
            case MATCHES_WITH_ID:
                return DatabaseContract.ScoreEntry.CONTENT_ITEM_TYPE;
            case MATCHES_WITH_DATE:
                return DatabaseContract.ScoreEntry.CONTENT_TYPE;
            case MATCH_LATEST:
                return DatabaseContract.ScoreEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri : " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor;
        //Log.v(FetchScoreTask.LOG_TAG,uri.getPathSegments().toString());
        int match = match_uri(uri);
        //Log.v(FetchScoreTask.LOG_TAG,SCORES_BY_LEAGUE);
        //Log.v(FetchScoreTask.LOG_TAG,selectionArgs[0]);
        //Log.v(FetchScoreTask.LOG_TAG,String.valueOf(match));
        switch (match) {
            case MATCHES:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        DatabaseContract.SCORES_TABLE,
                        projection, null, null, null, null, sortOrder);
                break;
            case MATCHES_WITH_DATE:
                //Log.v(FetchScoreTask.LOG_TAG,selectionArgs[1]);
                //Log.v(FetchScoreTask.LOG_TAG,selectionArgs[2]);
                retCursor = mOpenHelper.getReadableDatabase().query(
                        DatabaseContract.SCORES_TABLE,
                        projection, SCORES_BY_DATE, selectionArgs, null, null, sortOrder);
                break;
            case MATCHES_WITH_ID:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        DatabaseContract.SCORES_TABLE,
                        projection, SCORES_BY_ID, selectionArgs, null, null, sortOrder);
                break;
            case MATCHES_WITH_LEAGUE:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        DatabaseContract.SCORES_TABLE,
                        projection, SCORES_BY_LEAGUE, selectionArgs, null, null, sortOrder);
                break;
            case MATCH_LATEST:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        DatabaseContract.SCORES_TABLE,
                        projection, SCORES_LATEST, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri " + uri);
        }

        if (getContext() != null) {
            retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return retCursor;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        //db.delete(DatabaseContract.SCORES_TABLE,null,null);
        //Log.v(FetchScoreTask.LOG_TAG,String.valueOf(muriMatcher.match(uri)));
        switch (match_uri(uri)) {
            case MATCHES:
                db.beginTransaction();
                int returncount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insertWithOnConflict(DatabaseContract.SCORES_TABLE, null, value,
                                SQLiteDatabase.CONFLICT_REPLACE);
                        if (_id != -1) {
                            returncount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                if (getContext() != null) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }

                return returncount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }
}
