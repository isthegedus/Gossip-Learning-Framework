package gossipLearning.evaluators;

import gossipLearning.interfaces.ModelHolder;
import gossipLearning.interfaces.models.FeatureExtractor;
import gossipLearning.interfaces.models.MatrixBasedModel;
import gossipLearning.utils.InstanceHolder;
import gossipLearning.utils.SparseVector;

public class LRResultAggregator extends FactorizationResultAggregator {
  private static final long serialVersionUID = 7010094375422981942L;
  
  public LRResultAggregator(String[] modelNames, String[] evalNames) {
    super(modelNames, evalNames);
  }
  
  protected LRResultAggregator(LRResultAggregator a) {
    super(a);
  }
  
  @Override
  public Object clone() {
    return new LRResultAggregator(this);
  }
  
  @Override
  public void push(int pid, int index, int userIdx, SparseVector userModel, ModelHolder modelHolder, FeatureExtractor extractor) {
    if (modelHolder.size() == 0) {
      return;
    }
    InstanceHolder eval = extractor.extract(evalSet);
    MatrixBasedModel model = (MatrixBasedModel)modelHolder.getModel(modelHolder.size() - 1);
    modelAges[index] = model.getAge();
    for (int i = 0; i < eval.getNumberOfFeatures(); i++) {
    //for (VectorEntry entry : eval.getInstance(userIdx)) {
      double expected = eval.getInstance(userIdx).get(i);
      double predicted = model.predict(userIdx, userModel, i);
      for (int j = 0; j < evaluators[index].length; j++) {
        if (evaluators[index][j] instanceof MatrixBasedEvaluator) {
          evaluators[index][j].evaluate(expected, Math.round(predicted));
        } else {
          evaluators[index][j].evaluate(expected, predicted);
        }
      }
    }
    push(pid, index);
  }

}
