package com.hellrider.redtest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.content.Context;

import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import static androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV;
import static androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import static com.hellrider.redtest.FTSlib.currencyCode;
import static com.hellrider.redtest.FTSlib.isNetworkOnline;
import static com.hellrider.redtest.FTSlib.myToast;

import static com.hellrider.redtest.FTSlib.numeric_pattern;
import static com.hellrider.redtest.FTSlib.numeric_pattern_point;
import static com.hellrider.redtest.FTSlib.server_script_base_url;

public class MainActivity extends AppCompatActivity {

    LinearLayoutManager mLayoutManager;

    private int last_scroll_position=0;

    private int notificationNewMsgId=2; // for new messages notification
    private NotificationCompat.Builder builder;

    final String CHANNEL_ID="channel"; //notification channeel
    NotificationManagerCompat notificationManager;

    private Timer mTimer;
    private TimerTask mMyTimerTask;

    private boolean isInitialised=false;

    EditText alertEdit;
    Calendar calendar;

    SharedPreferences sPref;
    SharedPreferences.Editor sPrefEditor;
    private MasterKey masterKey;

    private float alert_value=0;

    private String date_pattern="dd/MM/yyyy";
    private SimpleDateFormat sdf=new SimpleDateFormat(date_pattern);
    private String date1, date2;

    RecyclerView recyclerView;

    private ArrayList<DataItem> items_arr;
    private DataItems_adapter itemsAdapter;

    Retrofit retrofit;

    CBService cbservice;

    private Record lastRecord;

    private boolean isLoading=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            masterKey=new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            sPref= EncryptedSharedPreferences.create(this,"fts.ini", masterKey, AES256_SIV, AES256_GCM);
            alert_value=sPref.getFloat("alert_value", 0);

        } catch (GeneralSecurityException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }

        notificationManager = NotificationManagerCompat.from(this);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel2 = new NotificationChannel(CHANNEL_ID,"channel", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel2);
        }

        calendar=java.util.Calendar.getInstance();
        date2=sdf.format(calendar.getTime());
        calendar.add(Calendar.DATE, -30);
        date1=sdf.format(calendar.getTime());

        retrofit = new Retrofit.Builder()
                .baseUrl(server_script_base_url)
                .addConverterFactory(JacksonConverterFactory.create(new XmlMapper(new JacksonXmlModule())))
                .build();
        cbservice = retrofit.create(CBService.class);

        try {
            isInitialised = savedInstanceState.getBoolean("isInitialised", false);
            last_scroll_position = savedInstanceState.getInt("last_scroll_position", 0);
        } catch (NullPointerException exception) {
            //
        }

        if (!isInitialised) {
            items_arr = new ArrayList<DataItem>();
        } else {
            items_arr = (ArrayList<DataItem>) savedInstanceState.getSerializable("items_arr");
        }

        alertEdit=findViewById(R.id.alertEdit);
        alertEdit.setText(String.valueOf(alert_value));
        recyclerView = findViewById(R.id.recView1);
        itemsAdapter = new DataItems_adapter(this, items_arr);
        recyclerView.setAdapter(itemsAdapter);

        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addOnScrollListener(scroll_listener);

        if (last_scroll_position > 0) recyclerView.scrollToPosition(last_scroll_position);

        if (!isInitialised) {
            mMyTimerTask = new MyTimerTask();
            mTimer = new Timer();
            mTimer.schedule(mMyTimerTask, 0, 5000);
        }
    }

    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    timer_action();
                }
            });
        }
    }

    public void timer_action()
    {
        if (!isInitialised) {
            if (!isNetworkOnline(getApplicationContext())) {
                myToast(getApplicationContext(), "Network error");
                return;
            }
            load_data();
        }
    }

    public void load_data() {
        if (!isLoading) {
            isLoading=true;

            Call<ValCurs> valInfoCall = cbservice.listData(date1, date2, currencyCode);

            valInfoCall.enqueue(new Callback<ValCurs>() {
                @Override
                public void onResponse(Call<ValCurs> call, Response<ValCurs> response) {
                    isLoading=false;
                    if (response.isSuccessful()) {
                        isInitialised=true;
                        mTimer.cancel();
                        update_data_list(response.body());
                    } else {
                        myToast(getApplicationContext(), "onResponse no success");
                    }
                }

                @Override
                public void onFailure(Call<ValCurs> call, Throwable t) {
                    isLoading=false;
                    myToast(getApplicationContext(), "onFailure "+t.getMessage());

                }
            });
        }
    }

    public void update_data_list(ValCurs curs_data)
    {
        items_arr.clear();

        for (int i=0; i < curs_data.getRecords().size(); i++)
        {
            items_arr.add(new DataItem(curs_data.getRecords().get(i).getDate(), curs_data.getRecords().get(i).getValue())); //curs_data.getRecords();
        }
        itemsAdapter.notifyDataSetChanged();

        if (curs_data.getRecords().size() > 0)
        {
            lastRecord=curs_data.getRecords().get(curs_data.getRecords().size()-1);

            if (Float.valueOf(lastRecord.getValue().replace(",", ".")) > alert_value)
            {
                notify_curs(Float.valueOf(lastRecord.getValue().replace(",", ".")));
            }
        }
    }

    public void notify_curs(float value)
    {
        audioNotification();
        show_new_msg_notification(value);
    }

    public void save_alert_value(View view)
    {
        float new_value=0;
        String buffer=alertEdit.getText().toString();
        if (isNumeric(buffer))
        {
            buffer=buffer.replace(",", ".");
            new_value=Float.valueOf(buffer);
            alert_value=new_value;
            sPrefEditor=sPref.edit();
            sPrefEditor.putFloat("alert_value", new_value);
            sPrefEditor.commit();
        } else
        {
            myToast(getApplicationContext(), "wrong number!");
        }
    }

    public boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        return numeric_pattern.matcher(strNum).matches() || numeric_pattern_point.matcher(strNum).matches();
    }

    public void show_new_msg_notification(float curs)
    {
        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_swap_horizontal_circle) // change later
                .setContentTitle("Currency alert: "+String.valueOf(curs))
                .setContentText("")
                .setShowWhen(false)
                .setAutoCancel(true)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(notificationNewMsgId,builder.build());
    }

    public void hide_msg_notification()
    {
        notificationManager.cancel(notificationNewMsgId);
    }

    public void audioNotification()
    {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

            outState.putBoolean("isInitialised", isInitialised);
            outState.putInt("last_scroll_position", last_scroll_position);
            outState.putSerializable("items_arr", items_arr);
    }

    RecyclerView.OnScrollListener scroll_listener=new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (dy > 0) // scrolled down
            {
                last_scroll_position=mLayoutManager.findFirstVisibleItemPosition();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            mTimer.cancel();
        } catch (NullPointerException E) {    }
    }
}