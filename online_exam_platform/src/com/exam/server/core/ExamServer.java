package com.exam.server.core;

import com.exam.server.manager.ExamManager;
import com.exam.server.manager.ScoreManager;
import com.exam.server.manager.TimerManager;
import com.exam.server.manager.UserManager;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExamServer {

    private static final int PORT = 8888;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(20);

    private final UserManager userManager;
    private final ExamManager examManager;
    private final ScoreManager scoreManager;

    public ExamServer() {
        this.userManager = new UserManager();
        this.examManager = ExamManager.getInstance();
        this.scoreManager = new ScoreManager();
    }

    public void start() {
        System.out.println("ExamServer starting on port " + PORT + "...");
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            serverSocket.setReuseAddress(true);
            System.out.println("ExamServer started. Waiting for connections...");
            System.out.println("Press Ctrl+C to stop.");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New connection: " + socket.getInetAddress().getHostAddress());
                ClientHandler handler = new ClientHandler(socket, userManager, examManager, scoreManager);
                threadPool.execute(handler);
            }
        } catch (Exception e) {
            System.err.println("Server FATAL error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
            TimerManager.getInstance().shutdown();
        }
    }

    public static void main(String[] args) {
        new ExamServer().start();
    }
}
