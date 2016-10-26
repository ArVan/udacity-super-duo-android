
package barqsoft.footballscores.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ScoresDBHelper extends SQLiteOpenHelper
{
    private static final String LOG_TAG = ScoresDBHelper.class.getSimpleName();
    private static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "Scores.db";

    /**
     * Constructor
     * @param context Context
     */
    public ScoresDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Create the given database, only called if no database exists yet
     * @param db SQLiteDatabase
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

        // sql string for creating the scores table
        final String CreateScoresTable = "CREATE TABLE "+ DatabaseContract.SCORES_TABLE +" ("+
                DatabaseContract.ScoreEntry._ID +" INTEGER PRIMARY KEY,"+
                DatabaseContract.ScoreEntry.DATE_COL +" TEXT NOT NULL,"+
                DatabaseContract.ScoreEntry.TIME_COL +" INTEGER NOT NULL,"+
                DatabaseContract.ScoreEntry.HOME_COL +" TEXT NOT NULL,"+
                DatabaseContract.ScoreEntry.HOME_ID_COL +" INTEGER NOT NULL,"+
                DatabaseContract.ScoreEntry.HOME_LOGO_COL +" TEXT,"+
                DatabaseContract.ScoreEntry.HOME_GOALS_COL +" TEXT NOT NULL,"+
                DatabaseContract.ScoreEntry.AWAY_COL +" TEXT NOT NULL,"+
                DatabaseContract.ScoreEntry.AWAY_ID_COL +" INTEGER NOT NULL,"+
                DatabaseContract.ScoreEntry.AWAY_LOGO_COL +" TEXT,"+
                DatabaseContract.ScoreEntry.AWAY_GOALS_COL +" TEXT NOT NULL,"+
                DatabaseContract.ScoreEntry.LEAGUE_COL +" INTEGER NOT NULL,"+
                DatabaseContract.ScoreEntry.MATCH_ID +" INTEGER NOT NULL,"+
                DatabaseContract.ScoreEntry.MATCH_DAY +" INTEGER NOT NULL,"+
                " UNIQUE ("+ DatabaseContract.ScoreEntry.MATCH_ID +") ON CONFLICT REPLACE);";

        // execute the scores table creation sql
        db.execSQL(CreateScoresTable);
    }

    /**
     * Upgrade the given database, only called if there already exists a database and the given
     *  version number is higher than previously installed version number
     * @param db SQLiteDatabase
     * @param oldVersion int
     * @param newVersion int
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // remove old values when upgrading
        db.execSQL("DROP TABLE IF EXISTS "+ DatabaseContract.SCORES_TABLE);

        // create the upgraded database
        onCreate(db);
    }
}