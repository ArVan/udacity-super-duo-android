package barqsoft.footballscores;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.bumptech.glide.Glide;

import barqsoft.footballscores.data.DatabaseContract;

/**
 * Created by yehya khaled on 2/26/2015.
 */
public class ScoresAdapter extends CursorAdapter    //renamed from lowercase to uppercase
{
    public static final String LOG_TAG = ScoresAdapter.class.getSimpleName();

    public double detail_match_id = 0;

    public ScoresAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View mItem = LayoutInflater.from(context).inflate(R.layout.scores_list_item, parent, false);

        ViewHolder mHolder = new ViewHolder(mItem);
        mItem.setTag(mHolder);

        return mItem;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final ViewHolder mHolder = (ViewHolder) view.getTag();

        String homeName = cursor.getString(cursor.getColumnIndex(DatabaseContract.ScoreEntry.HOME_COL));
        String awayName = cursor.getString(cursor.getColumnIndex(DatabaseContract.ScoreEntry.AWAY_COL));

        mHolder.home_name.setText(homeName);
        mHolder.away_name.setText(awayName);
        mHolder.date.setText(cursor.getString(cursor.getColumnIndex(DatabaseContract.ScoreEntry.TIME_COL)));
        mHolder.score.setText(Utilies.getScores(cursor.getInt(cursor.getColumnIndex(DatabaseContract.ScoreEntry.HOME_GOALS_COL)), cursor.getInt(cursor.getColumnIndex(DatabaseContract.ScoreEntry.AWAY_GOALS_COL))));
        mHolder.match_id = cursor.getDouble(cursor.getColumnIndex(DatabaseContract.ScoreEntry.MATCH_ID));

        Glide.with(context).load(cursor.getString(cursor.getColumnIndex(DatabaseContract.ScoreEntry.HOME_LOGO_COL))).into(mHolder.home_crest);
        Glide.with(context).load(cursor.getString(cursor.getColumnIndex(DatabaseContract.ScoreEntry.AWAY_LOGO_COL))).into(mHolder.away_crest);

        mHolder.home_crest.setContentDescription(homeName);
        mHolder.away_crest.setContentDescription(awayName);

        mHolder.league_text.setText(Utilies.getLeague(context, cursor.getInt(cursor.getColumnIndex(DatabaseContract.ScoreEntry.LEAGUE_COL))));
        mHolder.matchday_text.setText(Utilies.getMatchDay(context, cursor.getInt(cursor.getColumnIndex(DatabaseContract.ScoreEntry.MATCH_DAY)),
                cursor.getInt(cursor.getColumnIndex(DatabaseContract.ScoreEntry.LEAGUE_COL))));

        Button share_button = (Button) view.findViewById(R.id.share_button);
        share_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //add Share Action
                context.startActivity(createShareMatchScoreIntent(mHolder.home_name.getText() + " "
                        + mHolder.score.getText() + " " + mHolder.away_name.getText() + " " + context.getString(R.string.share_hashtag)));
            }
        });
    }

    public Intent createShareMatchScoreIntent(String shareText) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        return shareIntent;
    }

}
