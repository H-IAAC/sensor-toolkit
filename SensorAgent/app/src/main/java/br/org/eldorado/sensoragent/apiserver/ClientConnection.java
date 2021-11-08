package br.org.eldorado.sensoragent.apiserver;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import br.org.eldorado.sensoragent.util.Log;

public class ClientConnection {

    private Socket client;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private boolean isConnected;
    private Log log;
    private int keepAlive;

    public ClientConnection(Socket s) {
        try {
            this.log = new Log("ClientConnection");
            this.keepAlive = 0;
            this.client = s;
            this.in = new ObjectInputStream(client.getInputStream());
            this.out = new ObjectOutputStream(client.getOutputStream());
            this.isConnected = true;
            this.checkKeepAlive();
            handleMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleMessage() {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (isConnected) {
                            if (in.available() > 0) {
                                byte[] msg = new byte[in.readInt()];
                                in.read(msg);
                                APICommand cmd = new Gson().fromJson(new String(msg), APICommand.class);
                                APIController.getInstance().handleClientMessage(cmd, ClientConnection.this);
                            }
                            Thread.sleep(200);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            isConnected = false;
                            APIController.getInstance().removeClient(ClientConnection.this);
                            if (in != null) {
                                in.close();
                            }
                            if (out != null) {
                                out.close();
                            }
                            if (client != null) {
                                client.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(APICommand message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    log.i("Sending msg: " + message.getJson() + " client: " + client.isConnected());
                    byte[] msg = message.getBytes();
                    out.writeInt(msg.length);
                    out.write(message.getBytes());
                    out.flush();
                } catch (Exception e) {
                    isConnected = false;
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * If there aren't any messages from this client for more than 1 minute,
     * disconnects it
     */
    private void checkKeepAlive() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (isConnected) {
                        if (keepAlive > 60) {
                            /* Client didn't sent any message for more than 1 minute.
                             * Consider that this client is dead and disconnect it */
                            isConnected = false;
                            log.i("Client didn't send KEEP_ALIVE command for more than 1 minute. Disconnecting it . . .");
                        }
                        keepAlive++;
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Client sent a KEEP_ALIVE message
     */
    public synchronized void updateKeepAlive() {
        keepAlive = 0;
    }
}
