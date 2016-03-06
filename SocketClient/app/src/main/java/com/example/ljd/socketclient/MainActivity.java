package com.example.ljd.socketclient;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Date;
import java.text.SimpleDateFormat;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity{

    private static final int MESSAGE_RECEIVE_NEW_MSG = 1;
    private static final int MESSAGE_SOCKET_CONNECTED = 2;

    @Bind(R.id.msg_container)
    TextView mMessageTextView;

    @Bind(R.id.msg_edit_text)
    EditText mMessageEditText;

    private PrintWriter mPrintWriter;
    private Socket mClientSocket;
    private boolean mIsConnectServer = false;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MESSAGE_RECEIVE_NEW_MSG: {
                    mMessageTextView.setText(mMessageTextView.getText()
                            + (String) msg.obj);
                    break;
                }
                case MESSAGE_SOCKET_CONNECTED: {
                    Toast.makeText(MainActivity.this,"连接服务端成功",Toast.LENGTH_SHORT).show();
                }
                default:
                    break;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

    }

    @Override
    protected void onDestroy() {
        ButterKnife.unbind(this);
        disConnectServer();
        super.onDestroy();
    }

    @OnClick({R.id.send_btn,R.id.connect_btn,R.id.disconnect_btn})
    public void onClickButton(View v) {

        switch (v.getId()){
            case R.id.send_btn:
                sendMessageToServer();
                break;
            case R.id.connect_btn:
                new Thread() {
                    @Override
                    public void run() {
                        connectServer();
                    }
                }.start();
                break;
            case R.id.disconnect_btn:
                Toast.makeText(MainActivity.this,"已经断开连接",Toast.LENGTH_SHORT).show();
                disConnectServer();
                break;
        }

    }

    @SuppressLint("SimpleDateFormat")
    private String formatDateTime(long time) {
        return new SimpleDateFormat("(HH:mm:ss)").format(new Date(time));
    }

    private void connectServer() {

        if (mIsConnectServer)
            return;

        while (mClientSocket == null) {
            try {
                mClientSocket = new Socket("localhost", 8688);
                mPrintWriter = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(mClientSocket.getOutputStream())), true);
                mIsConnectServer = true;
                mHandler.obtainMessage(MESSAGE_SOCKET_CONNECTED).sendToTarget();
            } catch (IOException e) {
                SystemClock.sleep(1000);
            }
        }

        try {
            // 接收服务器端的消息
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    mClientSocket.getInputStream()));
            while (!MainActivity.this.isFinishing()) {
                String msg = br.readLine();
                if (msg != null) {
                    String time = formatDateTime(System.currentTimeMillis());
                    final String showedMsg = "server " + time + ":" + msg
                            + "\n";
                    mHandler.obtainMessage(MESSAGE_RECEIVE_NEW_MSG, showedMsg)
                            .sendToTarget();
                }
            }
            mPrintWriter.close();
            br.close();
            mClientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disConnectServer(){
        mIsConnectServer = false;
        if (mClientSocket != null) {
            try {
                mClientSocket.shutdownInput();
                mClientSocket.close();
                mClientSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMessageToServer(){
        if (!mIsConnectServer){
            Toast.makeText(this,"没有连接上服务端，请重新连接",Toast.LENGTH_SHORT).show();
            return;
        }
        final String msg = mMessageEditText.getText().toString();
        if (!TextUtils.isEmpty(msg) && mPrintWriter != null) {
            mPrintWriter.println(msg);
            mMessageEditText.setText("");
            String time = formatDateTime(System.currentTimeMillis());
            final String showedMsg = "client " + time + ":" + msg + "\n";
            mMessageTextView.setText(mMessageTextView.getText() + showedMsg);
        }
    }
}
