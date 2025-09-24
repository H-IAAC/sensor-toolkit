package br.org.eldorado.hiaac.datacollector.controller;

public final class FFT {
    private final int logm;
    final int MAXLOGM = 20;
    private final double TWOPI = 6.283185307179586;
    private final double SQHALF = 0.7071067811865476;
    private float[][] tab;
    int brseed[] = new int[4048];

    public FFT(int nlength) {
        double dtemp = Math.log(nlength) / Math.log(2);
        if (dtemp - (int) dtemp != 0.0) {
            throw new IllegalArgumentException("FFT length must be a power of 2.");
        }
        this.logm = (int) dtemp;
        if (logm >= 4) {
            createTab(logm);
        }
    }

    public double[] calculateFFTMagnitude(double[] inputData) {
        int n = 1 << logm;
        if (inputData.length > n) {
            throw new IllegalArgumentException("Input length exceeds FFT length.");
        }

        float[] x = new float[n];
        for (int i = 0; i < inputData.length; i++) x[i] = (float) inputData[i];

        rsfft(x);

        double[] mag = new double[n / 2 + 1];
        mag[0] = x[0];
        if (n == 1) return mag;
        mag[n / 2] = Math.abs(x[n / 2]);
        for (int i = 1; i < n / 2; i++) {
            mag[i] = Math.sqrt(x[i] * x[i] + x[n - i] * x[n - i]);
        }
        return mag;
    }

    public double[] calculateFFTPower(double[] inputData) {
        double[] mag = calculateFFTMagnitude(inputData);
        for (int i = 0; i < mag.length; i++) {
            mag[i] *= mag[i];
        }
        return mag;
    }

    private void rsfft(float[] x) {
        rsrec(x, logm);
        if (logm > 1) BR_permute(x, logm);
    }

    private void rsrec(float[] x, int logm) {
        int m, m2, m4, m8, nel, n;
        int x0 = 0;
        int xr1, xr2, xi1;
        int cn = 0;
        int spcn = 0;
        int smcn = 0;
        float tmp1, tmp2;
        double ang, c, s;




        /* Check range   of logm */
        if ((logm < 0) || (logm > MAXLOGM)) {
            System.err.println("FFT length m is too big: log2(m) = " + logm + "is out of bounds [" + 0 + "," + MAXLOGM + "]");

            throw new OutOfMemoryError();
        }

        /* Compute trivial cases */

        if (logm < 2) {
            if (logm == 1) {    /* length m = 2 */
                xr2 = x0 + 1;
                tmp1 = x[x0] + x[xr2];
                x[xr2] = x[x0] - x[xr2];
                x[x0] = tmp1;
                return;
            } else if (logm == 0) return;      /* length m = 1 */
        }

        /* Compute a few constants */
        m = 1 << logm;
        m2 = m / 2;
        m4 = m2 / 2;
        m8 = m4 / 2;

        /*  Step  1 */
        xr1 = x0;
        xr2 = xr1 + m2;
        for (n = 0; n < m2; n++) {
            tmp1 = x[xr1] + x[xr2];
            x[xr2] = x[xr1] - x[xr2];
            x[xr1] = tmp1;
            xr1++;
            xr2++;
        }

        /*  Step  2        */
        xr1 = x0 + m2 + m4;
        for (n = 0; n < m4; n++) {
            x[xr1] = -x[xr1];
            xr1++;
        }

        /*  Steps 3 &  4 */
        xr1 = x0 + m2;
        xi1 = xr1 + m4;
        if (logm >= 4) {
            nel = m4 - 2;
            cn = 0;
            spcn = cn + nel;
            smcn = spcn + nel;
        }

        xr1++;
        xi1++;
        for (n = 1; n < m4; n++) {
            if (n == m8) {
                tmp1 = (float) (SQHALF * (x[xr1] + x[xi1]));
                x[xi1] = (float) (SQHALF * (x[xi1] - x[xr1]));
                x[xr1] = tmp1;
            } else {//System.out.println ("logm-4="+(logm-4));
                tmp2 = tab[logm - 4][cn++] * (x[xr1] + x[xi1]);
                tmp1 = tab[logm - 4][spcn++] * x[xr1] + tmp2;
                x[xr1] = tab[logm - 4][smcn++] * x[xi1] + tmp2;
                x[xi1] = tmp1;
            }
            xr1++;
            xi1++;
        }

        /*  Call rsrec again with half DFT length */
        rsrec(x, logm - 1);

    /* Call complex DFT routine, with quarter DFT length.
        Constants have to be recomputed, because they are static! */
        m = 1 << logm;
        m2 = m / 2;
        m4 = 3 * (m / 4);
        srrec(x, x0 + m2, x0 + m4, logm - 2);

        /* Step 5: sign change & data reordering */
        m = 1 << logm;
        m2 = m / 2;
        m4 = m2 / 2;
        m8 = m4 / 2;
        xr1 = x0 + m2 + m4;
        xr2 = x0 + m - 1;
        for (n = 0; n < m8; n++) {
            tmp1 = x[xr1];
            x[xr1++] = -x[xr2];
            x[xr2--] = -tmp1;
        }
        xr1 = x0 + m2 + 1;
        xr2 = x0 + m - 2;
        for (n = 0; n < m8; n++) {
            tmp1 = x[xr1];
            x[xr1++] = -x[xr2];
            x[xr2--] = tmp1;
            xr1++;
            xr2--;
        }
        if (logm == 2) x[3] = -x[3];
    }

