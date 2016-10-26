package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.data.ScoresProvider;
import barqsoft.footballscores.service.ScoresFetchService;

/**
 * Created by arpy on 12/17/15.
 */
public class TodayGamesWidgetProvider extends AppWidgetProvider {
    public static String SCORES_MATCH_ID = "barqsoft.footballscores.todaygameswidget.MATCH_ID";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today_games);

            // set the widget header title
            views.setTextViewText(R.id.widget_title, context.getString(R.string.app_name) + " " + context.getString(R.string.today));

            // create an intent that will launch the mainactivity
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent);

            // set the adapter
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                setRemoteAdapter(context, views);
            } else {
                setRemoteAdapterV11(appWidgetId, context, views);
            }

            // set the intent-template to be used when clicking a listview item that launches the mainactivity
            Intent templateIntent = new Intent(context, MainActivity.class);
            templateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent templatePendingIntent = PendingIntent.getActivity(context, 0, templateIntent, 0);
            views.setPendingIntentTemplate(R.id.widget_scores_list, templatePendingIntent);

            // set the view to show when we have no fixtures
            views.setEmptyView(R.id.widget_scores_list, R.id.messageNoData);

            // update the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        context.startService(new Intent(context, LatestGameWidgetService.class));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ScoresProvider.ACTION_DATA_UPDATED.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, getClass()));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_scores_list);
        }
    }

    /**
     * Sets the remote adapter used to fill in the list items
     * @param views RemoteViews to set the RemoteAdapter
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setRemoteAdapter(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(R.id.widget_scores_list, new Intent(context, TodayGamesWidgetRemoteViewsService.class));
    }

    /**
     * Sets the remote adapter used to fill in the list items
     * @param views RemoteViews to set the RemoteAdapter
     */
    @SuppressWarnings("deprecation")
    private void setRemoteAdapterV11(int widgetId, Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(widgetId, R.id.widget_scores_list, new Intent(context, TodayGamesWidgetRemoteViewsService.class));
    }
}
