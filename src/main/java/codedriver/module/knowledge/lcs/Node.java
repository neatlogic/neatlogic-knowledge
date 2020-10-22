package codedriver.module.knowledge.lcs;

import java.util.ArrayList;
import java.util.List;
/**
 * 
* @Time:2020年10月22日
* @ClassName: Node 
* @Description: 节点类，两份不同数据（A、B）对比时，会先将数据分隔成不同的单元（字符串、字符等），A数据的每一个单元都会跟B数据的每一个单元比较一次，节点对象就是用来保存某一次比较的结果信息的。
 */
public class Node {
    /** 旧数据的单元下标 **/
    private final int oldIndex;
    /** 新数据的单元下标 **/
    private final int newIndex;
    /** 统计最大匹配长度 **/
    private int totalMatchLength;
    /** 记录这次比较是否匹配 **/
    private boolean match;
    /** 上一个节点 **/
    private Node previous;
    /** 下一个节点 **/
    private Node next;
    
    public Node(int oldIndex, int newIndex) {
        this.oldIndex = oldIndex;
        this.newIndex = newIndex;
    }
    public int getOldIndex() {
        return oldIndex;
    }
    public int getNewIndex() {
        return newIndex;
    }
    public int getTotalMatchLength() {
        return totalMatchLength;
    }
    public Node setTotalMatchLength(int totalMatchLength) {
        this.totalMatchLength = totalMatchLength;
        return this;
    }
    public boolean isMatch() {
        return match;
    }
    public Node setMatch(boolean match) {
        this.match = match;
        return this;
    }

    public Node getPrevious() {
        return previous;
    }

    public void setPrevious(Node previous) {
        this.previous = previous;
    }
    public List<SegmentMapping> getSegmentMappingList(){
        List<SegmentMapping> resultList = new ArrayList<>();
        Node node = this;
        while(node.previous != null) {
            Node current = node;
            node = node.previous;
            node.next = current;          
        }

        SegmentMapping segmentMapping = new SegmentMapping(0, 0, node.match);
        int oldPrevMatchIndex = 0;
        int newPrevMatchIndex = 0;
        do {               
            if(node.match) {
                oldPrevMatchIndex = node.oldIndex;
                newPrevMatchIndex = node.newIndex;
            }
            if(node.match != segmentMapping.isMatch()) {
                int oldCurrentIndex = node.oldIndex;
                int newCurrentIndex = node.newIndex;
                if(segmentMapping.isMatch()) {
                    oldCurrentIndex = oldPrevMatchIndex + 1;
                    newCurrentIndex = newPrevMatchIndex + 1;
                }
                segmentMapping.setEndIndex(oldCurrentIndex, newCurrentIndex);
                resultList.add(segmentMapping);
                segmentMapping = new SegmentMapping(oldCurrentIndex, newCurrentIndex, node.match);
            }
            node = node.next;
        }while(node != null);
        segmentMapping.setEndIndex(this.oldIndex + 1, this.newIndex + 1);
        resultList.add(segmentMapping);
        return resultList;
    }
    @Override
    public String toString() {
        return "[" + oldIndex + "][" + newIndex + "]=" + totalMatchLength + "," + (match ? "T" : "F");
    }
}
