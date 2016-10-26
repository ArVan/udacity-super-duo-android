package barqsoft.footballscores.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.data.ScoresProvider;
import barqsoft.footballscores.service.ScoresFetchService;

/**
 * Created by arpy on 12/17/15.
 */
public class LatestGameWidgetProvider extends AppWidgetProvider {
    public static String SCORES_DATE = "barqsoft.footballscores.latestgamewidget.DATE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        Intent footballDataService = new Intent(context, ScoresFetchService.class);
        context.startService(footballDataService);

        context.startService(new Intent(context, LatestGameWidgetService.class));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        context.startService(new Intent(context, LatestGameWidgetService.class));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if(intent.getAction().equals(ScoresProvider.ACTION_DATA_UPDATED)) {
            context.startService(new Intent(context, LatestGameWidgetService.class));
        }
    }
}
