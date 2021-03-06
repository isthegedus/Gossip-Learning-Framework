package gossipLearning.models.learning.mergeable.slim;

import gossipLearning.models.learning.mergeable.MergeableMultiLogReg;
import gossipLearning.utils.SparseVector;

import java.util.Arrays;
import java.util.Set;

public class SlimMultiLogReg extends MergeableMultiLogReg {
  private static final long serialVersionUID = 2834866979500268161L;
  
  /** @hidden */
  protected static final String PAR_LAMBDA = "SlimMultiLogReg.lambda";
  
  /**
   * Default constructor that calls the super();
   */
  public SlimMultiLogReg(String prefix) {
    super(prefix, PAR_LAMBDA);
  }
  
  /**
   * Constructs an object by clones (deep copy) the specified object.
   * @param a to be cloned.
   */
  public SlimMultiLogReg(SlimMultiLogReg a) {
    super(a);
  }
  
  protected SlimMultiLogReg(double lambda, double age, int numberOfClasses, SparseVector[] w, double[] distribution, double[] v, double[] bias) {
    super(lambda, age, numberOfClasses, w, distribution, v, bias);
  }
  
  public Object clone() {
    return new SlimMultiLogReg(this);
  }
  
  @Override
  public SlimMultiLogReg merge(MergeableMultiLogReg model) {
    super.merge(model);
    return this;
  }

  @Override
  public SlimMultiLogReg getModelPart(Set<Integer> indices) {
    SparseVector[] w = new SparseVector[numberOfClasses-1];
    for (int i = 0; i < numberOfClasses-1; i++) {
      w[i] = new SparseVector(indices.size());
    }
    for (int index : indices) {
      for (int i = 0; i < numberOfClasses-1; i++) {
        w[i].add(index, this.w[i].get(index));
      }
    }
    return new SlimMultiLogReg(lambda, age, numberOfClasses, w, 
        Arrays.copyOf(distribution, distribution.length), 
        Arrays.copyOf(v, v.length), 
        Arrays.copyOf(bias, bias.length));
  }

}
