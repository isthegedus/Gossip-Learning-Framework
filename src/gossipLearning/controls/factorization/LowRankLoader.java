package gossipLearning.controls.factorization;

import gossipLearning.evaluators.LowRankResultAggregator;
import gossipLearning.protocols.ExtractionProtocol;
import gossipLearning.protocols.LearningProtocol;
import gossipLearning.utils.AggregationResult;
import gossipLearning.utils.InstanceHolder;
import gossipLearning.utils.Matrix;
import gossipLearning.utils.SparseVector;
import gossipLearning.utils.Utils;

import java.util.Arrays;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;

/**
 * This class loads the rows of a generated matrix (A) to be factorized.<br/>
 * Matrices U and V are generated by using the butterfly technique. 
 * The diagonal elements of the S is generated from pareto distribution 
 * with parameters x_m = 1.0, alpha = 1.0. <br/>
 * The matrix to be factorized is done by A=USV^T.
 * U is nXr, V is dXr, and S is rXr matrix, where n is the number of nodes 
 * r is the rank of the matrix A, and d is the number features. So A is an 
 * nXd random matrix, with rank r.
 * @author István Hegedűs
 */
public class LowRankLoader implements Control {
  
  private static final String PAR_PIDE = "extractionProtocol";
  protected final int pidE;
  private static final String PAR_PIDLS = "learningProtocols";
  protected final int[] pidLS;
  private final String PAR_RANK = "rank";
  protected final int rank;
  private final String PAR_DIM = "dimension";
  protected final int dim;
  private final String PAR_PRINTPREC = "printPrecision";
  protected final double pareto_xm = 1.0;
  protected final double pareto_alpha = 1.0;
  
  protected final Matrix U;
  protected final Matrix V;
  protected final Matrix S;
  protected final Matrix M;
  
  public LowRankLoader(String prefix) {
    pidE = Configuration.getPid(prefix + "." + PAR_PIDE);
    String[] pidLSS = Configuration.getString(prefix + "." + PAR_PIDLS).split(",");
    pidLS = new int[pidLSS.length];
    for (int i = 0; i < pidLSS.length; i++) {
      pidLS[i] = Configuration.lookupPid(pidLSS[i]);
    }
    AggregationResult.printPrecision = Configuration.getInt(prefix + "." + PAR_PRINTPREC);
    
    rank = Configuration.getInt(prefix + "." + PAR_RANK);
    dim = Configuration.getInt(prefix + "." + PAR_DIM);
    if (dim < rank) {
      throw new RuntimeException("Dimension sould be greater or equal to rank: " + dim + " - " + rank);
    }
    
    int n = Network.size();
    // generate singular values
    S = new Matrix(rank, rank);
    //ParetoRandom er = new ParetoRandom(pareto_xm, pareto_alpha, Configuration.getLong("random.seed"));
    double[] arr = new double[rank];
    for (int i = 0; i < rank; i++) {
      //arr[i] = er.nextDouble() - pareto_xm;
      arr[i] = Utils.nextPareto(pareto_xm, pareto_alpha, CommonState.r) - pareto_xm;
    }
    Arrays.sort(arr);
    for (int i = 0; i < rank; i++) {
      S.set(i, i, arr[rank - i - 1]);
    }
    U = Utils.butterflyRandomOrthogonalMatrix(n, rank, CommonState.r);
    V = Utils.butterflyRandomOrthogonalMatrix(dim, rank, CommonState.r);
    M = U.mul(S).mul(V.transpose());
    V.transpose();
  }

  @Override
  public boolean execute() {
    // load rows of the matrix to the nodes
    InstanceHolder instanceHolder;
    SparseVector instance;
    double label = 0.0;
    for (int nId = 0; nId < Network.size(); nId++){
      instanceHolder = ((ExtractionProtocol)(Network.get(nId)).getProtocol(pidE)).getInstanceHolder();
      if (instanceHolder == null) {
        instanceHolder = new InstanceHolder(0, Network.size());
        ((ExtractionProtocol)(Network.get(nId)).getProtocol(pidE)).setInstanceHolder(instanceHolder);
      }
      instanceHolder.clear();
      instance = new SparseVector(M.getRow(nId));
      instanceHolder.add(instance, label);
    }
    // load decomposed matrices as the evalset
    for (int i = 0; i < Network.size(); i++) {
      for (int j = 0; j < pidLS.length; j++) {
        LearningProtocol protocol = (LearningProtocol)Network.get(i).getProtocol(pidLS[j]);
        ((LowRankResultAggregator)protocol.getResults()).setEvalSet(U, V, S);
      }
    }
    return false;
  }

}
