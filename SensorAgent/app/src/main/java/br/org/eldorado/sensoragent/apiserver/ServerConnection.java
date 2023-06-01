package br.org.eldorado.sensoragent.apiserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;

import br.org.eldorado.sensoragent.util.Log;

public class ServerConnection {

    private Log log;
    private static ServerConnection inst;
    private boolean isRunning;
    private ServerSocket socket;

    public static void getInstance() {
        if (inst == null) {
            inst = new ServerConnection();
        }
    }

    private ServerConnection() {
        try {
            this.log = new Log("ServerConnection");
            this.isRunning = false;
            this.socket = new ServerSocket(9669);
            log.i("Starting server at: " + getIpAddress());
            this.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startServer() {
        if (!isRunning) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    isRunning = true;
                    while (isRunning) {
                        try {
                            APIController.getInstance().addClient(new ClientConnection(socket.accept()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    private String getIpAddress() {
        StringBuilder ip = new StringBuilder();
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip.append("SiteLocalAddress: ").append(inetAddress.getHostAddress()).append("\n");
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip.append("Something Wrong! ").append(e.toString()).append("\n");
        }

        return ip.toString();
    }
}
