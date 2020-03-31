package com.example.wager;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Pie;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Dashboard extends AppCompatActivity {

    private final String wagerContractAddress = "0x7F2991f700832B065B8cEE9F3226957f8c04e595";
    //    private final String privateKey = "";
    File walletPath;
    String fileName = "keystore.json";
    Web3j web3 = Web3j.build(new HttpService("https://ropsten.infura.io/v3/18bdd762e4b84f1c85479b76ae563634"));
    //    Credentials credentials = Credentials.create(privateKey);
    Credentials credentials;
    //    WagerContract wagerContract = WagerContract.load(wagerContractAddress, web3, credentials, BigInteger.valueOf(1_000_000), BigInteger.valueOf(1_000_000));
    WagerContract wagerContract;
    TextView networkCapacityText;
    TextView votingPowerText;
    TextView estimatedIRText;
    ProgressDialog progressDialog;
    Button withdrawAll;
    Handler handler = new Handler();
    Runnable runnable;
    AtomicLong totalSupply = new AtomicLong();
    AtomicLong myVotingPower = new AtomicLong();
    AtomicInteger apxInterestRate = new AtomicInteger(10);
    AtomicBoolean isUpdating = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        networkCapacityText = findViewById(R.id.textViewNetwokCapacity);
        votingPowerText = findViewById(R.id.textViewMyVotingPower);
        estimatedIRText = findViewById(R.id.textViewEstimatedIR);

        estimatedIRText.setText("EIR: 0 %");

        withdrawAll = findViewById(R.id.buttonWithdrawAll);

        walletPath = this.getFilesDir();
        File f = new File(walletPath + "/" + fileName);
        if (f.exists() && !f.isDirectory()) {
            try {
                credentials = WalletUtils.loadCredentials("password", walletPath + "/" + fileName);
                wagerContract = WagerContract.load(wagerContractAddress, web3, credentials, BigInteger.valueOf(1_000_000), BigInteger.valueOf(1_000_000));
                updateData();
            } catch (IOException | CipherException e) {
                e.printStackTrace();
            }
        }


        withdrawAll.setOnClickListener(v -> {
            progressDialog = new ProgressDialog(Dashboard.this);
            progressDialog.setTitle("Processing...");
            progressDialog.setMessage("Waiting for transaction to be mined...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            isUpdating.set(true);
            try {
                new Background().execute().get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        });


        Pie pie = AnyChart.pie();
        List<DataEntry> data = new ArrayList<>();
        data.add(new ValueDataEntry("0KB",26 ));
        data.add(new ValueDataEntry("MKR", 24));
        data.add(new ValueDataEntry("LINK", 30));
        data.add(new ValueDataEntry("CRO", 20));

        pie.data(data);

        AnyChartView anyChartView = findViewById(R.id.any_chart_view);
        anyChartView.setChart(pie);

        runnable = () -> {
            if (!isUpdating.get()) {
                if (totalSupply.get() > 0) {
                    totalSupply.addAndGet(1618033);
                    networkCapacityText.setText("Total supply: " + totalSupply.get() + " WGRI");
                }
                if (myVotingPower.get() > 0) {
                    votingPowerText.setText("My Voting Power: " + myVotingPower.get() + " WGRI");
                    myVotingPower.addAndGet(1618033);
                }
            }
            handler.postDelayed(runnable, 500);
        };

        runnable.run();

    }

    void withdraw() {
        new Thread(() -> {
            TransactionReceipt receipt = null;
            try {
                receipt = wagerContract.withdrawAll().sendAsync().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            progressDialog.cancel();
            assert receipt != null;
            isUpdating.set(false);
            myVotingPower.set(0);
            votingPowerText.setText("My Voting Power: " + myVotingPower.get() + " WGRI");
            Looper.prepare();
            Toast.makeText(Dashboard.this, "Tx Receipt complete: " + receipt.getTransactionHash(), Toast.LENGTH_LONG).show();
            Looper.loop();
            updateData();
        }).start();
    }

    void updateData() {
        try {
            totalSupply.set(wagerContract.totalSupply().send().longValue());
            myVotingPower.set(wagerContract.myVotingPower(credentials.getAddress()).send().longValue());
            if (myVotingPower.get() == 0) {
                withdrawAll.setVisibility(View.INVISIBLE);
            } else {
                estimatedIRText.setText("EIR ~ " + apxInterestRate.get() + " %");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class Background extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            withdraw();
            updateData();
            return null;
        }
    }
}
