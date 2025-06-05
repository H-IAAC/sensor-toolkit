package br.org.eldorado.hiaac.filetransfer;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

public class FileTranfer {
    private static final String TAG = "FileTranfer";

    private static final int SEND_FILE = 1;
    private static final String IP = "143.106.7.174";
    private static final int PORT = 8080;
    private static final String FILES_DIR = "/home/hiaac/files/";

    public static void transfer(File file, String dir) throws IOException {
        try (Socket socket = new Socket(IP, PORT);
             FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
            String fileName = file.getName();
            String fileNameDir = FILES_DIR + "/" + dir + "/";

            fileName = fileNameDir + fileName;
            byte[] fileNameBytes = fileName.getBytes();
            byte[] fileBytes = new byte[(int) file.length()];

            fileInputStream.read(fileBytes);
            dataOutputStream.writeInt(SEND_FILE);
            dataOutputStream.writeInt(fileNameBytes.length);
            dataOutputStream.write(fileNameBytes);
            dataOutputStream.writeInt(fileBytes.length);
            dataOutputStream.write(fileBytes);
            dataOutputStream.flush();

            Log.d(TAG, "Transferred file to server: " + fileName);
        }
    }

}
