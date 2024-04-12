package br.org.eldorado.hiaac.datacollector.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import br.org.eldorado.sensorsdk.util.Log;

public class VideoMetadata {

    private static Log log = new Log("H-IAAC");

    public static void create(String filename,
                              float videoDuration,
                              long startTime,
                              long endTime,
                              File outputPath) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS  zZ");

        Date startTimeDate = new Date(startTime);
        Date endTimeDate = new Date(endTime);

        String content = "[Metadata]\n";
        content += "filename = " + filename + "\n";
        content += "videoDuration = " + videoDuration + "\n";
        content += "startTimestamp = " + startTime + "\n";
        content += "endTimestamp = " + endTime;

        log.d("VideoMetadata filename: " + filename);
        log.d("VideoMetadata startTimeDate: " + startTimeDate + " - " + dateFormat.format(startTimeDate));
        log.d("VideoMetadata endTimeDate: " + endTimeDate + " " + dateFormat.format(endTimeDate));

        File metadataFile = new File(outputPath.getAbsoluteFile() + File.separator + filename + ".video");

        try {
            metadataFile.deleteOnExit();
            metadataFile.createNewFile();

            FileOutputStream writer = new FileOutputStream(metadataFile);
            writer.write(content.getBytes());
            writer.close();
        } catch (IOException e) {
            log.i("Error creating video metadata file.");
        }

        log.i("File: " + metadataFile.getAbsolutePath() + " created.");
    }

    public static Metadata read(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        reader.readLine();

        String filename = reader.readLine().split("=")[1];
        String videoDuration = reader.readLine().split("=")[1];
        String startTimestamp = reader.readLine().split("=")[1];
        String endTimestamp = reader.readLine().split("=")[1];

        reader.close();

        return new Metadata(filename, videoDuration, startTimestamp, endTimestamp);
    }

    public static class Metadata {
        public String filename;
        public String videoDuration;
        public String startTimestamp;
        public String endTimestamp;

        public Metadata(String filename, String videoDuration, String startTimestamp, String endTimestamp) {
            this.filename = filename;
            this.videoDuration = videoDuration;
            this.startTimestamp = startTimestamp;
            this.endTimestamp = endTimestamp;
        }
    }

}
