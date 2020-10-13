package codedriver.module.knowledge.lcstest;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private final int oldIndex;
    private final int newIndex;
    private int totalMatchLength;
    private boolean match;
    private Node previous;
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

    public Node getNext() {
        return next;
    }
    public Node setNext(Node next) {
        this.next = next;
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
        while(node.next != null) {
            Node current = node;
            node = node.next;
            node.previous = current;          
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
            node = node.previous;
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
