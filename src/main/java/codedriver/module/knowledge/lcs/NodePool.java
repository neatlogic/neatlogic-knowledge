package codedriver.module.knowledge.lcs;

/**
 * 
* @Time:2020年11月2日
* @ClassName: NodePool 
* @Description: 节点池，用于代替原来的二维数组，使得空间复杂度由O(mn)变成O(2n)，时间复杂度还是O(mn)
 */
public class NodePool {
    private final int oldLength;
    private final int newLength;
    private Node[] evenRowsArray;
    private Node[] oddRowsArray;

    public NodePool(int oldLength, int newLength) {
        this.oldLength = oldLength;
        this.newLength = newLength;
        evenRowsArray = new Node[newLength];
        oddRowsArray = new Node[newLength];
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
            if(oldIndex % 2 == 0){
                return evenRowsArray[newIndex];
            }else{
                return oddRowsArray[newIndex];
            }
        }
        return null;
    }
    public void addNode(Node node) {
        if(rangeCheck(node.getOldIndex(), node.getNewIndex())) {
            if(node.getOldIndex() % 2 == 0){
                evenRowsArray[node.getNewIndex()] = node;
            }else{
                oddRowsArray[node.getNewIndex()] = node;
            }
        }
    }
}
