package com.exam.common.protocol;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private Serializable data;

    public Request() {}

    public Request(MessageType type, Serializable data) {
        this.type = type;
        this.data = data;
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public Serializable getData() { return data; }
    public void setData(Serializable data) { this.data = data; }
}
