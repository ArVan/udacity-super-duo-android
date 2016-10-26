package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.data.DatabaseContract;

/**
 * Created by arpy on 1/9/16.
 */
public class TodayGamesRemoteViewsFactory implements RemoteViewsFactory {
    private static final String LOG_TAG = TodayGamesRemoteViewsFactory.class.getSimpleName();
    private Context mContext;
    private Cursor mCursor = null;

    public TodayGamesRemoteViewsFactory(Context context) {
        mContext = context;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        if (mCursor != null) {
            mCursor.close();
        }

        // clear the calling identity
        final long identityToken = Binder.clearCallingIdentity();

        // set the date for today
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        // load the fixtures of today
        mCursor = mContext.getContentResolver().query(
                DatabaseContract.ScoreEntry.buildScoreWithDate(),
                null,
                null,
                new String[] { simpleDateFormat.format(date) },
                DatabaseContract.ScoreEntry.TIME_COL + " ASC, " + DatabaseContract.ScoreEntry.HOME_COL + " ASC");

        // and restore the identity again
        Binder.restoreCallingIdentity(identityToken);

    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
    }

    @Override
    public int getCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position == AdapterView.INVALID_POSITION || mCursor == null || !mCursor.moveToPosition(position)) {
            return null;
        }

        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.widget_today_games_list_item);

        String homeName = mCursor.getString(mCursor.getColumnIndex(DatabaseContract.ScoreEntry.HOME_COL));
        String awayName = mCursor.getString(mCursor.getColumnIndex(DatabaseContract.ScoreEntry.AWAY_COL));

        views.setTextViewText(R.id.home_name, homeName);
        views.setTextViewText(R.id.away_name, awayName);

        views.setTextViewText(R.id.score_textview, Utilies.getScores(mCursor.getInt(mCursor.getColumnIndex(DatabaseContract.ScoreEntry.HOME_GOALS_COL)), mCursor.getInt(mCursor.getColumnIndex(DatabaseContract.ScoreEntry.AWAY_GOALS_COL))));
        views.setTextViewText(R.id.date_textview, mCursor.getString(mCursor.getColumnIndex(DatabaseContract.ScoreEntry.TIME_COL)));

        setCrestImage(views, R.id.home_crest, mCursor.getString(mCursor.getColumnIndex(DatabaseContract.ScoreEntry.HOME_LOGO_COL)));
        setCrestImage(views, R.id.away_crest, mCursor.getString(mCursor.getColumnIndex(DatabaseContract.ScoreEntry.AWAY_LOGO_COL)));

        // set content description on team crests
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            setRemoteContentDescription(views, R.id.home_crest, homeName);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            setRemoteContentDescription(views, R.id.away_crest, awayName);
        }

        final Intent fillInIntent = new Intent();
        fillInIntent.putExtra(LatestGameWidgetProvider.SCORES_DATE, mCursor.getString(mCursor.getColumnIndex(DatabaseContract.ScoreEntry.DATE_COL)));
        fillInIntent.putExtra(TodayGamesWidgetProvider.SCORES_MATCH_ID, mCursor.getInt(mCursor.getColumnIndex(DatabaseContract.ScoreEntry.MATCH_ID)));
        views.setOnClickFillInIntent(R.id.widget_today_games_list_item, fillInIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return new RemoteViews(mContext.getPackageName(), R.layout.scores_list_item);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        if (mCursor.moveToPosition(position))
            return mCursor.getLong(0);
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }


    private void setCrestImage(RemoteViews views, int viewId, String imageUrl) {
        try {
            Bitmap bitmap = Glide.with(mContext)
                    .load(imageUrl)
                    .asBitmap()
                    .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .get();
            views.setImageViewBitmap(viewId, bitmap);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, int viewId, String description) {
        views.setContentDescription(viewId, description);
    }
}
