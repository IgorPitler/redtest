package com.hellrider.redtest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;
import java.util.regex.Pattern;

public class FTSlib {

    public static final String server_script_base_url="http://www.cbr.ru/scripts/";
    public static  final String currencyCode="R01235";
    public static Pattern numeric_pattern = Pattern.compile("-?\\d+(\\,\\d+)?");
    public static Pattern numeric_pattern_point = Pattern.compile("-?\\d+(\\.\\d+)?");

    public static void myToast(Context con, String s) {
        Toast.makeText(con, s, Toast.LENGTH_LONG).show();
    }

    public static boolean isNetworkOnline(Context con)
    {
        boolean status = false;

        try
        {
            ConnectivityManager cm = (ConnectivityManager) con
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getNetworkInfo(0);

            if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
                status = true;
            } else {
                netInfo = cm.getNetworkInfo(1);

                if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
                    status = true;
                } else {
                    status = false;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return status;
    }
}
