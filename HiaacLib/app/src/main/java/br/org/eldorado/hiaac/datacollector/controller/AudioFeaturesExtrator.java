package br.org.eldorado.hiaac.datacollector.controller;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import br.org.eldorado.hiaac.datacollector.model.ExtraSensoryData;
import br.org.eldorado.sensoragent.model.SensorBase;

public class AudioFeaturesExtrator {

    private static final String LOG_TAG = "[AudioFeaturesExtractor]";

    private static final int AUDIO_FRAME_WINDOW_SIZE = 2048;
    private static final int AUDIO_FRAME_HOP_SIZE = 1024;
    private static final double PREEMPHASIS_COEFFICIENT = 0.97;
    private static final double HAMMING_ALPHA = 0.54;
    private static final double HAMMING_BETTA = 1 - HAMMING_ALPHA;
    private static final int NUM_MEL_FILTERS = 34;
    private static final int NUM_CEPSTRAL_COEFFS = 13;
    private static final boolean USE_FIRST_COEFF = true;
    private static final double DEFAULT_AUDIO_NORMALIZATION_MULTIPLIER = 1.0 / (double) Short.MAX_VALUE;

    private final double[] hammingWindow;
    private final MFCC mfccProcessor;

    private int maxAbsValue = 0;
    private double normalizingMultiplier = DEFAULT_AUDIO_NORMALIZATION_MULTIPLIER;

    public AudioFeaturesExtrator(int sampleRate) {
        // Inicializa a janela Hamming
        hammingWindow = new double[AUDIO_FRAME_WINDOW_SIZE];
        double factor = 2 * Math.PI / (AUDIO_FRAME_WINDOW_SIZE - 1);
        for (int i = 0; i < AUDIO_FRAME_WINDOW_SIZE; i++) {
            hammingWindow[i] = HAMMING_ALPHA - HAMMING_BETTA * Math.cos(i * factor);
        }

        // Inicializa MFCC
        mfccProcessor = new MFCC(NUM_CEPSTRAL_COEFFS, sampleRate, NUM_MEL_FILTERS, AUDIO_FRAME_WINDOW_SIZE, false, 0, USE_FIRST_COEFF);
    }

    public String extractFeaturesFromStream(ByteArrayInputStream inputStream, ExtraSensoryData esData) {
        if (inputStream == null) return null;

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(inputStream))) {

            // Lê todos os samples como PCM 16 bits
            short[] pcmSamples = readAllShorts(dis);
            if (pcmSamples.length == 0) return null;

            // Calcula normalização
            calculateNormalizationValues(pcmSamples);

            // Armazena todos os MFCCs para cálculo de mean/std
            int estimatedFrames = pcmSamples.length / AUDIO_FRAME_HOP_SIZE + 2;
            double[][] allMFCC = new double[NUM_CEPSTRAL_COEFFS][estimatedFrames];

            int frameCount = 0;
            double[] rawFrame = new double[AUDIO_FRAME_WINDOW_SIZE];

            for (int start = 0; start < pcmSamples.length; start += AUDIO_FRAME_HOP_SIZE) {
                Arrays.fill(rawFrame, 0.0);
                for (int i = 0; i < AUDIO_FRAME_WINDOW_SIZE && (start + i) < pcmSamples.length; i++) {
                    double val = pcmSamples[start + i] * normalizingMultiplier;
                    if (i > 0) val -= PREEMPHASIS_COEFFICIENT * rawFrame[i - 1];
                    rawFrame[i] = val;
                }

                double[] windowed = new double[AUDIO_FRAME_WINDOW_SIZE];
                for (int i = 0; i < AUDIO_FRAME_WINDOW_SIZE; i++) {
                    windowed[i] = rawFrame[i] * hammingWindow[i];
                }

                double[] mfccVector = mfccProcessor.getParameters(windowed);
                for (int i = 0; i < NUM_CEPSTRAL_COEFFS; i++) {
                    allMFCC[i][frameCount] = mfccVector[i];
                }
                frameCount++;
            }

            // Calcula média e desvio padrão
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < NUM_CEPSTRAL_COEFFS; i++) {
                double mean = mean(allMFCC[i], frameCount);
                sb.append("audio_naive:mfcc").append(i).append(":mean,").append(mean).append(",");
                esData.addMFCCFeature(ExtraSensoryData.FeatureName.AUDIO_MFCC_MEAN, i, mean);
            }
            for (int i = 0; i < NUM_CEPSTRAL_COEFFS; i++) {
                double std = std(allMFCC[i], frameCount);
                sb.append("audio_naive:mfcc").append(i).append(":std,").append(std).append(",");
                esData.addMFCCFeature(ExtraSensoryData.FeatureName.AUDIO_MFCC_STD, i, std);
            }

            sb.append("audio_properties:max_abs_value,").append(maxAbsValue).append(",");
            sb.append("audio_properties:normalization_multiplier,").append(normalizingMultiplier);
            esData.addFeature(ExtraSensoryData.FeatureName.AUDIO_MAX_ABS, SensorBase.TYPE_AUDIO, maxAbsValue);
            esData.addFeature(ExtraSensoryData.FeatureName.AUDIO_NORMALIZATION_MULTIPLIER, SensorBase.TYPE_AUDIO, normalizingMultiplier);

            return sb.toString();

        } catch (IOException e) {
            Log.e(LOG_TAG, "Erro lendo InputStream", e);
            return null;
        }
    }

    private short[] readAllShorts(DataInputStream dis) throws IOException {
        short[] buffer = new short[dis.available() / 2];
        int index = 0;
        try {
            while (true) {
                buffer[index++] = dis.readShort();
            }
        } catch (IOException e) {
            // EOF esperado
        }
        if (index < buffer.length) {
            buffer = Arrays.copyOf(buffer, index);
        }
        return buffer;
    }

    private void calculateNormalizationValues(short[] samples) {
        int maxVal = 0;
        for (short s : samples) {
            int absVal = Math.abs(s);
            if (absVal > maxVal) maxVal = absVal;
        }
        maxAbsValue = maxVal;
        normalizingMultiplier = (maxVal > 0) ? 1.0 / maxVal : DEFAULT_AUDIO_NORMALIZATION_MULTIPLIER;
    }

    private double mean(double[] arr, int count) {
        double sum = 0.0;
        for (int i = 0; i < count; i++) sum += arr[i];
        return sum / count;
    }

    private double std(double[] arr, int count) {
        double m = mean(arr, count);
        double sumSq = 0.0;
        for (int i = 0; i < count; i++) sumSq += (arr[i] - m) * (arr[i] - m);
        return Math.sqrt(sumSq / count);
    }
}
