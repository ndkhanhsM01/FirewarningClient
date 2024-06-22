package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

public class InfoReceive {
    @SerializedName("type") public MessageType Type;
    @SerializedName("message") public String Message;
    @SerializedName("clientId") public String ClientId;

    public  enum  MessageType{MESSAGE, WARNING}
}
