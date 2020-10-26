package codedriver.module.knowledge.lcs;

import java.util.List;
import java.util.function.BiPredicate;
/**
 * 
* @Time:2020年10月22日
* @ClassName: LCSUtil 
* @Description: 求最长公共子序列（longest common sequence）算法工具类
 */
public class LCSUtil {
    /**
     * 
    * @Time:2020年10月22日
    * @Description: LCS算法比较 
    * @param <T>
    * @param oldList 旧数据列表
    * @param newList 新数据列表
    * @param biPredicate 每一个单元的比较逻辑
    * @return Node 返回最后一次比较结果信息
     */
    public static <T> Node LCSCompare(List<T> oldList, List<T> newList, BiPredicate<T, T> biPredicate) {
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
    /**
     * 
    * @Time:2020年10月22日
    * @Description: 对字符串不匹配的地方做标记 
    * @param str 字符串数据
    * @param segmentList 分段列表
    * @param startMark 开始标记
    * @param endMark 结束标记
    * @return String 返回一个已做标记的新字符串
     */
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