package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.data.DatabaseContract;

/**
 * Created by arpyvanyan on 12/26/15.
 */
public class LatestGameWidgetService extends IntentService {

    public LatestGameWidgetService() {
        super(LatestGameWidgetService.class.getName());
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public LatestGameWidgetService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Retrieve all of the Today widget ids: these are the widgets we need to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                LatestGameWidgetProvider.class));

        // Get today
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH:mm", Locale.US);

        // find latest game with content provider
        Uri latestGameUri = DatabaseContract.ScoreEntry.buildLatestScore();
        Cursor cursor = getContentResolver().query(
                latestGameUri,
                null,
                null,
                new String[] { simpleDateFormat.format(date), simpleTimeFormat.format(date) },
                DatabaseContract.ScoreEntry.DATE_COL +" DESC, " + DatabaseContract.ScoreEntry.TIME_COL + " DESC");

        if (cursor == null) {
            return;
        }

        if (!cursor.moveToFirst()) {
            cursor.close();

            for (int appWidgetId : appWidgetIds) {
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_latest_game);

                views.setTextViewText(R.id.date_textview, getString(R.string.message_no_data));

                Intent anIntent = new Intent(this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, anIntent, 0);
                views.setOnClickPendingIntent(R.id.widget_latest_game, pendingIntent);

                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
            return;
        }

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_latest_game);

            String homeName = cursor.getString(cursor.getColumnIndex(DatabaseContract.ScoreEntry.HOME_COL));
            String awayName = cursor.getString(cursor.getColumnIndex(DatabaseContract.ScoreEntry.AWAY_COL));

            views.setTextViewText(R.id.home_name, homeName);
            views.setTextViewText(R.id.away_name, awayName);

            views.setTextViewText(R.id.score_textview, Utilies.getScores(cursor.getInt(cursor.getColumnIndex(DatabaseContract.ScoreEntry.HOME_GOALS_COL)), cursor.getInt(cursor.getColumnIndex(DatabaseContract.ScoreEntry.AWAY_GOALS_COL))));
            views.setTextViewText(R.id.date_textview, cursor.getString(cursor.getColumnIndex(DatabaseContract.ScoreEntry.TIME_COL)));

            setCrestImage(views, R.id.home_crest, cursor.getString(cursor.getColumnIndex(DatabaseContract.ScoreEntry.HOME_LOGO_COL)));
            setCrestImage(views, R.id.away_crest, cursor.getString(cursor.getColumnIndex(DatabaseContract.ScoreEntry.AWAY_LOGO_COL)));

            // set content description on team crests
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setRemoteContentDescription(views, R.id.home_crest, homeName);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setRemoteContentDescription(views, R.id.away_crest, awayName);
            }


            Intent anIntent = new Intent(this, MainActivity.class);
            Bundle extras = new Bundle();
            extras.putString(LatestGameWidgetProvider.SCORES_DATE,
                    cursor.getString(cursor.getColumnIndex(DatabaseContract.ScoreEntry.DATE_COL)));
            extras.putInt(TodayGamesWidgetProvider.SCORES_MATCH_ID,
                    cursor.getInt(cursor.getColumnIndex(DatabaseContract.ScoreEntry.MATCH_ID)));
            anIntent.putExtras(extras);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, anIntent, Intent.FILL_IN_ACTION);
            views.setOnClickPendingIntent(R.id.widget_latest_game, pendingIntent);

            // update the widget with the set views
            appWidgetManager.updateAppWidget(appWidgetId, views);

        }

        cursor.close();

    }

    private void setCrestImage(RemoteViews views, int viewId, String imageUrl) {
        try {
            Bitmap bitmap = Glide.with(LatestGameWidgetService.this)
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
