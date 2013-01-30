package gossipLearning.overlays;

import gossipLearning.controls.ChurnControl;
import gossipLearning.interfaces.protocols.Churnable;
import gossipLearning.messages.TManMessage;
import gossipLearning.utils.NodeDescriptor;
import gossipLearning.utils.SparseVector;

import java.io.Serializable;
import java.security.InvalidParameterException;

import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Fallible;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.extras.mj.ednewscast.CycleMessage;
import peersim.transport.Transport;

public class TMan implements EDProtocol, Linkable, Serializable, Churnable {
  private static final long serialVersionUID = 5481011536830137165L;
  private static final String PAR_CACHE = "cache";
  private static final String PAR_BASEFREQ = "baseFreq";
  
  private final int baseFreq;
  private long sessionLength = ChurnControl.INIT_SESSION_LENGTH;
  
  private NodeDescriptor me;
  private NodeDescriptor[] cache;
  private int size;
  
  public TMan(String prefix) {
    final int cachesize = Configuration.getInt(prefix + "." + PAR_CACHE);
    baseFreq = Configuration.getInt(prefix + "." + PAR_BASEFREQ);

    if (baseFreq <= 0) {
      throw (InvalidParameterException) new InvalidParameterException(
          "parameter 'baseFreq' must be >0");
    }
    cache = new NodeDescriptor[cachesize];
    size = 0;
  }
  
  protected TMan(TMan a) {
    baseFreq = a.baseFreq;
    cache = new NodeDescriptor[a.cache.length];
    System.arraycopy(a.cache, 0, cache, 0, a.cache.length);
    size = a.size;
  }
  
  @Override
  public Object clone() {
    return new TMan(this);
  }

  @Override
  public void onKill() {
    cache = null;
  }

  @Override
  public int degree() {
    return size;
  }

  @Override
  public Node getNeighbor(int i) {
    return cache[i].getNode();
  }

  @Override
  public boolean addNeighbor(Node neighbour) {
    for (int i = 0; i < size; i++) {
      if (cache[i].getNode().getID() == neighbour.getID())
        return false;
    }

    if (size < cache.length) {
      // add new neighbor
      cache[size] = new NodeDescriptor(neighbour, new SparseVector(1));
      cache[size].setSimilarity(0.0);
      // find its position
      for (int j = size; j > 0 && cache[j].compareTo(cache[j-1]) > 0; j--) {
        NodeDescriptor tmp = cache[j-1];
        cache[j-1] = cache[j];
        cache[j] = tmp;
      }
      size ++;
      return true;
    } else {
      throw new IndexOutOfBoundsException();
    }
  }

  @Override
  public boolean contains(Node neighbor) {
    for (int i = 0; i < cache.length; i++) {
      if (cache[i].getNode().getID() == neighbor.getID())
        return true;
    }
    return false;
  }

  @Override
  public void pack() {
  }

  @Override
  public void processEvent(Node node, int pid, Object event) {
    if (event instanceof TManMessage) {
      
      final TManMessage msg = (TManMessage) event;
      merge(msg.cache);
      
      if (!msg.isAnswer) {
        ((Transport) node.getProtocol(FastConfig.getTransport(pid))).send(node, msg.src, new TManMessage(me, cache, true), pid);
      }
    }

    if (event instanceof CycleMessage) {
      // sending the cache
      final Node peern = getPeer();

      // send my descriptor and neighbors to the selected node
      if (peern != null) {
        ((Transport) node.getProtocol(FastConfig.getTransport(pid))).send(node, peern, new TManMessage(me, cache, false), pid);
      }
      // add the next event to the queue with base frequency as a delay
      EDSimulator.add(baseFreq, CycleMessage.inst, node, pid);
    }
  }
  
  public long getSessionLength() {
    return sessionLength;
  }

  public void setSessionLength(long sessionLength) {
    this.sessionLength = sessionLength;
  }

  public void initSession(Node node, int protocol) {
    size = 0;
    while (degree() < cache.length) {
      int onlineNeighbor = CommonState.r.nextInt(Network.size());
      if ( Network.get(onlineNeighbor).getFailState() != Fallible.DOWN
          && Network.get(onlineNeighbor).getFailState() != Fallible.DEAD
          && Network.get(onlineNeighbor).getID() != node.getID()) {
        //System.out.println(currentNode.getID() + " addNeighbor who is UP=" + onlineNeighbor + ", state=" + ((Network.get(onlineNeighbor).getFailState() == Fallible.OK) ? "UP" : "DOWN"));
        addNeighbor(Network.get(onlineNeighbor));
      }
    }
    EDSimulator.add(0, CycleMessage.inst, node, protocol);
  }
  
  public void initializeDescriptor(Node node, SparseVector descriptor) {
    me = new NodeDescriptor(node, descriptor);
    me.setSimilarity(1.0);
  }
  
  private void merge(NodeDescriptor[] cache) {
    for (int i = 0; i < cache.length; i++) {
      NodeDescriptor desc = cache[i];
      desc.setSimilarity(desc.computeSimilarity(me));
      insert(desc);
    }
  }
  
  private boolean insert(NodeDescriptor desc) {
    boolean repair = false;
    int index = containsNode(desc.getNode());
    if (index > 0) {
      cache[index] = desc;
      if (index < size -1 && cache[index].compareTo(cache[index +1]) < 0) {
        for (int i = index; i < size -1 && cache[index].compareTo(cache[index +1]) < 0; i++) {
          NodeDescriptor tmp = cache[index];
          cache[index] = cache[index +1];
          cache[index +1] = tmp;
        }
      } else {
        repair = true;
      }
    } else if (size < cache.length) {
      cache[size] = desc;
      index = size;
      size ++;
      repair = true;
    } else if (cache[size -1].compareTo(desc) < 0) {
      cache[size -1] = desc;
      index = size -1;
      repair = true;
    }
    if (repair) {
      for (int i = index; i > 0 && cache[i].compareTo(cache[i -1]) > 0; i--) {
        NodeDescriptor tmp = cache[i];
        cache[i] = cache[i -1];
        cache[i -1] = tmp;
      }
    }
    return repair;
  }
  
  private int containsNode(Node neighbor) {
    for (int i = 0; i < cache.length; i++) {
      if (cache[i].getNode().getID() == neighbor.getID())
        return i;
    }
    return -1;
  }
  
  private Node getPeer() {
    final int d = degree();
    if (d == 0) {
      return null;
    } else {
      return cache[CommonState.r.nextInt(d)].getNode();
    }
  }

}
