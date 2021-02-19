package codedriver.module.knowledge.lcs;

/**
 * 
* @Time:2020年11月2日
* @ClassName: NodePool 
* @Description: 节点池，用于代替原来的二维数组
 */
public class NodePool {
    private final int oldLength;
    private final int newLength;
    private Node[][] nodeTwoDimensionalArray;

    public NodePool(int oldLength, int newLength) {
        this.oldLength = oldLength;
        this.newLength = newLength;
        nodeTwoDimensionalArray = new Node[oldLength][newLength];
    }
    private boolean rangeCheck(int oldIndex, int newIndex){
        if(oldIndex < 0 || oldIndex >= oldLength){
            return false;
        }
        if(newIndex < 0 || newIndex >= newLength){
            return false;
        }
        return true;
    }
    public Node getOldNode(int oldIndex, int newIndex) {
        if(rangeCheck(oldIndex, newIndex)) {
            return nodeTwoDimensionalArray[oldIndex][newIndex];
        }
        return null;
    }
    public void addNode(Node node) {
        if(rangeCheck(node.getOldIndex(), node.getNewIndex())) {
            nodeTwoDimensionalArray[node.getOldIndex()][node.getNewIndex()] = node;
        }
    }
}
