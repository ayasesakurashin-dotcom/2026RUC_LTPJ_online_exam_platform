package com.exam.common.protocol;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String status;   // "OK" / "ERROR"
    private String message;
    private Serializable data;

    public Response() {}

    public Response(MessageType type, String status, String message, Serializable data) {
        this.type = type;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static Response ok(MessageType type, String message, Serializable data) {
        return new Response(type, "OK", message, data);
    }

    public static Response ok(MessageType type, Serializable data) {
        return new Response(type, "OK", null, data);
    }

    public static Response error(MessageType type, String message) {
        return new Response(type, "ERROR", message, null);
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Serializable getData() { return data; }
    public void setData(Serializable data) { this.data = data; }
}
