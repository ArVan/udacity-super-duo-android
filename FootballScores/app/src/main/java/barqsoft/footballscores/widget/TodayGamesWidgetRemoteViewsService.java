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
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

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
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TodayGamesWidgetRemoteViewsService extends RemoteViewsService {

    /**
     * Create the TodaysFixturesRemoteViewsFactory
     * @param intent Intent
     * @return TodaysFixturesRemoteViewsFactory
     */
    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TodayGamesRemoteViewsFactory(this.getApplicationContext());
    }
}
