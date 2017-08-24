package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

public final class QuoteSyncJob {

    private static final int ONE_OFF_ID = 2;
    public static final String ACTION_DATA_UPDATED = "com.udacity.stockhawk.ACTION_DATA_UPDATED";
    //private static final int PERIOD = 300000;
    private static final int PERIOD = 60000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    //private static final int YEARS_OF_HISTORY = 2;
    private static final int YEARS_OF_HISTORY = 10;

    public static Map<String, Stock> quotes;

    private QuoteSyncJob() {
    }

    static void getQuotes(final Context context) {

        Timber.d("Running sync job");

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);

        try {

            Set<String> stockPref = PrefUtils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            Timber.d(stockCopy.toString());

            if (stockArray.length == 0) {
                return;
            }

            quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            Timber.d(quotes.toString());

            ArrayList<ContentValues> quoteCVs = new ArrayList<>();

            while (iterator.hasNext()) {
                final String symbol = iterator.next();
                Stock stock = quotes.get(symbol);

                //We test if the stock is null. If it is null, we will trigger a toast to say that stock is invalid.
                if (stock == null || stock.getName() == null) {
                    PrefUtils.removeStock(context, symbol);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context,"Invalid Stock: " + symbol , Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
                }

                else{

                    StockQuote quote = stock.getQuote();

                    try {
                        String companyName = stock.getName();
                        float price = quote.getPrice().floatValue();
                        float change = quote.getChange().floatValue();
                        float percentChange = quote.getChangeInPercent().floatValue();

                        float bid;
                        if (quote.getBid() == null) {
                            bid = 0.0f;
                        } else {
                            bid = quote.getBid().floatValue();
                        }

                        float ask;
                        if (quote.getAsk() == null) {
                            ask = 0.0f;
                        } else {
                            ask = quote.getAsk().floatValue();
                        }

                        float open = quote.getOpen().floatValue();
                        float previousClose = quote.getPreviousClose().floatValue();
                        float dayLow = quote.getDayLow().floatValue();
                        float dayHigh = quote.getDayHigh().floatValue();
                        float yearLow = quote.getYearLow().floatValue();
                        float yearHigh = quote.getYearHigh().floatValue();
                        float marketCap = stock.getStats().getMarketCap().floatValue();


                        List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);

                        StringBuilder historyBuilder = new StringBuilder();

                        for (HistoricalQuote it : history) {
                            historyBuilder.append(it.getDate().getTimeInMillis());
                            historyBuilder.append(", ");
                            historyBuilder.append(it.getClose());
                            historyBuilder.append("\n");
                        }

                        ContentValues quoteCV = new ContentValues();
                        quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                        quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                        quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                        quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);
                        quoteCV.put(Contract.Quote.COLUMN_HISTORY, historyBuilder.toString());

                        // Add the new fields to the contentValues.
                        quoteCV.put(Contract.Quote.COLUMN_COMPANY_NAME, companyName);
                        quoteCV.put(Contract.Quote.COLUMN_BID, bid);
                        quoteCV.put(Contract.Quote.COLUMN_ASK, ask);
                        quoteCV.put(Contract.Quote.COLUMN_OPEN, open);
                        quoteCV.put(Contract.Quote.COLUMN_PREVIOUS_CLOSE, previousClose);
                        quoteCV.put(Contract.Quote.COLUMN_DAY_LOW, dayLow);
                        quoteCV.put(Contract.Quote.COLUMN_DAY_HIGH, dayHigh);
                        quoteCV.put(Contract.Quote.COLUMN_YEAR_LOW, yearLow);
                        quoteCV.put(Contract.Quote.COLUMN_YEAR_HIGH, yearHigh);
                        quoteCV.put(Contract.Quote.COLUMN_MARKET_CAP, marketCap);
                        quoteCVs.add(quoteCV);

                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                    context.getContentResolver()
                            .bulkInsert(
                                    Contract.Quote.URI,
                                    quoteCVs.toArray(new ContentValues[quoteCVs.size()]));

                    Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
                    context.sendBroadcast(dataUpdatedIntent);
                }
            }

        } catch (IOException exception) {
            Timber.e(exception, "Error fetching stock quotes");
        }
    }

    private static void schedulePeriodic(Context context) {
        Timber.d("Scheduling a periodic task");


        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));


        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        scheduler.schedule(builder.build());
    }


    public static synchronized void initialize(final Context context) {

        schedulePeriodic(context);
        syncImmediately(context);

    }

    public static synchronized void syncImmediately(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
        } else {

            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));


            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            scheduler.schedule(builder.build());

        }
    }

}
