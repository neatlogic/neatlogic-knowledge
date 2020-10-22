package codedriver.module.knowledge.lcs;

import java.util.List;
import java.util.function.BiPredicate;

public class LCSUtil {
    public static <T> Node longestCommonSequence(List<T> oldList, List<T> newList, BiPredicate<T, T> biPredicate) {
        Node[][] lcs = new Node[oldList.size()][newList.size()];       
        for(int i = 0; i < oldList.size(); i++) {
            for(int j = 0; j < newList.size(); j++) {
                Node currentNode = new Node(i, j);
                lcs[i][j] = currentNode;
                if(biPredicate.test(oldList.get(i), newList.get(j))) {
                    currentNode.setTotalMatchLength(1).setMatch(true);
                    Node upperLeftNode = null;
                    if(i > 0 && j > 0) {
                        upperLeftNode = lcs[i-1][j-1];
                    }
                    if(upperLeftNode != null) {
                        currentNode.setTotalMatchLength(upperLeftNode.getTotalMatchLength() + 1).setPrevious(upperLeftNode);
                    }
                }else {
                    int left = 0;
                    int top = 0;
                    Node leftNode = null;
                    if(j > 0) {
                        leftNode = lcs[i][j-1];
                    }
                    if(leftNode != null) {
                        left = leftNode.getTotalMatchLength();
                    }
                    Node topNode = null;
                    if(i > 0) {
                        topNode = lcs[i-1][j];
                    }
                    if(topNode != null) {
                        top = topNode.getTotalMatchLength();
                    }
                    if(top >= left) {
                        currentNode.setTotalMatchLength(top).setPrevious(topNode);
                    }else {
                        currentNode.setTotalMatchLength(left).setPrevious(leftNode);
                    }
                }
            }
        }       
        return lcs[oldList.size()-1][newList.size()-1];
    }
    
    public static String wrapChangePlace(String str, List<SegmentRange> segmentList, String startMark, String endMark) {
        int count = 0;
        for(SegmentRange segmentRange : segmentList) {
            if(!segmentRange.isMatch()) {
                count++;
            }
        }
        StringBuilder stringBuilder = new StringBuilder(str.length() + count * (startMark.length() + endMark.length()));
        for(SegmentRange segmentRange : segmentList) {
            if(segmentRange.getSize() > 0) {
                if(!segmentRange.isMatch()) {
                    stringBuilder.append(startMark);
                }
                stringBuilder.append(str.substring(segmentRange.getBeginIndex(), segmentRange.getEndIndex()));
                if(!segmentRange.isMatch()) {
                    stringBuilder.append(endMark);
                }
            }           
        }
        return stringBuilder.toString();
    }
}
