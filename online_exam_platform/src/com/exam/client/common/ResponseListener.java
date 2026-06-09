package com.exam.client.common;

import com.exam.common.protocol.Response;

public interface ResponseListener {
    void onResponse(Response response);
}
