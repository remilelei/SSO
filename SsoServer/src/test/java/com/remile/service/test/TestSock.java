package com.remile.service.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TestSock {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(50051);
            Socket socket = serverSocket.accept();
            System.out.println("recv");
            InputStream is = socket.getInputStream();
            byte[] buf = new byte[2048];
            int pos = 0;
            while((pos = is.read(buf)) > 0) {
                String s = new String(buf, 0, pos);
                System.out.println(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