    void srrec(float x[], int xr, int xi, int logm) {
        int m, m2, m4, m8, nel, n;
        // int        x0=0;
        int xr1, xr2, xi1, xi2;
        int cn, spcn, smcn, c3n, spc3n, smc3n;
        float tmp1, tmp2;
        cn = 0;
        spcn = 0;
        smcn = 0;
        c3n = 0;
        spc3n = 0;
        smc3n = 0;




        /* Check range of logm */
        if ((logm < 0) || (logm > MAXLOGM)) {
            System.err.println("FFT length m is too big: log2(m) = " + logm + "is out of bounds [" + 0 + "," + MAXLOGM + "]");

            throw new OutOfMemoryError();
        }

        /*  Compute trivial cases */
        if (logm < 3) {
            if (logm == 2) {  /* length m = 4 */
                xr2 = xr + 2;
                xi2 = xi + 2;
                tmp1 = x[xr] + x[xr2];
                x[xr2] = x[xr] - x[xr2];
                x[xr] = tmp1;
                tmp1 = x[xi] + x[xi2];
                x[xi2] = x[xi] - x[xi2];
                x[xi] = tmp1;
                xr1 = xr + 1;
                xi1 = xi + 1;
                xr2++;
                xi2++;
                tmp1 = x[xr1] + x[xr2];
                x[xr2] = x[xr1] - x[xr2];
                x[xr1] = tmp1;
                tmp1 = x[xi1] + x[xi2];
                x[xi2] = x[xi1] - x[xi2];
                x[xi1] = tmp1;
                xr2 = xr + 1;
                xi2 = xi + 1;
                tmp1 = x[xr] + x[xr2];
                x[xr2] = x[xr] - x[xr2];
                x[xr] = tmp1;
                tmp1 = x[xi] + x[xi2];
                x[xi2] = x[xi] - x[xi2];
                x[xi] = tmp1;
                xr1 = xr + 2;
                xi1 = xi + 2;
                xr2 = xr + 3;
                xi2 = xi + 3;
                tmp1 = x[xr1] + x[xi2];
                tmp2 = x[xi1] + x[xr2];
                x[xi1] = x[xi1] - x[xr2];
                x[xr2] = x[xr1] - x[xi2];
                x[xr1] = tmp1;
                x[xi2] = tmp2;
                return;
            } else if (logm == 1) { /* length m = 2 */
                xr2 = xr + 1;
                xi2 = xi + 1;
                tmp1 = x[xr] + x[xr2];
                x[xr2] = x[xr] - x[xr2];
                x[xr] = tmp1;
                tmp1 = x[xi] + x[xi2];
                x[xi2] = x[xi] - x[xi2];
                x[xi] = tmp1;
                return;
            } else if (logm == 0) return;     /* length m = 1*/
        }

        /* Compute a few constants */
        m = 1 << logm;
        m2 = m / 2;
        m4 = m2 / 2;
        m8 = m4 / 2;


        /*  Step 1 */
        xr1 = xr;
        xr2 = xr1 + m2;
        xi1 = xi;
        xi2 = xi1 + m2;

        for (n = 0; n < m2; n++) {
            tmp1 = x[xr1] + x[xr2];
            x[xr2] = x[xr1] - x[xr2];
            x[xr1] = tmp1;
            tmp2 = x[xi1] + x[xi2];
            x[xi2] = x[xi1] - x[xi2];
            x[xi1] = tmp2;
            xr1++;
            xr2++;
            xi1++;
            xi2++;
        }
        /*   Step 2  */
        xr1 = xr + m2;
        xr2 = xr1 + m4;
        xi1 = xi + m2;
        xi2 = xi1 + m4;
        for (n = 0; n < m4; n++) {
            tmp1 = x[xr1] + x[xi2];
            tmp2 = x[xi1] + x[xr2];
            x[xi1] = x[xi1] - x[xr2];
            x[xr2] = x[xr1] - x[xi2];
            x[xr1] = tmp1;
            x[xi2] = tmp2;
            xr1++;
            xr2++;
            xi1++;
            xi2++;
        }

        /*   Steps  3 & 4 */
        xr1 = xr + m2;
        xr2 = xr1 + m4;
        xi1 = xi + m2;
        xi2 = xi1 + m4;
        if (logm >= 4) {
            nel = m4 - 2;
            cn = 0;
            spcn = cn + nel;
            smcn = spcn + nel;
            c3n = smcn + nel;
            spc3n = c3n + nel;
            smc3n = spc3n + nel;
        }
        xr1++;
        xr2++;
        xi1++;
        xi2++;
        for (n = 1; n < m4; n++) {
            if (n == m8) {
                tmp1 = (float) (SQHALF * (x[xr1] + x[xi1]));
                x[xi1] = (float) (SQHALF * (x[xi1] - x[xr1]));
                x[xr1] = tmp1;
                tmp2 = (float) (SQHALF * (x[xi2] - x[xr2]));
                x[xi2] = (float) (-SQHALF * (x[xr2] + x[xi2]));
                x[xr2] = tmp2;
            } else {
                tmp2 = tab[logm - 4][cn++] * (x[xr1] + x[xi1]);
                tmp1 = tab[logm - 4][spcn++] * x[xr1] + tmp2;
                x[xr1] = tab[logm - 4][smcn++] * x[xi1] + tmp2;
                x[xi1] = tmp1;
                tmp2 = tab[logm - 4][c3n++] * (x[xr2] + x[xi2]);
                tmp1 = tab[logm - 4][spc3n++] * x[xr2] + tmp2;
                x[xr2] = tab[logm - 4][smc3n++] * x[xi2] + tmp2;
                x[xi2] = tmp1;
            }
            xr1++;
            xr2++;
            xi1++;
            xi2++;
        }
        /* Call ssrec again with half DFT length  */
        srrec(x, xr, xi, logm - 1);

   /* Call ssrec again twice with one quarter DFT length.
     Constants have to be recomputed, because they are static!*/
        m = 1 << logm;
        m2 = m / 2;
        srrec(x, xr + m2, xi + m2, logm - 2);
        m = 1 << logm;
        m4 = 3 * (m / 4);
        srrec(x, xr + m4, xi + m4, logm - 2);
    }

