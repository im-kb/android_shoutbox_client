package com.example.ppsm_budzik_shoutbox

import retrofit2.Call
import retrofit2.http.*

interface JsonPlaceholderAPI {
    @GET("shoutbox/messages")
    fun getMessageArray(): Call<Array<MyMessage>?>?

    @POST("shoutbox/message")
    fun createPost(@Body MyMessage: MyMessage): Call<MyMessage>

    @PUT("shoutbox/message/{id}")
    fun createPut(
        @Path("id") id: String,
        @Body exampleItem: MyMessage
    ): Call<MyMessage>

    @DELETE("shoutbox/message/{id}")
    fun createDelete(
        @Path("id") id: String
    ): Call<MyMessage>
}