package com.example.ljd.socketserver;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Date;
import java.text.SimpleDateFormat;


import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @Bind(R.id.show_linear)
    LinearLayout mShowLinear;

    @Bind(R.id.msg_edit_text)
    EditText mMessageEditText;

    private ServerSocket mServerSocket;
    private Button mSendButton;
    private PrintWriter mPrintWriter;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0){
                TextView textView = new TextView(MainActivity.this);
                textView.setText((String)msg.obj);
                mShowLinear.addView(textView);
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mSendButton = (Button)findViewById(R.id.send_btn);
        try {
            mServerSocket = new ServerSocket(8688);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSendButton.setOnClickListener(MainActivity.this);
        new Thread(new AcceptClient()).start();
    }


    @Override
    public void onDestroy() {
        ButterKnife.unbind(this);
        if (mServerSocket != null){
            try {
                mServerSocket.close();
                mServerSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        final String msg = mMessageEditText.getText().toString();
        if (!TextUtils.isEmpty(msg) && mPrintWriter != null) {
            mPrintWriter.println(msg);
            mMessageEditText.setText("");
            String time = getTime(System.currentTimeMillis());
            final String showedMsg = "server " + time + ":" + msg;
            TextView textView = new TextView(this);
            textView.setText(showedMsg);
            mShowLinear.addView(textView);
        }
    }

    private String getTime(long time) {
        return new SimpleDateFormat("(HH:mm:ss)").format(new Date(time));
    }

    class AcceptClient implements Runnable{

        @Override
        public void run() {
            try {
                Socket clientSocket = null;
                while (clientSocket == null){
                    clientSocket = mServerSocket.accept();
                    mPrintWriter = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(clientSocket.getOutputStream())), true);
                }
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                        clientSocket.getInputStream()));
                while (!MainActivity.this.isFinishing()) {
                    String msg = bufferedReader.readLine();
                    if (msg != null) {
                        String time = getTime(System.currentTimeMillis());
                        final String showedMsg = "client " + time + ":" + msg;
                        mHandler.obtainMessage(0, showedMsg)
                                .sendToTarget();
                    }
                }
                bufferedReader.close();
                clientSocket.close();
                mPrintWriter.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}










