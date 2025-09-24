package br.org.eldorado.hiaac.audiocollector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import br.org.eldorado.hiaac.datacollector.util.Log;

public class AudioRecorder {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final Log log = new Log("AudioRecorder");
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private ByteArrayOutputStream outputStream;
    private Handler timeoutHandler;

    private final int sampleRate = 22050;
    private final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private final int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

    /* Default timeout to stop the audio record if the application don't call the stop method */
    private long record_timeout = 5 * 60 * 1000;

    /* An optional listener to receive the audio data when it stops recording */
    private OnRecordCompleteListener recordCompleteListener;

    private FragmentActivity activity;
    private boolean isPermissionGranted = false;


    public AudioRecorder(FragmentActivity activity) {
        this.activity = activity;

    }

    public AudioRecorder(Context ctx) {
        isPermissionGranted = hasAudioPermission(ctx);
        startAfterPermissionGranted();
    }

    public void setOnRecordCompleteListener(OnRecordCompleteListener listener) {
        this.recordCompleteListener = listener;
    }

    private void checkPermissions() {
        PermissionFragment fragment = PermissionFragment.newInstance(granted -> {
            isPermissionGranted = granted;
            if (granted) {
                log.i("Permission granted, ready to record!");
                startAfterPermissionGranted();
            } else {
                log.e("Permission denied, cant record!");
            }
        });

        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(fragment, "PermissionFragment")
                .commit();
    }

    private boolean hasAudioPermission(Context ctx) {
        return ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Starts recording audio if, and only if, permissions are granted
     */
    @SuppressLint("MissingPermission")
    public void startRecord() {
        if (isRecording) return;
        this.checkPermissions();
    }

    @SuppressLint("MissingPermission")
    private void startAfterPermissionGranted() {
        if (!isPermissionGranted) {
            log.e("Audio permission not granted!");
            throw new RuntimeException("Audio permission not granted!");
        }
        isRecording = true;
        isPaused = false;
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize);
        outputStream = new ByteArrayOutputStream();
        timeoutHandler = new Handler(Looper.getMainLooper());

        audioRecord.startRecording();

        new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            while (isRecording) {
                if (!isPaused) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        outputStream.write(buffer, 0, read);
                    }
                }
            }
        }).start();

        timeoutHandler.postDelayed(() -> {
            ByteArrayInputStream wavStream = stopRecord();
            if (recordCompleteListener != null && wavStream != null) {
                recordCompleteListener.onRecordComplete(wavStream);
            }
        }, record_timeout);
    }

    public void setRecordingTimeout(long timeout) {
        record_timeout = timeout;
    }

    /* Pauses the actual recording */
    public void pause() {
        if (isRecording) {
            isPaused = true;
        }
    }

    /* Resumes the recording if it was paused */
    public void resume() {
        if (isRecording) {
            isPaused = false;
        }
    }

    /**
     * Stops the audio recording and convert data to WAV format
     * @return The InputStream of the audio recorded and converted to WAV
     */
    public ByteArrayInputStream stopRecord() {
        isRecording = false;
        isPaused = false;

        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        ByteArrayOutputStream wavStream = new ByteArrayOutputStream();
        try {
            writeWAV(outputStream.toByteArray(), wavStream, sampleRate);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        ByteArrayInputStream resultStream = new ByteArrayInputStream(wavStream.toByteArray());

        if (recordCompleteListener != null) {
            recordCompleteListener.onRecordComplete(resultStream);
        }

        return resultStream;
    }

    /**
     * Utility method to convert an InputStream to an file
     * @param inputStream The InputStream to be converted
     * @param outputFile The target File
     * @return true if the conversion is successfully, false otherwise
     */
    public static boolean toFile(InputStream inputStream, File outputFile) {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void writeWAV(byte[] audioData, ByteArrayOutputStream outputStream, int sampleRate) throws IOException {
        int audioDataSize = audioData.length;
        int wavSize = 36 + audioDataSize;

        outputStream.write("RIFF".getBytes()); // RIFF Header
        outputStream.write(intToBytes(wavSize));
        outputStream.write("WAVE".getBytes());

        outputStream.write("fmt ".getBytes()); // Subchunk1: fmt
        outputStream.write(intToBytes(16)); // Tamanho do subchunk1 (16 para PCM)
        outputStream.write(shortToBytes((short) 1)); // Formato do áudio (1 = PCM sem compressão)
        outputStream.write(shortToBytes((short) 1)); // Número de canais (1 = mono, 2 = estéreo)
        outputStream.write(intToBytes(sampleRate)); // Taxa de amostragem
        outputStream.write(intToBytes(sampleRate * 2)); // Byte rate (sampleRate * canais * bitsPerSample/8)
        outputStream.write(shortToBytes((short) 2)); // Alinhamento de bloco (canais * bitsPorAmostra / 8)
        outputStream.write(shortToBytes((short) 16)); // Bits por amostra (16-bit)

        outputStream.write("data".getBytes()); // Subchunk2: data
        outputStream.write(intToBytes(audioDataSize)); // Tamanho dos dados de áudio
        outputStream.write(audioData); // Dados de áudio
    }

    private byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private byte[] shortToBytes(short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
    }

    /**
     * Utility callback that returns the recorded data after the timeout if stop method is not called
     */
    public interface OnRecordCompleteListener {
        void onRecordComplete(InputStream wavStream);
    }
}
