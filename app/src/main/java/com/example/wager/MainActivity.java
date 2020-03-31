/* Copyright (C) 2020 wAgEr: Vijay Lakshminarayanan - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL license.
 * Feel free to use the code or samples but please mention my name
 * Donate us a coffee or beer if you find it useful :)
 *
 */



package com.example.wager;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.anychart.APIlib;
import com.anychart.charts.Pie;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Line;
import com.anychart.data.Mapping;
import com.anychart.data.Set;
import com.anychart.enums.Anchor;
import com.anychart.enums.MarkerType;
import com.anychart.enums.TooltipPositionMode;
import com.anychart.graphics.vector.Stroke;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.Provider;
import java.security.Security;
import java.sql.Wrapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import kotlin.collections.unsigned.UArraysKt;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;



public class MainActivity extends AppCompatActivity {

    private final String privateKey = "";
    private final String wagerContractAddress = "0x7F2991f700832B065B8cEE9F3226957f8c04e595";
    private final OkHttpClient client = new OkHttpClient();
    Web3j web3 = Web3j.build(new HttpService("https://ropsten.infura.io/v3/5584712aa62f4de1b5063fe58bbf27eb"));
    //    Credentials credentials = Credentials.create(privateKey);
    Credentials credentials;
    //    WagerContract wagerContract = WagerContract.load(wagerContractAddress, web3, credentials, BigInteger.valueOf(1_000_000), BigInteger.valueOf(1_000_000));
    WagerContract wagerContract;
    File walletPath;
    String fileName = "keystore.json";

    TextView accountAddress;
    TextView accountBalance;
    TextView riskLevelText;
    TextView frequencyLevelText;
    TextView amountToLendText;
    Button createLoan;
    SeekBar riskBar;
    SeekBar amountBar;
    SeekBar durationBar;
    int amountToLendLevel;
    int riskLevel;
    int frequencyLevel;
    BigInteger mainAccountBalance;
    BigInteger amountToLendInEth;
    ProgressDialog progressDialog;
    int historicalLength = 60;
    int rankTokens = 5;
    double[] btcPrice = new double[historicalLength];
    double[] ethPrice = new double[historicalLength];
    double[] tokenPercent = new double[rankTokens];
    String[] tokenList = new String[rankTokens];
    AnyChartView lineChart,pieChartView;
    Cartesian cartesian1;
    Pie pieChart;
    ArrayList<DataEntry> ethSeries = new ArrayList<>();
    ArrayList<DataEntry> pieData = new ArrayList<>();
    Map<String, Double> wagerResultTable = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBouncyCastle(); // set crypto provider

