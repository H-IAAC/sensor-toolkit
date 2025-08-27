package br.org.eldorado.hiaac.datacollector.controller;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.org.eldorado.hiaac.datacollector.data.LabeledData;
import br.org.eldorado.hiaac.datacollector.model.ExtraSensoryData;
import br.org.eldorado.hiaac.datacollector.util.Log;

public class ExtraSensoryConverterController {

    private final Log log = new Log("ExtraSensoryConverterController");
    private final double samplingRate;

    private Integer currentSensor;

    private ExtraSensoryData esData;

    public ExtraSensoryConverterController() {
        this(40.0);
    }

    public ExtraSensoryConverterController(double samplingRate) {
        this.samplingRate = samplingRate;
        this.esData = new ExtraSensoryData();
    }

    public ExtraSensoryData convertData(Map<Integer, List<LabeledData>> data) {
        for (Integer sensor : data.keySet()) {
            List<LabeledData> sensorData = data.get(sensor);
            extractFeatures(sensorData);
        }
        return esData;
    }

    private void extractFeatures(List<LabeledData> sensorData) {
        try {
            if (sensorData == null || sensorData.isEmpty()) {
                throw new IllegalArgumentException("sensorData empty");
            }
            if (sensorData.get(0).getSensor().getValuesArray().length != 3) {
                throw new IllegalArgumentException("Waiting 3 axes data (x,y,z).");
            }
            currentSensor = sensorData.get(0).getSensor().getType();
            if (!esData.isSensorSupported(currentSensor)) {
                throw new IllegalArgumentException("Sensor not supported - " + sensorData.get(0).getSensor().getName());
            }

            log.d("Converting data to ExtraSensory format. Collected Samples: " + sensorData.size() + " Sensor: " + sensorData.get(0).getSensor().getName());

            int dataSize = sensorData.size();
            double[] x = new double[dataSize];
            double[] y = new double[dataSize];
            double[] z = new double[dataSize];

            for (int i = 0; i < dataSize; i++) {
                x[i] = sensorData.get(i).getSensorValuesAsDouble()[0];
                y[i] = sensorData.get(i).getSensorValuesAsDouble()[1];
                z[i] = sensorData.get(i).getSensorValuesAsDouble()[2];
            }

            double[] magnitude = new double[dataSize];
            for (int i = 0; i < dataSize; i++) {
                magnitude[i] = Math.sqrt(x[i] * x[i] + y[i] * y[i] + z[i] * z[i]);
            }

            extractMagnitudeFeatures(magnitude);
            extract3dFeatures(x, y, z);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= magnitude features (1-17) =================
    private void extractMagnitudeFeatures(double[] magnitude) {

        // 1-7: estatísticas básicas
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_MEAN, currentSensor, safeDouble(StatUtils.mean(magnitude)));

        // padrão amostral (ddof=1) -> biasCorrected = true em StandardDeviation
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_STD, currentSensor, safeDouble(new StandardDeviation(true).evaluate(magnitude)));

        // moment3: sinal preservado + raiz cúbica; moment4: raiz quarta do valor absoluto
        double m3 = moment(magnitude, 3);
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_MOMENT3, currentSensor, safeDouble(Math.signum(m3) * Math.pow(Math.abs(m3), 1.0 / 3.0)));

