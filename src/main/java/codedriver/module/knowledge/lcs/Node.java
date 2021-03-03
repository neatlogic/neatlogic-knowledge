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
    private int oldIndex;
    /** 新数据的单元下标 **/
    private int newIndex;
    /** 统计最大匹配长度 **/
    private int totalMatchLength;
    /** 记录这次比较是否匹配 **/
    private boolean match;
    /** 上一个节点 **/
    private Node previous;
    /** 上一个节点 **/
    private Node anotherPrevious;
    /** 下一个节点 **/
    @Deprecated
    private Node next;

    private Node anotherNext;
    /** 下一个节点数量 **/
    private int nextCount;
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
    public Node setOldIndex(int oldIndex) {
        this.oldIndex = oldIndex;
        return this;
    }
    public Node setNewIndex(int newIndex) {
        this.newIndex = newIndex;
        return this;
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

    public Node setPrevious(Node previous) {
        this.previous = previous;
        if(previous != null) {
            ++previous.nextCount;
        }
        return this;
    }

    public Node getAnotherPrevious() {
        return anotherPrevious;
    }

    public Node setAnotherPrevious(Node anotherPrevious) {
        this.anotherPrevious = anotherPrevious;
        if(anotherPrevious != null) {
            ++anotherPrevious.nextCount;
        }
        return this;
    }
    @Deprecated
    public Node getNext() {
        return next;
    }
    @Deprecated
    public void setNext(Node next) {
        this.next = next;
    }

    public Node getAnotherNext() {
        return anotherNext;
    }

    public void setAnotherNext(Node anotherNext) {
        this.anotherNext = anotherNext;
    }

    public void nextCountDecrement() {
        --this.nextCount;
    }
    public int getNextCount() {
        return nextCount;
    }
    @Deprecated
    public List<SegmentPair> getSegmentPairList(){
        List<SegmentPair> resultList = new ArrayList<>();
        Node node = this;
        while(node.previous != null) {
//            System.out.println(node);
            Node current = node;
            node = node.previous;
            node.next = current;
        }
//        System.out.println(node);

        int oldPrevMatchIndex = 0;
        int newPrevMatchIndex = 0;
        if(node.match) {
            if(node.oldIndex != 0 || node.newIndex != 0) {
                SegmentPair segmentMapping = new SegmentPair(0, 0, false);
                segmentMapping.setEndIndex(node.oldIndex, node.newIndex);
                resultList.add(segmentMapping);
                oldPrevMatchIndex = node.oldIndex;
                newPrevMatchIndex = node.newIndex;
            }
        }
        SegmentPair segmentMapping = new SegmentPair(oldPrevMatchIndex, newPrevMatchIndex, node.match);
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
                segmentMapping = new SegmentPair(oldCurrentIndex, newCurrentIndex, node.match);
            }
            node = node.next;
        }while(node != null);
        segmentMapping.setEndIndex(this.oldIndex + 1, this.newIndex + 1);
        resultList.add(segmentMapping);
        return resultList;
    }
    
    public void reset() {
        this.oldIndex = 0;
        this.newIndex = 0;
        this.totalMatchLength = 0;
        this.match = false;
        this.previous = null;
        this.next = null;
        this.nextCount = 0;
    }
    @Override
    public String toString() {
        return "[" + oldIndex + "][" + newIndex + "]=" + totalMatchLength + "," + (match ? "T" : "F");
    }
}
