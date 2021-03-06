package gossipLearning.controls.bandits;

import java.util.Random;

import peersim.config.Configuration;

/**
 * This singleton class represents a multi-armed bandit machine. </br>
 * The configuration parameters:
 * <ul>
 *   <li> seed: random seed</li>
 *   <li> K: number of arms </br>mu<sub>i</sub> = 0.1 + 0.8(i-1)/(K-1)</br>
 *   (unnecessary if arms are set)</li>
 *   <li> arms: list of the expected rewards of the arms (separated by commas)</li>
 * </ul>
 * @author István Hegedűs
 */
public class Machine {
  private static final String PAR_SEED = "Bandits.seed";
  private static final String PAR_K = "Bandits.K";
  private static final String PAR_ARMS = "Bandits.arms";
  
  private Arm arms[];
  private Random r;
  private int K;
  private long plays;
  
  private static Machine instance = null;
  
  private int bestArm;
  
  private Machine(long seed, int K) {
    r = new Random(seed);
    this.K = K;
    bestArm = K-1;
    int numOfArms = K;
    double slices = numOfArms -1;
    
    arms = new Arm[numOfArms];
    int c = 0;
    for (double i = 0.0; i <= slices; i++) {
      arms[c] = new Arm((0.8 / slices) * i + 0.1);
      c++;
    }
    plays = 0;
  }
  
  private Machine(long seed, double[] mus) {
    r = new Random(seed);
    this.K = mus.length;
    arms = new Arm[K];
    double bestValue = 0.0;
    bestArm = -1;
    for (int i = 0; i < mus.length; i++) {
      arms[i] = new Arm(mus[i]);
      if (mus[i] > bestValue) {
        bestValue = mus[i];
        bestArm = i;
      }
    }
    plays = 0;
  }
  
  private Machine(String prefix) {
    long seed = Configuration.getLong(prefix + "." + PAR_SEED);
    r = new Random(seed);
    K = Configuration.getInt(prefix + "." + PAR_K);
    if (!Configuration.contains(prefix + "." + PAR_ARMS)) {
      bestArm = K-1;
      int numOfArms = K;
      double slices = numOfArms -1;
      
      arms = new Arm[numOfArms];
      int c = 0;
      for (double i = 0.0; i <= slices; i++) {
        arms[c] = new Arm((0.8 / slices) * i + 0.1);
        c++;
      }
    } else {
      String[] armParams = Configuration.getString(prefix + "." + PAR_ARMS).split(",");
      bestArm = -1;
      double bestParam = 0.0;
      arms = new Arm[armParams.length];
      for (int i = 0; i < arms.length; i++) {
        arms[i] = new Arm(Double.parseDouble(armParams[i]));
        if (arms[i].getMu() > bestParam) {
          bestParam = arms[i].getMu();
          bestArm = i;
        }
      }
    }
    //System.out.println("#ARMS: " + Arrays.toString(arms));
    plays = 0;
    //d = (0.8 / (K-1)) * 0.9;
  }
  
  /**
   * Plays the specified arm and returns the reward.
   * @param index to be played
   * @return reward
   */
  public synchronized double play(int index) {
    plays ++;
    return arms[index].play(r);
  }
  
  /**
   * Returns the number of plays on the machine.
   * @return number of plays
   */
  public long getPlays() {
    return plays;
  }
  
  /**
   * Returns the specified arm.
   * @param index to be returned
   * @return arm
   */
  public Arm getArm(int index) {
    return arms[index];
  }
  
  /**
   * Returns the number of arms.
   * @return number of arms
   */
  public int size() {
    return arms.length;
  }
  
  /**
   * Returns the fraction of the best arm plays (that has the maximal 
   * expected reward).
   * @return fraction of the best arm plays
   */
  public double getPrecision() {
    double sum = 0.0;
    for (int i = 0; i < arms.length; i++) {
      sum += arms[i].numPlays();
    }
    return arms[bestArm].numPlays() / sum;
  }
  
  /**
   * Returns the sum of the reward of arms.
   * @return sum of the rewards
   */
  public double getRewards() {
    double sum = 0.0;
    for (int i = 0; i < arms.length; i++) {
      sum += arms[i].getMu() * arms[i].numPlays();
    }
    return sum;
  }
  
  /**
   * Returns the maximal reward that could be returned (if the best arm is 
   * played only).
   * @return maximal reward
   */
  public double getMaximalReward() {
    double sum = 0.0;
    for (int i = 0; i < arms.length; i++) {
      sum += arms[i].numPlays();
    }
    return arms[bestArm].getMu() * sum;
  }
  
  /**
   * The difference between the maximal and the returned rewards.
   * @return getMaximalReward() - getRewards()
   */
  public double getRegret() {
    return getMaximalReward() - getRewards();
  }
  
  /**
   * Constructs an instance of the Machine based on the configuration file. 
   * And sets the singleton instance for the function getInstance().
   * @param prefix of this class in the configuration file
   * @return the instance of the Machine
   */
  public static Machine getInstance(String prefix) {
    instance = new Machine(prefix);
    return instance;
  }
  
  /**
   * Constructs an instance of the Machine based on the specified parameters. </br>
   * mu<sub>i</sub> = 0.1 + 0.8(i-1)/(K-1)
   * And sets the singleton instance for the function getInstance().
   * @param seed random seed
   * @param K number of arms
   * @return the instance of the Machine
   */
  public static Machine getInstance(long seed, int K) {
    instance = new Machine(seed, K);
    return instance;
  }
  
  /**
   * Constructs an instance of the Machine based on the specified parameters. 
   * And sets the singleton instance for the function getInstance().
   * @param seed random seed
   * @param mus the expected values of the arms
   * @return the instance of the Machine
   */
  public static Machine getInstance(long seed, double[] mus) {
    instance = new Machine(seed, mus);
    return instance;
  }
  
  /**
   * Returns the single instance of the Machine or null if it is not constructed.
   * @return the single instance, or null if other getInstance(...) was not 
   * called before.
   */
  public static Machine getInstance() {
    return instance;
  }
  
}
