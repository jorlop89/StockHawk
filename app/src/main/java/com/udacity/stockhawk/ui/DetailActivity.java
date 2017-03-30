package com.udacity.stockhawk.ui;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.widget.DetailWidgetProvider;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import au.com.bytecode.opencsv.CSVReader;
import butterknife.BindView;
import butterknife.ButterKnife;
import yahoofinance.histquotes.HistoricalQuote;

/**
 * Created by Portatil on 19/03/2017.
 */

public class DetailActivity extends AppCompatActivity {



    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.linechart)
    LineChart lineChart;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.stock_detail_container)
    LinearLayout stockDetailContainer;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.txtname)
    public TextView tvname;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.txtprice)
    public TextView tvprice;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.txtbid)
    public TextView tvbid;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.txtask)
    public TextView tvask;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.txtopen)
    public TextView tvopen;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.txtpreviousClose)
    public TextView tvpreviousclose;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.txtdayLow)
    public TextView tvdaylow;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.txtdayHigh)
    public TextView tvdayhigh;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.txtYearLow)
    public TextView tvyearlow;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.txtYearHigh)
    public TextView tvyearhigh;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.txtmarketcap)
    public TextView tvMarketcap;

    public List<HistoricalQuote> history;
    public String name;

    public float price;
    public float ask;
    public float bid;
    public float open;
    public float previousClose;
    public float dayLow;
    public float dayHigh;
    public float yearLow;
    public float yearHigh;
    public float marketCap;

    LineData lineData;
    LineDataSet closeValues;

    //public static final String EXTRA_SYMBOL = "symbol";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_activity);
        ButterKnife.bind(this);

        /* Change the title in the action bar of the detail activity */
        String symbol = getIntent().getStringExtra(DetailWidgetProvider.EXTRA_SYMBOL);
        setTitle(symbol);

        getHistory(symbol);
        getStockData(symbol);
        showStockData();


    }

    private void showStockData() {
        tvname.setText(name);
        tvprice.setText(String.valueOf(price));
        tvask.setText(String.valueOf(ask));
        tvbid.setText(String.valueOf(bid));
        tvopen.setText(String.valueOf(open));
        tvpreviousclose.setText(String.valueOf(previousClose));
        tvdaylow.setText(String.valueOf(dayLow));
        tvdayhigh.setText(String.valueOf(dayHigh));
        tvyearlow.setText(String.valueOf(yearLow));
        tvyearhigh.setText(String.valueOf(yearHigh));
        tvMarketcap.setText(String.format("%.2f", marketCap) + " $");
    }

    private void getStockData(String symbol) {
        Cursor cursor = getContentResolver().query(Contract.Quote.makeUriForStock(symbol), null, null, null, null);
        if (cursor.moveToFirst()) {

            name = cursor.getString(cursor.getColumnIndex(Contract.Quote.COLUMN_COMPANY_NAME));
            price = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_PRICE));
            ask = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_ASK));
            bid = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_BID));
            open = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_OPEN));
            previousClose = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_PREVIOUS_CLOSE));
            dayLow = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_DAY_LOW));
            dayHigh = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_DAY_HIGH));
            yearLow = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_YEAR_LOW));
            yearHigh = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_YEAR_HIGH));
            marketCap = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_MARKET_CAP));

        }
    }


    private void getHistory(String symbol){

        String history = getHistoryString(symbol);

        List<String[]> lines = getLines(history);

        List<Long> dates = new ArrayList<>();

        List<Entry> entries = new ArrayList<>();

        int position = 0;

        Collections.reverse(lines);

        for(String[] line: lines){
            dates.add(Long.valueOf(line[0]));
            Entry entry = new Entry(position, Float.valueOf(line[1]));
            entries.add(entry);
            position++;
        }

        setupChart(symbol, dates, entries);
    }

    private String getHistoryString(String symbol) {
        Cursor cursor = getContentResolver().query(Contract.Quote.makeUriForStock(symbol), null, null, null, null);

        String history = "";
        if (cursor.moveToFirst()) {
            history = cursor.getString(cursor.getColumnIndex(Contract.Quote.COLUMN_HISTORY));
            cursor.close();
        }

        return history;
    }

    private List<String[]> getLines(String history){
        CSVReader reader = new CSVReader(new StringReader(history));
        List<String[]> lines = null;
        try{
            lines = reader.readAll();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return lines;
    }

    private void setupChart(String symbol, final List<Long> dates, List<Entry> entries) {
        Legend legend = lineChart.getLegend();
        legend.setTextColor(Color.WHITE);

        lineChart.animateX(3000, Easing.EasingOption.Linear);

        closeValues = new LineDataSet(entries, symbol);
        closeValues.setAxisDependency(YAxis.AxisDependency.LEFT);

        closeValues.setColors(ColorTemplate.VORDIPLOM_COLORS);
        closeValues.setValueTextColor(Color.WHITE);

        lineData = new LineData(closeValues);
        lineData.setValueFormatter(new MyValueFormatter());
        lineChart.setData(lineData);
        lineChart.invalidate();

        IAxisValueFormatter formatter = new IAxisValueFormatter() {

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                Date date = new Date(dates.get((int)value));
                return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(date);

            }
        };

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(formatter);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setTextColor(Color.WHITE);
    }
}