        // allow strict mode
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_main);

        // Test views
        accountAddress = findViewById(R.id.textViewAccountAddress);
        accountBalance = findViewById(R.id.textViewAccountBalance);
        riskLevelText = findViewById(R.id.textViewRiskLevel);
        frequencyLevelText = findViewById(R.id.textViewDuration);
        amountToLendText = findViewById(R.id.textViewLoanAmount);

        // buttons
        createLoan = findViewById(R.id.buttonCreateWager);

        // Bars
        riskBar = findViewById(R.id.riskBar);
        riskLevel = riskBar.getProgress()*10;
        if(riskLevel == 0)
            riskLevel = 10;
        amountBar = findViewById(R.id.amountBar);
        amountToLendLevel = amountBar.getProgress() * 10;
        if(amountToLendLevel == 0)
            amountToLendLevel = 10;
        durationBar = findViewById(R.id.durationBar);
        frequencyLevel = durationBar.getProgress();
        if(frequencyLevel == 0)
            frequencyLevel = durationBar.getProgress();

        //Views
        lineChart = findViewById(R.id.lineChart1);
        APIlib.getInstance().setActiveAnyChartView(lineChart);
        lineChart.setProgressBar(findViewById(R.id.progress_bar1));
        cartesian1 = AnyChart.line();

        //Initialize progress bars
        riskLevelText.setText("Risk Level: " + riskLevel + "%");
        amountToLendText.setText("Amount to lend: " + amountToLendLevel + " % Wallet");
        frequencyLevelText.setText("Loan Duration:" + frequencyLevel* 6 + "days");

        durationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress == 0)
                    progress = 1;
                frequencyLevelText.setText("Loan Duration: " + progress* 6 +"days");
                frequencyLevel = progress*6;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Update historical data
                lineChart.clear();
                lineChart = findViewById(R.id.lineChart1);
                APIlib.getInstance().setActiveAnyChartView(lineChart);
                lineChart.setProgressBar(findViewById(R.id.progress_bar1));
                ethSeries.clear();
                for(int i=0;i<frequencyLevel;i++) {
                    ethSeries.add(new CustomDataEntry(i, ethPrice[i]));
                }
                cartesian1.data(ethSeries);
                lineChart.setChart(cartesian1);

                try {
                    wagerResultTable.clear();
                    getML(riskLevel, frequencyLevel);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Get risk assessment
                pieChartView.clear();
                APIlib.getInstance().setActiveAnyChartView(pieChartView);
                pieData.clear();
                for (int i =0; i < rankTokens-1; i++)
                    pieData.add(new PieDataEntry(tokenList[i],tokenPercent[i]));
                pieChart.data(pieData);
                pieChartView.setChart(pieChart);

            }
        });

        riskBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress ==0)
                    progress=1;
                riskLevelText.setText("Risk Level: " + progress*10+"%");
                riskLevel = progress*10;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                try {
                    wagerResultTable.clear();
                    getML(riskLevel, frequencyLevel);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Get risk assessment
                pieChartView.clear();
                APIlib.getInstance().setActiveAnyChartView(pieChartView);
                pieData.clear();
                   for (int i =0; i < rankTokens-1; i++)
                        pieData.add(new PieDataEntry(tokenList[i],tokenPercent[i]));
                pieChart.data(pieData);
                pieChartView.setChart(pieChart);

            }
        });

        amountBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress == 0)
                    progress = 1;
                amountToLendText.setText("Amount to lend: " + progress * 10 + " % Wallet");
                amountToLendLevel = progress * 10;

                amountToLendInEth = mainAccountBalance.divide(BigInteger.valueOf(100)).multiply(BigInteger.valueOf(amountToLendLevel));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {


            }
        });

        try {
            walletPath = this.getFilesDir();
            File f = new File(walletPath + "/" + fileName);
            if (f.exists() && !f.isDirectory()) {
                credentials = WalletUtils.loadCredentials("password", walletPath + "/" + fileName);
            } else {
                String generatedName = WalletUtils.generateLightNewWalletFile("password", walletPath);
                File generatedFile = new File(walletPath, generatedName);
                File keystore = new File(walletPath, fileName);
                generatedFile.renameTo(keystore);
                credentials = WalletUtils.loadCredentials("password", walletPath + "/" + fileName);
            }
            accountAddress.setText(credentials.getAddress());
            EthGetBalance balanceWei = web3.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();
            BigDecimal balanceInEther = Convert.fromWei(balanceWei.getBalance().toString(), Convert.Unit.ETHER);
            mainAccountBalance = balanceWei.getBalance();
            if (mainAccountBalance.compareTo(BigInteger.ZERO) > 0) {
                accountBalance.setText("ETH balance: " + balanceInEther.toPlainString());
            } else {
                accountBalance.setText("Please top up your balance");
            }
            amountToLendInEth = mainAccountBalance.divide(BigInteger.valueOf(100)).multiply(BigInteger.valueOf(amountToLendLevel));
            //Get historical data from Amber Data
         //   btcPrice=getHistoricalData("https://web3api.io/api/v2/market/prices/btc_usd/historical","UAK42f1c35cfd5527c86e5dc9df987af10f","btc_usd");
            ethPrice=getHistoricalData("https://web3api.io/api/v2/market/prices/eth_usd/historical","UAK42f1c35cfd5527c86e5dc9df987af10f","eth_usd");
           //draw first graph of bitcoin, eth prices
            for (int i =0; i< 5; i++) {
                tokenList[i] = "ETH";
                tokenPercent[i] = 20;
            }
            drawGraph(btcPrice.length,ethPrice);
            drawPieChart();
        } catch (Exception e) {
            e.printStackTrace();
        }

        accountAddress.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("address", accountAddress.getText());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getApplication(), "Address copied!", Toast.LENGTH_SHORT).show();
        });

        createLoan.setOnClickListener(v -> {
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setTitle("Please wait");
            progressDialog.setMessage("Waiting for transaction status...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            new Background().execute();
        });

    }


    private void setupBouncyCastle() {
        final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            // Web3j will set up the provider lazily when it's first used.
            return;
        }
        if (provider.getClass().equals(BouncyCastleProvider.class)) {
            // BC with same package name, shouldn't happen in real life.
            return;
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    void sendTx() {
        new Thread(() -> {
            TransactionReceipt receipt = null;
            try {
                wagerContract = WagerContract.load(wagerContractAddress, web3, credentials, BigInteger.valueOf(1_000_000), BigInteger.valueOf(1_000_000));
                receipt = wagerContract.createWager(credentials.getAddress(), amountToLendInEth, amountToLendInEth).send();
            } catch (Exception e) {
                e.printStackTrace();
            }
            progressDialog.cancel();
            assert receipt != null;
            Looper.prepare();
            Toast.makeText(MainActivity.this, "Tx Receipt complete: " + receipt.getTransactionHash(), Toast.LENGTH_LONG).show();
            updateBalance();
            Looper.loop();
        }).start();
    }

    void updateBalance() {
        EthGetBalance balanceWei = null;
        try {
            balanceWei = web3.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        BigDecimal balanceInEther = Convert.fromWei(balanceWei.getBalance().toString(), Convert.Unit.ETHER);
        if (mainAccountBalance.intValue() > 0) {
            System.out.println(mainAccountBalance.toString());
            accountBalance.setText("ETH balance: " + balanceInEther.toPlainString());
        } else {
            accountBalance.setText("Please top up your balance");
        }
        System.out.println("Updated balance " + balanceInEther.toPlainString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.bashboard) {
            Intent intent = new Intent(this, Dashboard.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void getML(int risk, int duration)throws Exception {
        Request request = new Request.Builder()
                .url("http://209.97.177.54/wager_function?risk=" + risk + "&duration=" + duration)
                .build();
        System.out.println(request.url().toString());

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            Gson gson = new Gson();
            Type assetMapType = new TypeToken<Map<String, Double>>() {
            }.getType();
            Map<String, Double> stringDoubleMap = gson.fromJson(response.body().charStream(), assetMapType);

            for (Map.Entry<String, Double> entry : stringDoubleMap.entrySet()) {
                if (!entry.getKey().equals("BTC")) {
                    wagerResultTable.put(entry.getKey(), entry.getValue());
                }
            }
        }
        int i=0;
        for (Map.Entry<String, Double> entry : wagerResultTable.entrySet()) {
            tokenList[i] = entry.getKey();
            tokenPercent[i] = entry.getValue();
            System.out.println(tokenList[i]+ "/" + tokenPercent[i]);
            i=i+1;
        }
    }

    //Get Historical data from Amberdata
    //Price chart to show the users how their token is performing over a certain duration
    public double[] getHistoricalData(String url, String apikey,String currencykey)
            throws IOException {
        int i = 0;
        double[] price = new double[historicalLength];
        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("x-api-key", apikey)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            Gson gson = new Gson();
           Map response1 = gson.fromJson(response.body().string(), Map.class);
           Map response2 = gson.fromJson(response1.get("payload").toString(),Map.class);
           JsonArray objectValues  = gson.fromJson(response2.get(currencykey).toString(),JsonArray.class);

            Iterator <JsonElement> iter = objectValues.iterator();
            //Iterating the contents of the array
            while(iter.hasNext()) {
                Map list1 = gson.fromJson(iter.next().toString(),Map.class);
                if((list1.get("price")!= null)&&(i<historicalLength)) {
                    price[i] = (double) list1.get("price");
                    i++;
                }
            }
            }
        return price;
        }


    public void drawGraph(int points, double[] eth){
        //This will give an overview of how the crypto-currency tokens are doing before user "wagers" some cash
        //Code not refactored, possible improvements in using Linechart with multiple axes
        //Scaling across different current currency ranges needs to be considered
        lineChart = findViewById(R.id.lineChart1);
        APIlib.getInstance().setActiveAnyChartView(lineChart);
        lineChart.setProgressBar(findViewById(R.id.progress_bar1));
        ethSeries.clear();
        for(int i=0;i<points;i++) {
            ethSeries.add(new CustomDataEntry(i, eth[i]));
        }
        Line series1 = cartesian1.line(ethSeries);
        cartesian1.data(ethSeries);
        cartesian1.animation(true);
        cartesian1.padding(2d, 5d, 1d, 5d);
        cartesian1.crosshair().enabled(true);
        cartesian1.crosshair()
                .yLabel(true)
                .yStroke((Stroke) null, null, null, (String) null, (String) null);
        cartesian1.tooltip().positionMode(TooltipPositionMode.POINT);
        cartesian1.yAxis(0).title("USD Price");
        cartesian1.xAxis(0).title("Days");
        cartesian1.xAxis(0).labels().padding(1d, 1d, 1d, 1d);

        series1.hovered().markers().enabled(true);
        series1.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        series1.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);
        cartesian1.title("ETH Price history");
        cartesian1.title().fontSize(11d);
        cartesian1.title().padding(0d, 0d, 2d, 0d);
        lineChart.setChart(cartesian1);
    }

    public void drawPieChart(){
        //This will give an overview of the token basket chosen for the various risk profiles
        pieChartView = findViewById(R.id.pieChart1);
        APIlib.getInstance().setActiveAnyChartView(pieChartView);
        pieChart = AnyChart.pie();
          for (int i =0; i < rankTokens; i++)
              pieData.add(new PieDataEntry(tokenList[i],tokenPercent[i]));

        pieChart.title("Token basket");
        pieChart.title().fontColor("BLUE");
        pieChart.title().fontSize(11d);
        pieChart.hovered().markers().enabled(true);
        pieChart.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        pieChart.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);
        pieChart.animation(true);
        pieChart.data(pieData);
        pieChartView.setChart(pieChart);
    }
    //Data entry method for the line chart
    public class CustomDataEntry extends ValueDataEntry {
        CustomDataEntry(Integer x, Double value) {
            super(x,value);
            setValue("x", x);
            setValue("value", value);
        }
    }

    //Data entry method for the pie chart
    public class PieDataEntry extends ValueDataEntry {
        PieDataEntry(String token,double x) {
            super(token, x);
            setValue("token",token);
        }
    }


    class Background extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            sendTx();
            return null;
        }
    }
}


