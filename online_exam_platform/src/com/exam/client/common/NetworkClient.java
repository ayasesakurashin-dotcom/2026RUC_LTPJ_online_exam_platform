package com.exam.client.common;

import com.exam.common.protocol.Request;
import com.exam.common.protocol.Response;

import java.io.*;
import java.net.Socket;

public class NetworkClient {

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private volatile boolean running;
    private ResponseListener listener;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        oos = new ObjectOutputStream(socket.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(socket.getInputStream());
        running = true;

        // 后台读取线程
        new Thread(() -> {
            while (running) {
                try {
                    Response response = (Response) ois.readObject();
                    if (listener != null) {
                        // 直接在读取线程回调，避免EDT死锁。
                        // 监听器负责需要时用invokeLater触达EDT更新UI。
                        listener.onResponse(response);
                    }
                } catch (EOFException e) {
                    running = false;
                } catch (Exception e) {
                    if (running) {
                        System.err.println("Read error: " + e.getMessage());
                    }
                    running = false;
                }
            }
        }, "Client-Reader").start();
    }

    public synchronized void send(Request request) throws IOException {
        oos.writeObject(request);
        oos.flush();
    }

    public void setListener(ResponseListener listener) {
        this.listener = listener;
    }

    public void disconnect() {
        running = false;
        try { socket.close(); } catch (Exception ignored) {}
    }

    public boolean isConnected() {
        return running && socket != null && !socket.isClosed();
    }
}
