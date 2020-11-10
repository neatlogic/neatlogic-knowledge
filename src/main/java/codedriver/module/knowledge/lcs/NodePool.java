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

    private long poolSize;
    private long largestPoolSize;
    private int lastOldIndex;
    private int lastNewIndex;
    private final int maximumOldLength;
    private final int maximumNewLength;
    /** 最大匹配长度 **/
    private int matchLength;
    
    private Queue<Node> freeNodeQueue = new LinkedList<>();
    private Map<Long, Node> map = new HashMap<>();
    
    public NodePool(int maximumOldLength, int maximumNewLength) {
        this.maximumOldLength = maximumOldLength;
        this.maximumNewLength = maximumNewLength;
    }
    public Node getNewNode(int i, int j) {
        if(i < 0 || j < 0) {
            return null;
        }
        Node node = null;
        if(i > 1) {
            node = getFreeNode();
        }
        if(node != null) {
            node.setOldIndex(i).setNewIndex(j);
        }else {
            node = new Node(i, j);
            largestPoolSize++;
        }
        poolSize++;
        return node;
    }
    public Node getOldNode(int i, int j) {
        if(i < 0 || j < 0) {
            return null;
        }
        return map.get(generateKey(i, j));
    }
    public void addNode(Node node) {
        Long key = generateKey(node.getOldIndex(), node.getNewIndex());
        if(!map.containsKey(key)) {
            if(matchLength < node.getTotalMatchLength()) {
                matchLength = node.getTotalMatchLength();
            }
//            System.out.print(node);
//            System.out.print("\t");
            map.put(key, node);
            lastOldIndex = node.getOldIndex();
            lastNewIndex = node.getNewIndex();
        }
    }
    private long generateKey(long i, long j) {
        return i * maximumNewLength + j;
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
        int count = 0;
        for(int oldIndex = lastOldIndex; oldIndex >= 0; oldIndex--) {
            for(int newIndex = maximumNewLength - 1; newIndex >= 0; newIndex--) {
                if(newIndex >= lastNewIndex) {
                    if(oldIndex >= lastOldIndex - 1) {
                        continue;
                    }
                }else {
                    if(oldIndex == lastOldIndex) {
                        if(oldIndex != maximumOldLength - 1) {
                            continue;
                        }
                    }
                }
                Node node = map.get(generateKey(oldIndex, newIndex));
                if(node != null) {
                    if(node.getNextCount() == 0) {
                        map.remove(generateKey(oldIndex, newIndex));
                        Node prev = node.getPrevious();
                        if(prev != null) {
                            prev.getNextCountDecrement();                           
                        }
                        node.reset();
                        freeNodeQueue.add(node);
                        count++;
                    }
                }
            }
        }
        poolSize -= count;
        return count;
    }
    @Override
    public String toString() {
        return "NodePool [poolSize=" + poolSize + ", largestPoolSize=" + largestPoolSize + ", maximumOldLength="
            + maximumOldLength + ", maximumNewLength=" + maximumNewLength + ", matchLength=" + matchLength + "]";
    }

}
