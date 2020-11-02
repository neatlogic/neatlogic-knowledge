package codedriver.module.knowledge.lcs;

import java.util.HashMap;
import java.util.Map;
/**
 * 
* @Time:2020年11月2日
* @ClassName: NodePool 
* @Description: 节点池，用于代替原来的二维数组
 */
public class NodePool {

    private Map<String, Node> map = new HashMap<>();
    
    public Node getNode(int i, int j) {
        if(i < 0 || j < 0) {
            return null;
        }
        return map.computeIfAbsent(generateKey(i, j), k -> new Node(i, j));
    }
    
    private String generateKey(int i, int j) {
        return i + "#" + j;
    }

}
