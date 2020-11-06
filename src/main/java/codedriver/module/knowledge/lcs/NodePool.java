package codedriver.module.knowledge.lcs;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
/**
 * 
* @Time:2020年11月2日
* @ClassName: NodePool 
* @Description: 节点池，用于代替原来的二维数组
 */
public class NodePool {

    //private int maximumPoolSize;
    private int poolSize;
    private int largestPoolSize;
//    private int lastOldIndex;
//    private int lastNewIndex;
    private final int maximumOldIndex;
    private final int maximumNewIndex;
    /** 最大匹配长度 **/
    private int matchLength;
    
    private Queue<Node> freeNodeQueue = new LinkedList<>();
    private Map<String, Node> map = new HashMap<>();
    
    public NodePool(int maximumOldIndex, int maximumNewIndex) {
        this.maximumOldIndex = maximumOldIndex;
        this.maximumNewIndex = maximumNewIndex;
    }
    public Node getNewNode(int i, int j) {
        if(i < 0 || j < 0) {
            return null;
        }
        Node node = map.get(generateKey(i, j));
        if(node == null) {
            node = getFreeNode();
            if(node != null) {
                node.setOldIndex(i).setNewIndex(j);
            }else {
                node = new Node(i, j);
                poolSize++;
                largestPoolSize++;
            }
        }
        return node;
    }
    public Node getOldNode(int i, int j) {
        return map.get(generateKey(i, j));
    }
    public void addNode(Node node) {
        String key = generateKey(node.getOldIndex(), node.getNewIndex());
        if(!map.containsKey(key)) {
            if(matchLength < node.getTotalMatchLength()) {
                matchLength = node.getTotalMatchLength();
            }
//            System.out.print(node);
//            System.out.print("\t");
            map.put(key, node);
//            lastOldIndex = node.getOldIndex();
//            lastNewIndex = node.getNewIndex();
        }
    }
    private String generateKey(int i, int j) {
        return i + "#" + j;
    }
    
    private Node getFreeNode() {
        Node node = freeNodeQueue.poll();
        if(node == null) {
            if(garbageCollectionNode() > 0) {
                node = freeNodeQueue.poll();
            }
        }
        return node;
    }
    private int garbageCollectionNode() {
        return 0;
    }
    @Override
    public String toString() {
        return "NodePool [poolSize=" + poolSize + ", largestPoolSize=" + largestPoolSize + ", maximumOldIndex="
            + maximumOldIndex + ", maximumNewIndex=" + maximumNewIndex + ", matchLength=" + matchLength + "]";
    }

}
