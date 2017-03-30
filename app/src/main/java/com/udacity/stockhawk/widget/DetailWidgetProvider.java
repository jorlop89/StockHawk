package com.udacity.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.ui.MainActivity;

/**
 * Created by Portatil on 25/03/2017.
 */

public class DetailWidgetProvider extends AppWidgetProvider{

    public static final String EXTRA_SYMBOL = "symbol";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for(int appWidgetId : appWidgetIds){
            Intent intent = new Intent(context, MainActivity.class);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),R.layout.widget_initial);

            Intent widgetIntent = new Intent(context, DetailWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            context.startService(widgetIntent);

            remoteViews.setRemoteAdapter(R.id.list, widgetIntent);

            PendingIntent pendingIntent =  PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setOnClickPendingIntent(R.id.container, pendingIntent);
            remoteViews.setPendingIntentTemplate(R.id.container, pendingIntent);

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId,R.id.list);

            appWidgetManager.updateAppWidget(appWidgetId,remoteViews);

        }
    }
}
