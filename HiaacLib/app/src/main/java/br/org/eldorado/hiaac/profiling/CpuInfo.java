package br.org.eldorado.hiaac.profiling;

import java.io.File;
import java.io.FileFilter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class CpuInfo {

    // current cores frequencies
    private static ArrayList<CoreFreq> mCoresFreq;

    /*
     * return current cpu usage (0 to 100) guessed from core frequencies
     */
    protected static int getCpuUsageFromFreq() {
        return getCpuUsage(getCoresUsageGuessFromFreq());
    }

    /* @return total cpu usage (from 0 to 100) from cores usage array
     * @param coresUsage must come from getCoresUsageXX().
     */
    private static int getCpuUsage(int[] coresUsage) {
        // compute total cpu usage from each core as the total cpu usage given by /proc/stat seems
        // not considering offline cores: i.e. 2 cores, 1 is offline, total cpu usage given by /proc/stat
        // is equal to remaining online core (should be remaining online core / 2).
        int cpuUsage = 0;
        if (coresUsage.length < 2) {
            return 0;
        }
        for (int i = 1; i < coresUsage.length; i++) {
            if (coresUsage[i] > 0) {
                cpuUsage += coresUsage[i];
            }
        }
        return cpuUsage / (coresUsage.length - 1);
    }

    /*
     * guess core usage using core frequency (e.g. all core at min freq => 0% usage;
     *   all core at max freq => 100%)
     *
     * This function is compatible with android oreo and later but is less precise than
     *   getCoresUsageDeprecated.
     * This function returns the current cpu usage (not the average usage since last call).
     *
     * @return array of cores usage
     *   array size = nbcore +1 as the first element is for global cpu usage
     *   array element: 0 => cpu at 0% ; 100 => cpu at 100%
     */
    private static synchronized int[] getCoresUsageGuessFromFreq() {
        initCoresFreq();
        int nbCores = mCoresFreq.size() + 1;
        int[] coresUsage = new int[nbCores];
        coresUsage[0] = 0;
        for (byte i = 0; i < mCoresFreq.size(); i++) {
            coresUsage[i + 1] = mCoresFreq.get(i).getCurUsage();
            coresUsage[0] += coresUsage[i + 1];
        }
        if (mCoresFreq.size() > 0)
            coresUsage[0] /= mCoresFreq.size();
        return coresUsage;
    }

    private static void initCoresFreq() {
        if (mCoresFreq == null) {
            int nbCores = getNbCores();
            mCoresFreq = new ArrayList<>();
            for (byte i = 0; i < nbCores; i++) {
                mCoresFreq.add(new CoreFreq(i));
            }
        }
    }

    private static int getCurCpuFreq(int coreIndex) {
        return readIntegerFile("/sys/devices/system/cpu/cpu" + coreIndex + "/cpufreq/scaling_cur_freq");
    }

    private static int getMinCpuFreq(int coreIndex) {
        return readIntegerFile("/sys/devices/system/cpu/cpu" + coreIndex + "/cpufreq/cpuinfo_min_freq");
    }

    private static int getMaxCpuFreq(int coreIndex) {
        return readIntegerFile("/sys/devices/system/cpu/cpu" + coreIndex + "/cpufreq/cpuinfo_max_freq");
    }


    // return 0 if any pb occurs
    private static int readIntegerFile(String path) {
        int ret = 0;
        try {
            try (RandomAccessFile reader = new RandomAccessFile(path, "r")) {
                String line = reader.readLine();
                ret = Integer.parseInt(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     * @return The number of cores, or 1 if failed to get result
     */
    private static int getNbCores() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by one or more digits
                return Pattern.matches("cpu\\d+", pathname.getName());
            }
        }

        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch(Exception e) {
            //Default to return 1 core
            return 1;
        }
    }


    private static class CoreFreq {
        final int num;
        int cur;
        int min;
        int max;

        CoreFreq(int num) {
            this.num = num;
            min = getMinCpuFreq(num);
            max = getMaxCpuFreq(num);
        }

        void updateCurFreq() {
            cur = getCurCpuFreq(num);
            // min & max cpu could not have been properly initialized if core was offline
            if (min == 0)
                min = getMinCpuFreq(num);
            if (max == 0)
                max = getMaxCpuFreq(num);
        }

        /* return usage from 0 to 100 */
        int getCurUsage() {
            updateCurFreq();
            int cpuUsage = 0;
            if (max - min > 0 && max > 0 && cur > 0) {
                cpuUsage = (cur - min) * 100 / (max - min);
            }
            return cpuUsage;
        }
    }

}
