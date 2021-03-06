package gossipLearning.models.learning.mergeable;

import gossipLearning.interfaces.models.Mergeable;
import gossipLearning.interfaces.models.Partializable;
import gossipLearning.models.learning.multiclass.MultiLogReg;
import gossipLearning.utils.SparseVector;
import gossipLearning.utils.VectorEntry;

import java.util.Set;

/**
 * This class represents a multi-class logistic regression classifier 
 * that can be merged to an other mergeable multi-class logistic regression classifier.
 * <br/><br/>
 * Required configuration parameters:<br/>
 * <ul>
 * <li>MergeableMultiLogReg.lambda - learning rate</li>
 * </ul>
 * @author István Hegedűs
 */
public class MergeableMultiLogReg extends MultiLogReg implements Mergeable<MergeableMultiLogReg>, Partializable<MergeableMultiLogReg> {
  private static final long serialVersionUID = -7800995106591726828L;

  /** @hidden */
  protected static final String PAR_LAMBDA = "MergeableMultiLogReg.lambda";
  
  /**
   * Default constructor that calls the super();
   */
  public MergeableMultiLogReg(String prefix) {
    super(prefix, PAR_LAMBDA);
  }
  
  public MergeableMultiLogReg(String prefix, String PAR_LAMBDA) {
    super(prefix, PAR_LAMBDA);
  }
  
  /**
   * Constructs an object by clones (deep copy) the specified object.
   * @param a to be cloned.
   */
  public MergeableMultiLogReg(MergeableMultiLogReg a) {
    super(a);
  }
  
  protected MergeableMultiLogReg(double lambda, double age, int numberOfClasses, SparseVector[] w, double[] distribution, double[] v, double[] bias) {
    super(lambda, age, numberOfClasses, w, distribution, v, bias);
  }
  
  public Object clone() {
    return new MergeableMultiLogReg(this);
  }
  
  @Override
  public MergeableMultiLogReg merge(MergeableMultiLogReg model) {
    //age = Math.max(age, model.age);
    for (int i = 0; i < numberOfClasses -1; i++) {
      //w[i].mul(0.5);
      //w[i].add(model.w[i], 0.5);
      //bias[i] = (bias[i] + model.bias[i]) * 0.5;
      for (VectorEntry e : model.w[i]) {
        double value = w[i].get(e.index);
        w[i].add(e.index, (e.value - value) * 0.5);
        //w[i].put(e.index, (e.value + value) / (2.0 - 1.0/age));
        //w[i].put(e.index, (e.value + value));
      }
      bias[i] = (bias[i] + model.bias[i]) * 0.5;
      //bias[i] = (bias[i] + model.bias[i]) / (2.0 - 1.0/age);
      //w[i].normalize();
    }
    return this;
  }

  @Override
  public MergeableMultiLogReg getModelPart(Set<Integer> indices) {
    return new MergeableMultiLogReg(this);
  }

}