    private void createTab(int logm) {
        int m, m2, m4, m8, nel, n, rlogm;
        int cn, spcn, smcn, c3n, spc3n, smc3n;
        double ang, s, c;
        tab = new float[logm - 4 + 1][6 * ((1 << logm) / 4 - 2)];
        for (rlogm = logm; rlogm >= 4; rlogm--) {
            m = 1 << rlogm;
            m2 = m / 2;
            m4 = m2 / 2;
            m8 = m4 / 2;
            nel = m4 - 2;
            /* Initialize pointers */

            cn = 0;
            spcn = cn + nel;
            smcn = spcn + nel;
            c3n = smcn + nel;
            spc3n = c3n + nel;
            smc3n = spc3n + nel;


            /* Compute tables */
            for (n = 1; n < m4; n++) {
                if (n == m8) continue;
                ang = n * TWOPI / m;
                c = Math.cos(ang);
                s = Math.sin(ang);
                tab[rlogm - 4][cn++] = (float) c;
                tab[rlogm - 4][spcn++] = (float) (-(s + c));
                tab[rlogm - 4][smcn++] = (float) (s - c);

                ang = 3 * n * TWOPI / m;
                c = Math.cos(ang);
                s = Math.sin(ang);
                tab[rlogm - 4][c3n++] = (float) c;
                tab[rlogm - 4][spc3n++] = (float) (-(s + c));
                tab[rlogm - 4][smc3n++] = (float) (s - c);
            }
        }
    }

    void creatbrseed(int logm) {
        int lg2, n;
        lg2 = logm >> 1;
        n = 1 << lg2;
        if (logm != (logm >> 1) << 1) lg2++;
        brseed[0] = 0;
        brseed[1] = 1;
        for (int j = 2; j <= lg2; j++) {
            int imax = 1 << (j - 1);
            for (int i = 0; i < imax; i++) {
                brseed[i] <<= 1;
                brseed[i + imax] = brseed[i] + 1;
            }
        }
    }

    private void BR_permute(float[] x, int logm) {
        int i, j, imax, lg2, n;
        int off, fj, gno;
        float tmp;
        int xp, xq, brp;
        int x0 = 0;

        lg2 = logm >> 1;
        n = 1 << lg2;
        if (logm != (logm >> 1) << 1) lg2++;

        creatbrseed(logm);

        /*  Unshuffling   loop */
        for (off = 1; off < n; off++) {
            fj = n * brseed[off];
            i = off;
            j = fj;
            tmp = x[i];
            x[i] = x[j];
            x[j] = tmp;
            xp = i;
            brp = 1;

            for (gno = 1; gno < brseed[off]; gno++) {
                xp += n;
                j = fj + brseed[brp++];
                xq = x0 + j;
                tmp = x[xp];
                x[xp] = x[xq];
                x[xq] = tmp;
            }
        }
    }
}

