package com.hellrider.redtest;

import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CBService {
    @GET("XML_dynamic.asp")
    Call<ValCurs> listData(@Query("date_req1") String date1, @Query("date_req2") String date2, @Query("VAL_NM_RQ") String currencyCode);
}