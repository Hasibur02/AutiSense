package com.example.asddetection;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL =
            "https://asd-detection-api-356986128669.asia-south1.run.app/";

    private static Retrofit retrofit;

    public static Retrofit getRetrofitInstance() {

        if (retrofit == null) {

            OkHttpClient client =
                    new OkHttpClient.Builder()

                            // Connection establish করার জন্য
                            .connectTimeout(
                                    60,
                                    TimeUnit.SECONDS
                            )

                            // Server response আসার জন্য
                            .readTimeout(
                                    300,
                                    TimeUnit.SECONDS
                            )

                            // Request পাঠানোর জন্য
                            .writeTimeout(
                                    300,
                                    TimeUnit.SECONDS
                            )

                            // পুরো call-এর জন্য
                            .callTimeout(
                                    300,
                                    TimeUnit.SECONDS
                            )

                            .build();


            retrofit =
                    new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client)
                            .addConverterFactory(
                                    GsonConverterFactory.create()
                            )
                            .build();
        }

        return retrofit;
    }


    public static ApiService getApiService() {

        return getRetrofitInstance()
                .create(ApiService.class);
    }
}