        double m4 = moment(magnitude, 4);
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_MOMENT4, currentSensor, safeDouble(Math.pow(Math.abs(m4), 1.0 / 4.0)));

        // percentis 25/50/75
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_PERCENTILE25, currentSensor, safeDouble(percentile(magnitude, 25.0)));
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_PERCENTILE50, currentSensor, safeDouble(percentile(magnitude, 50)));
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_PERCENTILE75, currentSensor, safeDouble(percentile(magnitude, 75.0)));

        // 8-9: entropias
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_VALUE_ENTROPY, currentSensor, safeDouble(calculateValueEntropy(magnitude, 20)));
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_TIME_ENTROPY, currentSensor, safeDouble(calculateTimeEntropy(magnitude)));

        // 10-15: espectro
        extractSpectrumFeatures(magnitude);

        // 16-17: autocorrelação
        extractAutocorrelationFeatures(magnitude);
    }

    // ================= 3D features (18-26) =================
    private void extract3dFeatures(double[] x, double[] y, double[] z) {
        esData.addFeature(ExtraSensoryData.FeatureName.THREE_D_MEAN_X, currentSensor, safeDouble(StatUtils.mean(x)));
        esData.addFeature(ExtraSensoryData.FeatureName.THREE_D_MEAN_Y, currentSensor, safeDouble(StatUtils.mean(y)));
        esData.addFeature(ExtraSensoryData.FeatureName.THREE_D_MEAN_Z, currentSensor, safeDouble(StatUtils.mean(z)));

        esData.addFeature(ExtraSensoryData.FeatureName.THREE_D_STD_X, currentSensor, safeDouble(new StandardDeviation(true).evaluate(x)));
        esData.addFeature(ExtraSensoryData.FeatureName.THREE_D_STD_Y, currentSensor, safeDouble(new StandardDeviation(true).evaluate(y)));
        esData.addFeature(ExtraSensoryData.FeatureName.THREE_D_STD_Z, currentSensor, safeDouble(new StandardDeviation(true).evaluate(z)));

        PearsonsCorrelation corr = new PearsonsCorrelation();
        esData.addFeature(ExtraSensoryData.FeatureName.THREE_D_RO_XY, currentSensor, safeCorr(corr, x, y));
        esData.addFeature(ExtraSensoryData.FeatureName.THREE_D_RO_XZ, currentSensor, safeCorr(corr, x, z));
        esData.addFeature(ExtraSensoryData.FeatureName.THREE_D_RO_YZ, currentSensor, safeCorr(corr, y, z));
    }

    private double safeCorr(PearsonsCorrelation corr, double[] a, double[] b) {
        if (a == null || b == null || a.length < 2) return 0.0;
        double r = corr.correlation(a, b);
        if (Double.isNaN(r)) return 0.0;
        return r;
    }

    // ================= utilitários estatísticos =================
    private double moment(double[] data, int order) {
        if (data == null || data.length == 0) return 0.0;
        double mean = StatUtils.mean(data);
        double sum = 0.0;
        for (double v : data) sum += Math.pow(v - mean, order);
        return sum / data.length;
    }

    private double percentile(double[] data, double p) {
        if (data == null || data.length == 0) return Double.NaN;
        return StatUtils.percentile(data, p);
    }

    private double calculateValueEntropy(double[] data, int bins) {
        try {
            double sumAbs = Arrays.stream(data).map(Math::abs).sum();
            if (sumAbs <= 0) return 0.0;
            double[] counts = histogramCounts(data, bins);
            return entropyFromCounts(counts);
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private double calculateTimeEntropy(double[] data) {
        try {
            double sumAbs = Arrays.stream(data).map(Math::abs).sum();
            if (sumAbs <= 0) return 0.0;
            double[] absVals = Arrays.stream(data).map(Math::abs).toArray();
            // usar entropia direta (tratamos os valores absolutos como "bins" discretos)
            return entropyFromCounts(absVals);
        } catch (Exception ex) {
            return 0.0;
        }
    }

    // histogram counts (equivalente ao numpy.histogram(...)[0])
    private double[] histogramCounts(double[] data, int bins) {
        double min = Arrays.stream(data).min().orElse(0.0);
        double max = Arrays.stream(data).max().orElse(0.0);
        double[] counts = new double[bins];
        if (max == min) {
            // tudo no mesmo valor -> primeiro bin recebe todos
            counts[0] = data.length;
            return counts;
        }
        double width = (max - min) / bins;
        for (double v : data) {
            int idx = (int) ((v - min) / width);
            if (idx < 0) idx = 0;
            if (idx >= bins) idx = bins - 1;
            counts[idx] += 1.0;
        }
        return counts;
    }

    // entropia a partir de contagens ou valores (se o vetor vier como "valores", tratamos como contagens)
    private double entropyFromCounts(double[] countsOrValues) {
        double sum = Arrays.stream(countsOrValues).sum();
        if (sum == 0) return 0.0;
        double e = 0.0;
        for (double v : countsOrValues) {
            if (v > 0) {
                double p = v / sum;
                e -= p * Math.log(p);
            }
        }
        return e;
    }

    // ================= espectro (10-15) =================
    private void extractSpectrumFeatures(double[] magnitude) {
        //Map<String, Double> features = new LinkedHashMap<>();
        int mSize = magnitude.length;

        // inicializa com NaN caso não haja dados suficientes
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND0, currentSensor, Double.NaN);
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND1, currentSensor, Double.NaN);
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND2, currentSensor, Double.NaN);
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND3, currentSensor, Double.NaN);
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND4, currentSensor, Double.NaN);
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRAL_ENTROPY, currentSensor, Double.NaN);

        if (mSize <= 1) return;

        try {
            // Janela Hamming
            double[] ham = hammingWindow(mSize);
            double[] xw = new double[mSize];
            for (int i = 0; i < mSize; i++) xw[i] = ham[i] * magnitude[i];

            // FFT (complex) de tamanho T
            DoubleFFT_1D fft = new DoubleFFT_1D(mSize);
            double[] complex = new double[2 * mSize]; // interleaved real, imag
            for (int i = 0; i < mSize; i++) {
                complex[2 * i] = xw[i];
                complex[2 * i + 1] = 0.0;
            }
            fft.complexForward(complex);

            // frequências correspondentes
            double[] freqs = new double[mSize];
            for (int k = 0; k < mSize; k++) {
                freqs[k] = k * (samplingRate / mSize);
                if (k >= mSize/2 + 1) { // ajustar para frequências negativas
                    freqs[k] = (k - mSize) * (samplingRate / mSize);
                }
            }

            // manter apenas frequências >= 0 (índices 0 .. floor(T/2))
            int maxPos = mSize / 2;
            double[] PS = new double[maxPos + 1];
            for (int k = 0; k <= maxPos; k++) {
                double re = complex[2 * k];
                double im = complex[2 * k + 1];
                PS[k] = re * re + im * im;
            }

            double power = Arrays.stream(PS).sum();

            double[] nPS;
            if (power > 0.0) {
                nPS = new double[PS.length];
                for (int i = 0; i < PS.length; i++) nPS[i] = PS[i] / power;
            } else {
                // se power == 0, seguir comportamento defensivo (retornar NaN)
                esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND0, currentSensor, Double.NaN);
                esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND1, currentSensor, Double.NaN);
                esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND2, currentSensor, Double.NaN);
                esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND3, currentSensor, Double.NaN);
                esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND4, currentSensor, Double.NaN);
                esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRAL_ENTROPY, currentSensor, Double.NaN);
                return;
            }

            // calcular energias das sub-bandas (conforme ExtraSensory)
            double[] energies = new double[5];
            // band0: 0 < f <= 0.5
            for (int k = 1; k <= maxPos; k++) {
                double f = freqs[k];
                if (f > 0.0 && f <= 0.5) energies[0] += nPS[k];
            }
            // bands 1-4 definidas pelos cutoffs [0.5, 1., 3., 5., nyquist]
            double[] cutoffs = new double[]{0.5, 1.0, 3.0, 5.0};
            for (int bi = 0; bi < cutoffs.length; bi++) {
                double low = cutoffs[bi];
                double high = (bi < cutoffs.length - 1) ? cutoffs[bi + 1] : Double.POSITIVE_INFINITY;
                if (bi == cutoffs.length - 1) high = samplingRate / 2.0; // band4 high = Nyquist
                for (int k = 0; k <= maxPos; k++) {
                    double f = freqs[k];
                    if (f >= low && f < high) energies[bi + 1] += nPS[k];
                }
            }

            // log compression
            double[] logE = logCompression(energies);

            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND0, currentSensor, safeDouble(logE[0]));
            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND1, currentSensor, safeDouble(logE[1]));
            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND2, currentSensor, safeDouble(logE[2]));
            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND3, currentSensor, safeDouble(logE[3]));
            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND4, currentSensor, safeDouble(logE[4]));

            // espectral entropy
            double spectralEntropy = entropyFromCounts(nPS);
            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRAL_ENTROPY, currentSensor, safeDouble(spectralEntropy));

        } catch (Exception ex) {
            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND0, currentSensor, Double.NaN);
            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND1, currentSensor, Double.NaN);
            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND2, currentSensor, Double.NaN);
            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND3, currentSensor, Double.NaN);
            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRUM_LOG_BAND4, currentSensor, Double.NaN);
            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_SPECTRAL_ENTROPY, currentSensor, Double.NaN);
        }
    }

    private double[] hammingWindow(int N) {
        double[] w = new double[N];
        for (int n = 0; n < N; n++) {
            w[n] = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * n / (N - 1));
        }
        return w;
    }

    private double[] logCompression(double[] vals) {
        double eps = 0.001;
        double[] out = new double[vals.length];
        for (int i = 0; i < vals.length; i++) {
            double lv = Math.log(eps + Math.abs(vals[i])) - Math.log(eps);
            out[i] = Math.signum(vals[i]) * lv;
        }
        return out;
    }

    // ================= autocorrelação (16-17) =================
    private void extractAutocorrelationFeatures(double[] magnitude) {
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_AUTOCORRELATION_PERIOD, currentSensor, Double.NaN);
        esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_AUTOCORRELATION_NORMALIZED_AC, currentSensor, Double.NaN);
        int n = magnitude.length;
        if (n <= 1) return;

        try {
            // remover DC
            double mean = StatUtils.mean(magnitude);
            double[] x = new double[n];
            double sumAbs = 0.0;
            for (int i = 0; i < n; i++) {
                x[i] = magnitude[i] - mean;
                sumAbs += Math.abs(x[i]);
            }
            if (sumAbs == 0.0) return;

            // FFT-based autocorrelation: tamanho M >= nextPow2(2*N)
            int M = nextPowerOfTwo(2 * n);
            double[] complex = new double[2 * M];
            for (int i = 0; i < n; i++) {
                complex[2 * i] = x[i];
                complex[2 * i + 1] = 0.0;
            }
            for (int i = n; i < M; i++) {
                complex[2 * i] = 0.0;
                complex[2 * i + 1] = 0.0;
            }

            DoubleFFT_1D fft = new DoubleFFT_1D(M);
            fft.complexForward(complex);

            // |FFT|^2 (power spectrum)
            for (int k = 0; k < M; k++) {
                double re = complex[2 * k];
                double im = complex[2 * k + 1];
                double power = re * re + im * im;
                complex[2 * k] = power;
                complex[2 * k + 1] = 0.0;
            }

            // ifft of power -> autocorrelation (circular, mas como M>=2N vira linear)
            fft.complexInverse(complex, true);

            // autocorr linear tem comprimento até M; queremos a porção centrada como em np.correlate(x,x,'same')
            // índice de lag zero na autocorr linear está em (N - 1)
            int zeroLagIndex = n - 1;
            int available = M - zeroLagIndex;
            double[] acfFrom0 = new double[available];
            double zerolag = complex[2 * zeroLagIndex]; // real part at zero lag
            if (zerolag == 0.0) {
                log.d("extractAutocorrelationFeatures - zerolag = 0.0. Returning NaN ");
                esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_AUTOCORRELATION_PERIOD, currentSensor, Double.NaN);
                esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_AUTOCORRELATION_NORMALIZED_AC, currentSensor, Double.NaN);
                return;
            }
            for (int i = 0; i < available; i++) {
                acfFrom0[i] = complex[2 * (zeroLagIndex + i)] / zerolag; // normalize by zero lag
            }

            // encontrar primeiros índices negativos para zerar antes do primeiro crossing negativo
            int firstNeg = -1;
            for (int i = 0; i < acfFrom0.length; i++) {
                if (acfFrom0[i] < 0) { firstNeg = i; break; }
            }
            int periodLag;
            if (firstNeg <= 0) {
                // sem periodicidade detectada -> usar último lag
                periodLag = acfFrom0.length - 1;
            } else {
                // zerar valores antes do primeiro crossing negativo
                for (int i = 0; i < firstNeg; i++) acfFrom0[i] = 0.0;
                // selecionar máximo do segundo lóbulo em diante
                double maxVal = Double.NEGATIVE_INFINITY;
                int maxIdx = 0;
                for (int i = 0; i < acfFrom0.length; i++) {
                    if (acfFrom0[i] > maxVal) { maxVal = acfFrom0[i]; maxIdx = i; }
                }
                periodLag = maxIdx;
            }

            double periodSeconds = ((double) periodLag) / samplingRate;
            double periodAc = (periodLag < acfFrom0.length) ? acfFrom0[periodLag] : 0.0;

            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_AUTOCORRELATION_PERIOD, currentSensor, safeDouble(periodSeconds));
            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_AUTOCORRELATION_NORMALIZED_AC, currentSensor, safeDouble(periodAc));

        } catch (Exception ex) {
            log.e("extractAutocorrelationFeatures error: " + ex.getMessage());
            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_AUTOCORRELATION_PERIOD, currentSensor, Double.NaN);
            esData.addFeature(ExtraSensoryData.FeatureName.MAGNITUDE_AUTOCORRELATION_NORMALIZED_AC, currentSensor, Double.NaN);
        }
        return;
    }

    private int nextPowerOfTwo(int n) {
        int p = 1;
        while (p < n) p <<= 1;
        return p;
    }

    // ================= utilidades menores =================
    private Double safeDouble(double d) {
        if (Double.isFinite(d)) return d;
        return Double.NaN;
    }
}

