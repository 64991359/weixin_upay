package com.yixinu.dev.upay.service;

import org.json.JSONObject;

public interface IService {
    public boolean sendData(JSONObject json);
    public boolean sendData(String jsonstr);
}
