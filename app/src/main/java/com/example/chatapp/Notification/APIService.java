package com.example.chatapp.Notification;

import com.example.chatapp.Notification.Sender;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAAXUbWJJ8:APA91bF3w9V_QiseCUTOtO_0nssJdNIIud2eST5NSKUOo6Rqn2wCXkI041O6aQTolClbWVkL2cS6WWsAQCWtfD6q6X2MdOmWUjfqiWh9ybYLEP3Ju-2ZVolGCkeXeZey52R3rtmhMa8t"
            }
    )

    @POST("fcm/send")
    Call<Response> sendNotification(@Body Sender body);
}
