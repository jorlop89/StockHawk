package com.udacity.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.ui.DetailActivity;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.udacity.stockhawk.widget.DetailWidgetProvider.EXTRA_SYMBOL;

/**
 * Created by Portatil on 26/03/2017.
 */

public class DetailWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new DetailStockWidgetViewFactory(getApplicationContext(), intent);
    }

    private class DetailStockWidgetViewFactory implements RemoteViewsFactory {

        private final Context mApplicationContext;
        private List<ContentValues> mCvList = new ArrayList<>();
        private final DecimalFormat dollarFormatWithPlus;
        private final DecimalFormat dollarFormat;
        private final DecimalFormat percentageFormat;
        private int mWidgetId;
        private Cursor mCursor;


        public DetailStockWidgetViewFactory(Context context, Intent intent){
            mApplicationContext = context;
            dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
            dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
            dollarFormatWithPlus.setPositivePrefix("+$");
            percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
            percentageFormat.setMaximumFractionDigits(2);
            percentageFormat.setMinimumFractionDigits(2);
            percentageFormat.setPositivePrefix("+");
            mWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

        }

        @Override
        public void onCreate() {
        }

        private void getData(){
            mCvList.clear();

            long identity = Binder.clearCallingIdentity();
            try {
                ContentResolver contentResolver = mApplicationContext.getContentResolver();
                mCursor = contentResolver.query(Contract.Quote.URI, null, null, null, null);

                while (mCursor.moveToNext()) {
                    String symbol = mCursor.getString(mCursor.getColumnIndex(Contract.Quote.COLUMN_SYMBOL));
                    float price = mCursor.getFloat(mCursor.getColumnIndex(Contract.Quote.COLUMN_PRICE));
                    float absChange = mCursor.getFloat(mCursor.getColumnIndex(Contract.Quote.COLUMN_ABSOLUTE_CHANGE));
                    float percentChange = mCursor.getFloat(mCursor.getColumnIndex(Contract.Quote.COLUMN_PERCENTAGE_CHANGE));

                    ContentValues cv = new ContentValues();
                    cv.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                    cv.put(Contract.Quote.COLUMN_PRICE, price);
                    cv.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, absChange);
                    cv.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);

                    mCvList.add(cv);

                }

                mCursor.close();
            }
            finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void onDataSetChanged() {
            getData();
        }

        @Override
        public void onDestroy() {

        }

        @Override
        public int getCount() {
            return mCvList.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            ContentValues cv = mCvList.get(position);
            RemoteViews remoteViews = new RemoteViews(mApplicationContext.getPackageName(),R.layout.list_item_quote);

            String symbol = cv.getAsString(Contract.Quote.COLUMN_SYMBOL);
            remoteViews.setTextViewText(R.id.symbol, symbol);
            remoteViews.setTextViewText(R.id.price, dollarFormat.format(cv.getAsFloat(Contract.Quote.COLUMN_PRICE)));

            float absChange = cv.getAsFloat(Contract.Quote.COLUMN_ABSOLUTE_CHANGE);
            float perChange = cv.getAsFloat(Contract.Quote.COLUMN_PERCENTAGE_CHANGE);

            if(absChange > 0){
                remoteViews.setInt(R.id.change,"setBackgroundResource",R.drawable.percent_change_pill_green);
            }
            else{
                remoteViews.setInt(R.id.change,"setBackgroundResource",R.drawable.percent_change_pill_red);
            }

            remoteViews.setTextViewText(R.id.change, percentageFormat.format(perChange / 100));

            Intent fillIntent = new Intent(getApplicationContext(), DetailActivity.class);
            Bundle extras = new Bundle();
            extras.putString(EXTRA_SYMBOL, symbol);
            fillIntent.putExtras(extras);

            PendingIntent pendingIntent =  PendingIntent.getActivity(mApplicationContext, 0, fillIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.listitem, pendingIntent);
            //remoteViews.setOnClickFillInIntent(R.id.listitem,fillIntent);

            return remoteViews;
        }

        @Override
        public RemoteViews getLoadingView() {
            return new RemoteViews(mApplicationContext.getPackageName(), R.layout.list_item_quote);
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
