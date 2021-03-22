package application;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class LdaGibbsSampler {

    /**
     * document data (term lists)<br>
     */
    int[][] documents;

    /**
     * vocabulary size<br>
     */
    int V;

    /**
     * number of topics<br>
     */
    int K;

    /**
     * Dirichlet parameter (document--topic associations)<br>
     */
    double alpha = 2.0;

    /**
     * Dirichlet parameter (topic--term associations)<br>
     */
    double beta = 0.5;

    /**
     * topic assignments for each word.<br>
     */
    int z[][];

    /**
     * cwt[i][j] number of instances of word i (term?) assigned to topic j.<br>
     */
    int[][] nw;

    /**
     * na[i][j] number of words in document i assigned to topic j.<br>
     */
    int[][] nd;

    /**
     * nwsum[j] total number of words assigned to topic j.<br>
     */
    int[] nwsum;

    /**
     * nasum[i] total number of words in document i.<br>
     */
    int[] ndsum;

    /**
     * cumulative statistics of theta<br>
     */
    double[][] thetasum;

    /**
     * cumulative statistics of phi<br>
     */
    double[][] phisum;

    /**
     * size of statistics<br>
     */
    int numstats;

    /**
     * sampling lag (?)<br>
     */
    private static int THIN_INTERVAL = 20;

    /**
     * burn-in period<br>
     */
    private static int BURN_IN = 100;

    /**
     * max iterations<br>
     */
    private static int ITERATIONS = 1000;

    /**
     * sample lag (if -1 only one sample taken)<br>
     */
    private static int SAMPLE_LAG = 10;

    private static int dispcol = 0;

    /**
     * Initialise the Gibbs sampler with data.<br>
     *
     * @param documents
     * @param V         vocabulary size
     */
    public LdaGibbsSampler(int[][] documents, int V) {
        this.documents = documents;
        this.V = V;
    }

    /**
     * Initialisation: Must start with an assignment of observations to topics ?
     * Many alternatives are possible, I chose to perform random assignments
     * with equal probabilities<br>
     *
     * @param K number of topics
     */
    public void initialState(int K) {
        int M = documents.length;

        // initialise count variables.
        nw = new int[V][K];
        nd = new int[M][K];
        nwsum = new int[K];
        ndsum = new int[M];

        // The z_i are are initialised to values in [1,K] to determine the
        // initial state of the Markov chain.

        z = new int[M][];
        for (int m = 0; m < M; m++) {
            int N = documents[m].length;
            z[m] = new int[N];
            for (int n = 0; n < N; n++) {
                int topic = (int) (Math.random() * K);
                z[m][n] = topic;
                // number of instances of word i assigned to topic j
                nw[documents[m][n]][topic]++;
                // number of words in document i assigned to topic j.
                nd[m][topic]++;
                // total number of words assigned to topic j.
                nwsum[topic]++;
            }
            // total number of words in document i
            ndsum[m] = N;
        }
    }

    public void gibbs(int K) {
        gibbs(K, 2.0, 0.5);
    }

    /**
     * Main method: Select initial state ? Repeat a large number of times: 1.
     * Select an element 2. Update conditional on other elements. If
     * appropriate, output summary for each run.<br>
     *
     * @param K     number of topics
     * @param alpha symmetric prior parameter on document--topic associations
     * @param beta  symmetric prior parameter on topic--term associations
     */
    public void gibbs(int K, double alpha, double beta) {
        this.K = K;
        this.alpha = alpha;
        this.beta = beta;

        // init sampler statistics
        if (SAMPLE_LAG > 0) {
            thetasum = new double[documents.length][K];
            phisum = new double[K][V];
            numstats = 0;
        }

        // initial state of the Markov chain:
        initialState(K);

        System.out.println("Sampling " + ITERATIONS + " iterations with burn-in of " + BURN_IN + " (B/S=" + THIN_INTERVAL + ").");

        for (int i = 0; i < ITERATIONS; i++) {
            // for all z_i
            for (int m = 0; m < z.length; m++) {
                for (int n = 0; n < z[m].length; n++) {
                    // (z_i = z[m][n])
                    // sample from p(z_i|z_-i, w)
                    int topic = sampleFullConditional(m, n);
                    z[m][n] = topic;
                }
            }

            if ((i < BURN_IN) && (i % THIN_INTERVAL == 0)) {
                //System.out.print("B");
                dispcol++;
            }
            // display progress
            if ((i > BURN_IN) && (i % THIN_INTERVAL == 0)) {
                //System.out.print("S");
                dispcol++;
            }
            // get statistics after burn-in
            if ((i > BURN_IN) && (SAMPLE_LAG > 0) && (i % SAMPLE_LAG == 0)) {
                updateParams();
                //System.out.print("|");
                if (i % THIN_INTERVAL != 0) dispcol++;
            }
            if (dispcol >= 100) {
                //System.out.println();
                dispcol = 0;
            }
        }
        System.out.println();
    }

    /**
     * Sample a topic z_i from the full conditional distribution: p(z_i = j |
     * z_-i, w) = (n_-i,j(w_i) + beta)/(n_-i,j(.) + W * beta) * (n_-i,j(d_i) +
     * alpha)/(n_-i,.(d_i) + K * alpha) <br>
     *
     * @param m document
     * @param n word
     */
    private int sampleFullConditional(int m, int n) {

        // remove z_i from the count variables
        int topic = z[m][n];
        nw[documents[m][n]][topic]--;
        nd[m][topic]--;
        nwsum[topic]--;
        ndsum[m]--;

        // do multinomial sampling via cumulative method:
        double[] p = new double[K];
        for (int k = 0; k < K; k++) {
            p[k] = (nw[documents[m][n]][k] + beta) / (nwsum[k] + V * beta)
                    * (nd[m][k] + alpha) / (ndsum[m] + K * alpha);
        }
        // cumulate multinomial parameters
        for (int k = 1; k < p.length; k++) {
            p[k] += p[k - 1];
        }
        // scaled sample because of unnormalised p[]
        double u = Math.random() * p[K - 1];
        for (topic = 0; topic < p.length; topic++) {
            if (u < p[topic])
                break;
        }

        // add newly estimated z_i to count variables
        nw[documents[m][n]][topic]++;
        nd[m][topic]++;
        nwsum[topic]++;
        ndsum[m]++;

        return topic;
    }

    /**
     * Add to the statistics the values of theta and phi for the current state.<br>
     */
    private void updateParams() {
        for (int m = 0; m < documents.length; m++) {
            for (int k = 0; k < K; k++) {
                thetasum[m][k] += (nd[m][k] + alpha) / (ndsum[m] + K * alpha);
            }
        }
        for (int k = 0; k < K; k++) {
            for (int w = 0; w < V; w++) {
                phisum[k][w] += (nw[w][k] + beta) / (nwsum[k] + V * beta);
            }
        }
        numstats++;
    }

    /**
     * Retrieve estimated document--topic associations. If sample lag > 0 then
     * the mean value of all sampled statistics for theta[][] is taken.<br>
     *
     * @return theta multinomial mixture of document topics (M x K)
     */
    public double[][] getTheta() {
        double[][] theta = new double[documents.length][K];

        if (SAMPLE_LAG > 0) {
            for (int m = 0; m < documents.length; m++) {
                for (int k = 0; k < K; k++) {
                    theta[m][k] = thetasum[m][k] / numstats;
                }
            }

        } else {
            for (int m = 0; m < documents.length; m++) {
                for (int k = 0; k < K; k++) {
                    theta[m][k] = (nd[m][k] + alpha) / (ndsum[m] + K * alpha);
                }
            }
        }

        return theta;
    }

    /**
     * Retrieve estimated topic--word associations. If sample lag > 0 then the
     * mean value of all sampled statistics for phi[][] is taken.<br>
     *
     * @return phi multinomial mixture of topic words (K x V)
     */
    public double[][] getPhi() {
        double[][] phi = new double[K][V];
        if (SAMPLE_LAG > 0) {
            for (int k = 0; k < K; k++) {
                for (int w = 0; w < V; w++) {
                    phi[k][w] = phisum[k][w] / numstats;
                }
            }
        } else {
            for (int k = 0; k < K; k++) {
                for (int w = 0; w < V; w++) {
                    phi[k][w] = (nw[w][k] + beta) / (nwsum[k] + V * beta);
                }
            }
        }
        return phi;
    }

    /**
     * Print table of multinomial data
     *
     * @param data vector of evidence
     * @param fmax max frequency in display
     * @return the scaled histogram bin values
     */
    public static void hist(double[] data, int fmax) {

        double[] hist = new double[data.length];
        // scale maximum
        double hmax = 0;
        for (int i = 0; i < data.length; i++) {
            hmax = Math.max(data[i], hmax);
        }
        double shrink = fmax / hmax;
        for (int i = 0; i < data.length; i++) {
            hist[i] = shrink * data[i];
        }

        NumberFormat nf = new DecimalFormat("00");
        String scale = "";
        for (int i = 1; i < fmax / 10 + 1; i++) {
            scale += "    .    " + i % 10;
        }

        System.out.println("x" + nf.format(hmax / fmax) + "\t0" + scale);
        for (int i = 0; i < hist.length; i++) {
            System.out.print(i + "\t|");
            for (int j = 0; j < Math.round(hist[i]); j++) {
                if ((j + 1) % 10 == 0)
                    System.out.print("]");
                else
                    System.out.print("|");
            }
            System.out.println();
        }
    }

    /**
     * Configure the gibbs sampler<br>
     *
     * @param iterations   number of total iterations
     * @param burnIn       number of burn-in iterations
     * @param thinInterval update statistics interval
     * @param sampleLag    sample interval (-1 for just one sample at the end)
     */
    public void configure(int iterations, int burnIn, int thinInterval, int sampleLag) {
        ITERATIONS = iterations;
        BURN_IN = burnIn;
        THIN_INTERVAL = thinInterval;
        SAMPLE_LAG = sampleLag;
    }

    /**
     * Inference a new document by a pre-trained phi matrix
     *
     * @param phi pre-trained phi matrix
     * @param doc document
     * @return a p array
     */
    public static double[] inference(double alpha, double beta, double[][] phi, int[] doc) {
        int K = phi.length;
        int V = phi[0].length;
        // init

        // initialise count variables.
        int[][] nw = new int[V][K];
        int[] nd = new int[K];
        int[] nwsum = new int[K];
        int ndsum = 0;

        // The z_i are are initialised to values in [1,K] to determine the
        // initial state of the Markov chain.

        int N = doc.length;
        int[] z = new int[N];
        for (int n = 0; n < N; n++) {
            int topic = (int) (Math.random() * K);
            z[n] = topic;
            // number of instances of word i assigned to topic j
            nw[doc[n]][topic]++;
            // number of words in document i assigned to topic j.
            nd[topic]++;
            // total number of words assigned to topic j.
            nwsum[topic]++;
        }
        // total number of words in document i
        ndsum = N;
        for (int i = 0; i < ITERATIONS; i++) {
            for (int n = 0; n < z.length; n++) {

                // (z_i = z[m][n])
                // sample from p(z_i|z_-i, w)
                // remove z_i from the count variables 
                int topic = z[n];
                nw[doc[n]][topic]--;
                nd[topic]--;
                nwsum[topic]--;
                ndsum--;

                // do multinomial sampling via cumulative method
                double[] p = new double[K];
                for (int k = 0; k < K; k++) {
                    p[k] = phi[k][doc[n]]
                            * (nd[k] + alpha) / (ndsum + K * alpha);
                }
                // cumulate multinomial parameters
                for (int k = 1; k < p.length; k++) {
                    p[k] += p[k - 1];
                }
                // scaled sample because of unnormalise
                double u = Math.random() * p[K - 1];
                for (topic = 0; topic < p.length; topic++) {
                    if (u < p[topic])
                        break;
                }
                if (topic == K) {
                    throw new RuntimeException("the param K or topic is set too small");
                }
                // add newly estimated z_i to count variables 
                nw[doc[n]][topic]++;
                nd[topic]++;
                nwsum[topic]++;
                ndsum++;
                z[n] = topic;
            }
        }

        double[] theta = new double[K];

        for (int k = 0; k < K; k++) {
            theta[k] = (nd[k] + alpha) / (ndsum + K * alpha);
        }
        return theta;
    }

    public static double[] inference(double[][] phi, int[] doc) {
        return inference(2.0, 0.5, phi, doc);
    }

    static String[] shades = {"     ", ".    ", ":    ", ":.   ", "::   ", "::.  ", ":::  ", ":::. ", ":::: ", "::::.", ":::::"};

    static NumberFormat lnf = new DecimalFormat("00E0");

    /**
     * create a string representation whose gray value appears as an indicator
     * of magnitude, cf. Hinton diagrams in statistics.
     *
     * @param d   value
     * @param max maximum value
     * @return
     */
    public static String shadeDouble(double d, double max) {
        int a = (int) Math.floor(d * 10 / max + 0.5);
        if (a > 10 || a < 0) {
            String x = lnf.format(d);
            a = 5 - x.length();
            for (int i = 0; i < a; i++) {
                x += " ";
            }
            return "<" + x + ">";
        }
        return "[" + shades[a] + "]";
    }
}
