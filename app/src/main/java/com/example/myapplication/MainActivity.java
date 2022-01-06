package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import br.com.aditum.tef.v1.enums.PaymentType;
import br.com.aditum.tef.v1.model.CancelationResponse;
import br.com.aditum.tef.v1.model.ConfirmationResponse;
import br.com.aditum.tef.v1.model.InitRequest;
import br.com.aditum.tef.AditumTefApi;
import br.com.aditum.tef.v1.model.InitResponse;
import br.com.aditum.tef.v1.model.PaymentRequest;
import br.com.aditum.tef.v1.model.PaymentResponse;

public class MainActivity extends AppCompatActivity {

    private AditumTefApi tefApi = null;
    private String _lastPaymentNsu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText activationCodeEditText = findViewById(R.id.activationCodeEditText);
        final TextView notificationTextView = findViewById(R.id.notificationTextView);
        final Button initButton = findViewById(R.id.initButton);
        final Button payButton = findViewById(R.id.payButton);
        final Button revertButton = findViewById(R.id.revertButton);
        final Button confirmButton = findViewById(R.id.confirmButton);
        final Button cancelButton = findViewById(R.id.cancelButton);
        final Button abortButton = findViewById(R.id.abortButton);

        payButton.setEnabled(false);
        revertButton.setEnabled(false);
        confirmButton.setEnabled(false);
        cancelButton.setEnabled(false);
        abortButton.setEnabled(false);

        tefApi = new AditumTefApi(this, new AditumTefApi.PPCompCallbacks() {
            @Override
            public void onNotification(String s) {
                Log.d("ON_NOTIFICATION", s);
                notificationTextView.setText(s);
            }
        });

        // Init
        initButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                notificationTextView.setText("");
                initButton.setEnabled(false);
                InitRequest initRequest = new InitRequest();
                initRequest.setApplicationToken("1234567890");
                initRequest.setApplicationName("MyApplication");
                initRequest.setApplicationVersion("1.0.0");
                initRequest.setActivationCode(activationCodeEditText.getText().toString());

                tefApi.init(initRequest, new AditumTefApi.IResponseCallback<InitResponse>() {
                    @Override
                    public void onFinished(InitResponse initResponse) {
                        if(initResponse.getSuccess()) {
                            payButton.setEnabled(true);
                            abortButton.setEnabled(true);
                        } else {
                            initButton.setEnabled(true);
                        }
                        notificationTextView.setText("");
                    }
                });
            }
        });

        // Pay
        payButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                notificationTextView.setText("");
                payButton.setEnabled(false);
                revertButton.setEnabled(false);
                confirmButton.setEnabled(false);

                PaymentRequest payRequest = new PaymentRequest();
                payRequest.setAmount((long)1000);
                payRequest.setPaymentType(PaymentType.CREDIT);

                tefApi.pay(payRequest, new AditumTefApi.IResponseCallback<PaymentResponse>() {
                    @Override
                    public void onFinished(PaymentResponse paymentResponse) {
                        if(paymentResponse.getIsApproved()) {
                            revertButton.setEnabled(true);
                            confirmButton.setEnabled(true);

                            _lastPaymentNsu = paymentResponse.getCharge().getNsu();
                            Log.d("VIA LOGISTA", String.join("\n", paymentResponse.getCharge().getMerchantReceipt()));
                            Log.d("VIA CLIENTE", String.join("\n", paymentResponse.getCharge().getCardholderReceipt()));
                        } else {
                            payButton.setEnabled(true);
                        }
                        notificationTextView.setText("");
                    }
                });
            }
        });

        // Revert
        revertButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                notificationTextView.setText("");
                confirmButton.setEnabled(false);
                revertButton.setEnabled(false);

                tefApi.revert(_lastPaymentNsu, new AditumTefApi.IResponseCallback<CancelationResponse>() {
                    @Override
                    public void onFinished(CancelationResponse cancelationResponse) {
                        if(!cancelationResponse.getSuccess()) {
                            revertButton.setEnabled(true);
                        } else {
                            payButton.setEnabled(true);
                            _lastPaymentNsu = null;
                        }
                        notificationTextView.setText("");
                    }
                });
            }
        });

        // Confirm
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                notificationTextView.setText("");
                revertButton.setEnabled(false);
                confirmButton.setEnabled(false);
                tefApi.confirm(_lastPaymentNsu, new AditumTefApi.IResponseCallback<ConfirmationResponse>() {
                    @Override
                    public void onFinished(ConfirmationResponse confirmationResponse) {
                        if(!confirmationResponse.getSuccess()) {
                            confirmButton.setEnabled(true);
                        } else {
                            // Transaction can
                            payButton.setEnabled(true);
                            cancelButton.setEnabled(true);
                        }
                        notificationTextView.setText("");
                    }
                });
            }
        });

        // Cancel
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                notificationTextView.setText("");
                cancelButton.setEnabled(false);
                tefApi.cancel(_lastPaymentNsu, new AditumTefApi.IResponseCallback<CancelationResponse>() {
                    @Override
                    public void onFinished(CancelationResponse cancelationResponse) {
                        if(cancelationResponse.getCanceled()) {
                            revertButton.setEnabled(false);
                            confirmButton.setEnabled(false);
                            cancelButton.setEnabled(false);

                            Log.d("VIA LOGISTA", String.join("\n", cancelationResponse.getCharge().getMerchantReceipt()));
                            Log.d("VIA CLIENTE", String.join("\n", cancelationResponse.getCharge().getCardholderReceipt()));
                            _lastPaymentNsu = null;
                        } else {
                            cancelButton.setEnabled(true);
                        }
                        notificationTextView.setText("");
                    }
                });
            }
        });

        // Abort
        abortButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tefApi.abort();
            }
        });
    }
}