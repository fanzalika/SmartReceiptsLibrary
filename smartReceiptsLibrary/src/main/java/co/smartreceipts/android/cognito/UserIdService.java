package co.smartreceipts.android.cognito;

import android.support.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * An interface that we can use in conjunction with network requests
 */
public interface UserIdService {

    @GET("/user/{id}")
    Call<UserIdResponse> getUserId(@NonNull @Path("id") String id);
}