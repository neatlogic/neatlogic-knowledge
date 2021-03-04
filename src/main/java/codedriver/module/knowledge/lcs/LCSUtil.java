package codedriver.module.knowledge.lcs;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;

import java.util.*;
import java.util.function.BiPredicate;
/**
 * 
* @Time:2020年10月22日
* @ClassName: LCSUtil 
* @Description: 求最长公共子序列（longest common sequence）算法工具类
 */
public class LCSUtil {

    public final static String SPAN_CLASS_INSERT = "<span class='insert'>";
    public final static String SPAN_CLASS_DELETE = "<span class='delete'>";
    public final static String SPAN_END = "</span>";
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
    @Deprecated
    public static <T> Node LCSCompare_old(List<T> oldList, List<T> newList, BiPredicate<T, T> biPredicate) {
        NodePool nodePool = new NodePool(oldList.size(), newList.size());       
        for(int i = 0; i < oldList.size(); i++) {
            for(int j = 0; j < newList.size(); j++) {
                Node currentNode = new Node(i, j);
                if(biPredicate.test(oldList.get(i), newList.get(j))) {
                    currentNode.setMatch(true);
                    Node upperLeftNode = nodePool.getOldNode(i - 1, j - 1);
                    if(upperLeftNode != null) {
                        currentNode.setTotalMatchLength(upperLeftNode.getTotalMatchLength() + 1).setPrevious(upperLeftNode);
                    }else{
                        currentNode.setTotalMatchLength(1);
                    }
                }else {
                    int left = 0;
                    int top = 0;
                    Node leftNode = nodePool.getOldNode(i, j - 1);
                    if(leftNode != null) {
                        left = leftNode.getTotalMatchLength();
                    }
                    Node topNode = nodePool.getOldNode(i - 1, j);
                    if(topNode != null) {
                        top = topNode.getTotalMatchLength();
                    }
                    if(top >= left) {
                        currentNode.setTotalMatchLength(top).setPrevious(topNode);
                    }else {
                        currentNode.setTotalMatchLength(left).setPrevious(leftNode);
                    }
                }
                nodePool.addNode(currentNode);
            }
        }       
        return nodePool.getOldNode(oldList.size() - 1, newList.size() - 1);
    }
    /**
     *
     * @Time:2020年10月22日
     * @Description: LCS算法比较
     * @param <T>
     * @param source 旧数据列表
     * @param target 新数据列表
     * @return Node 返回最后一次比较结果信息
     */
    public static <T>  List<SegmentPair> LCSCompare(List<T> source, List<T> target) {
        List<SegmentPair> resultList = new ArrayList<>();
        /** 先判断，至少有一个数据列表为空的情况 **/
        if(CollectionUtils.isEmpty(source) && CollectionUtils.isEmpty(target)){
            return resultList;
        }
        if(CollectionUtils.isEmpty(source)){
            resultList.add(new SegmentPair(0, 0, 0, target.size(), false));
            return resultList;
        }
        if(CollectionUtils.isEmpty(target)){
            resultList.add(new SegmentPair(0, source.size(), 0, 0, false));
            return resultList;
        }
        /** 两个数据列表是否相同 **/
        if(Objects.equals(source, target)){
            resultList.add(new SegmentPair(0, source.size(), 0, target.size(), true));
            return resultList;
        }
        /** 再判断，两个数据列表是否有公共前缀和后缀 **/
        int prefixLength = getPrefixLength(source, target);
        if(prefixLength == source.size()){
            /** source是target的子数据列表 **/
            resultList.add(new SegmentPair(0, prefixLength, 0, prefixLength, true));
            resultList.add(new SegmentPair(prefixLength, prefixLength, prefixLength, target.size(), false));
            return resultList;
        }else if(prefixLength == target.size()){
            /** target是source的子数据列表 **/
            resultList.add(new SegmentPair(0, prefixLength, 0, prefixLength, true));
            resultList.add(new SegmentPair(prefixLength, source.size(), prefixLength, prefixLength, false));
            return resultList;
        }
        int suffixLength = getSuffixLength(source, target);
        int sourceCount = source.size() - prefixLength - suffixLength;
        int targetCount = target.size() - prefixLength - suffixLength;
        if(sourceCount == 0){
            if(prefixLength > 0) {
                resultList.add(new SegmentPair(0, prefixLength, 0, prefixLength, true));
            }
            resultList.add(new SegmentPair(prefixLength, source.size() - suffixLength, prefixLength, target.size() - suffixLength, false));
            resultList.add(new SegmentPair(source.size() - suffixLength, source.size(), target.size() - suffixLength, target.size(), true));
            return resultList;
        }
        if(targetCount == 0){
            if(prefixLength > 0) {
                resultList.add(new SegmentPair(0, prefixLength, 0, prefixLength, true));
            }
            resultList.add(new SegmentPair(prefixLength, source.size() - suffixLength, prefixLength, target.size() - suffixLength, false));
            resultList.add(new SegmentPair(source.size() - suffixLength, source.size(), target.size() - suffixLength, target.size(), true));
            return resultList;
        }
        /** 再判断，两个数据列表去掉公共前缀和后缀后，是否是包含关系 **/
        if(sourceCount > targetCount){
            int index = indexOf(source, prefixLength, sourceCount, target, prefixLength, targetCount);
            if(index != -1){
                if(prefixLength > 0) {
                    resultList.add(new SegmentPair(0, prefixLength, 0, prefixLength, true));
                }
                resultList.add(new SegmentPair(prefixLength, prefixLength + index, prefixLength, prefixLength, false));
                resultList.add(new SegmentPair(prefixLength + index, prefixLength + index + targetCount, prefixLength, prefixLength + targetCount, true));
                resultList.add(new SegmentPair(prefixLength + index + targetCount, source.size() - suffixLength, prefixLength + targetCount, target.size() - suffixLength, false));
                if(suffixLength > 0) {
                    resultList.add(new SegmentPair(source.size() - suffixLength, source.size(), target.size() - suffixLength, target.size(), true));
                }
                return resultList;
            }
        }else if(sourceCount < targetCount){
            int index = indexOf(target, prefixLength, targetCount, source, prefixLength, sourceCount);
            if(index != -1){
                if(prefixLength > 0) {
                    resultList.add(new SegmentPair(0, prefixLength, 0, prefixLength, true));
                }
                resultList.add(new SegmentPair(prefixLength, prefixLength, prefixLength, prefixLength + index, false));
                resultList.add(new SegmentPair(prefixLength, prefixLength + sourceCount, prefixLength + index, prefixLength + index + sourceCount, true));
                resultList.add(new SegmentPair(prefixLength + sourceCount, source.size() - suffixLength, prefixLength + index + sourceCount, target.size() - suffixLength, false));
                if(suffixLength > 0) {
                    resultList.add(new SegmentPair(source.size() - suffixLength, source.size(), target.size() - suffixLength, target.size(), true));
                }
                return resultList;
            }
        }
        if(prefixLength > 0){
            resultList.add(new SegmentPair(0, prefixLength, 0, prefixLength, true));
        }
//        System.out.println("执行LCS");
        /** 没有包含关系的情况下，通过LCS算法对两个数据列表去掉公共前缀和后缀后进行匹配 **/
        Node node = LCSCompare(source, prefixLength, sourceCount, target, prefixLength, targetCount);
        List<SegmentPair> segmentPairList = getSegmentPairList(node, sourceCount, targetCount);
        for(SegmentPair segmentpair : segmentPairList) {
            if(prefixLength > 0){
                segmentpair.moveRight(prefixLength);
            }
            resultList.add(segmentpair);
        }
        if(suffixLength > 0){
            resultList.add(new SegmentPair(source.size() - suffixLength, source.size(), target.size() - suffixLength, target.size(), true));
        }

//        List<SegmentRange> oldSegmentRangeList = new ArrayList<>();
//        List<SegmentRange> newSegmentRangeList = new ArrayList<>();
//        for(SegmentPair segmentpair : getSegmentPairList(node, sourceCount, targetCount)) {
//            oldSegmentRangeList.add(segmentpair.getOldSegmentRange());
//            newSegmentRangeList.add(segmentpair.getNewSegmentRange());
//        }
//
//        String oldResult = wrapChangePlace(source, prefixLength, sourceCount, oldSegmentRangeList, SPAN_CLASS_DELETE, SPAN_END);
//        String newResult = wrapChangePlace(target, prefixLength, targetCount, newSegmentRangeList, SPAN_CLASS_INSERT, SPAN_END);
        return resultList;
    }

    /**
     *
     * @Time:2020年10月22日
     * @Description: LCS算法比较
     * @param <T>
     * @param source 旧数据列表
     * @param target 新数据列表
     * @return Node 返回最后一次比较结果信息
     */
    private static <T> Node LCSCompare(List<T> source, int sourceOffset, int sourceCount, List<T> target, int targetOffset, int targetCount) {
        NodePool nodePool = new NodePool(sourceCount, targetCount);
        for(int i = sourceCount - 1; i >= 0; i--) {
            for(int j = targetCount - 1; j >= 0; j--) {
                Node currentNode = new Node(i, j);
                if(Objects.equals(source.get(sourceOffset + i), target.get(targetOffset + j))) {
                    int totalMatchLength = 1;
                    currentNode.setMatch(true);
                    Node upperLeftNode = nodePool.getOldNode(i + 1, j + 1);
                    if(upperLeftNode != null) {
                        totalMatchLength = upperLeftNode.getTotalMatchLength() + 1;
                        currentNode.setNext(upperLeftNode);
                    }
                    currentNode.setTotalMatchLength(totalMatchLength);
                    Node leftNode = nodePool.getOldNode(i, j + 1);
                    if(leftNode != null) {
                        if(totalMatchLength == leftNode.getTotalMatchLength()){
                            currentNode.setAnotherNext(leftNode);
                        }
                    }else {
                        Node topNode = nodePool.getOldNode(i + 1, j);
                        if(topNode != null) {
                            if(totalMatchLength == topNode.getTotalMatchLength()){
                                currentNode.setAnotherNext(topNode);
                            }
                        }
                    }
                }else {
                    int left = 0;
                    int top = 0;
                    Node leftNode = nodePool.getOldNode(i, j + 1);
                    if(leftNode != null) {
                        left = leftNode.getTotalMatchLength();
                    }
                    Node topNode = nodePool.getOldNode(i + 1, j);
                    if(topNode != null) {
                        top = topNode.getTotalMatchLength();
                    }
                    if(top > left) {
                        currentNode.setTotalMatchLength(top).setNext(topNode);
                    }else if(left > top) {
                        currentNode.setTotalMatchLength(left).setNext(leftNode);
                    }else {
                        currentNode.setTotalMatchLength(left).setNext(leftNode);
                    }
                }
                nodePool.addNode(currentNode);
            }
        }
        return nodePool.getOldNode(0, 0);
    }

    /**
     * 
    * @Time:2020年11月02日
    * @Description: LCS算法比较字符串 
    * @param oldStr 旧字符串
    * @param newStr 新字符串
    * @return Node 返回最后一次比较结果信息
     */
    @Deprecated
    public static Node LCSCompare_old(String oldStr, String newStr) {
//        System.out.println(oldStr);
//        System.out.println(newStr);
//        System.out.println("--------------------------------------------------------------------------");
        NodePool nodePool = new NodePool(oldStr.length(), newStr.length());       
        for(int i = 0; i < oldStr.length(); i++) {
            for(int j = 0; j < newStr.length(); j++) {
                Node currentNode = new Node(i, j);
                if(oldStr.charAt(i) == newStr.charAt(j)) {
                    currentNode.setTotalMatchLength(1).setMatch(true);
                    Node upperLeftNode = nodePool.getOldNode(i - 1, j - 1);
                    if(upperLeftNode != null) {
                        currentNode.setTotalMatchLength(upperLeftNode.getTotalMatchLength() + 1).setPrevious(upperLeftNode);
                    }
                }else {
                    int left = 0;
                    int top = 0;
                    Node leftNode = nodePool.getOldNode(i, j - 1);
                    if(leftNode != null) {
                        left = leftNode.getTotalMatchLength();
                    }
                    Node topNode = nodePool.getOldNode(i - 1, j);
                    if(topNode != null) {
                        top = topNode.getTotalMatchLength();
                    }
                    if(top >= left) {
                        currentNode.setTotalMatchLength(top).setPrevious(topNode);
                    }else {
                        currentNode.setTotalMatchLength(left).setPrevious(leftNode);
                    }
                }
//                System.out.print(currentNode);
//                System.out.print("\t");
                nodePool.addNode(currentNode);
            }
//            System.out.println();
        }
//        System.out.println("===============================================================================");
        return nodePool.getOldNode(oldStr.length() - 1, newStr.length() - 1);
    }

    /**
     * @Description: 统计两个字符串的最大匹配长度
     * @Author: linbq
     * @Date: 2021/3/1 16:30
     * @Params:[source, target]
     * @Returns:int
     **/
    public static int getTotalMatchLength(String source, String target) {
        /** 先判断，至少有一个字符串为空的情况 **/
        if(StringUtils.isEmpty(source) && StringUtils.isEmpty(target)){
            return 0;
        }
        if(StringUtils.isEmpty(source)){
            return 0;
        }
        if(StringUtils.isEmpty(target)){
            return 0;
        }
        /** 两个字符串是否相同 **/
        if(Objects.equals(source, target)){
            return source.length();
        }
        /** 再判断，两个字符串是否有公共前缀和后缀 **/
        int prefixLength = getPrefixLength(source, target);
        if(prefixLength == source.length()){
            /** source是target的子字符串 **/
            return prefixLength;
        }else if(prefixLength == target.length()){
            /** target是source的子字符串 **/
            return prefixLength;
        }
        int suffixLength = getSuffixLength(source, target);
        int sourceCount = source.length() - prefixLength - suffixLength;
        int targetCount = target.length() - prefixLength - suffixLength;
        if(sourceCount == 0){
            return source.length();
        }
        if(targetCount == 0){
            return target.length();
        }
        /** 再判断，两个字符串去掉公共前缀和后缀后，是否是包含关系 **/
        if(sourceCount > targetCount){
            int index = indexOf(source, prefixLength, sourceCount, target, prefixLength, targetCount);
            if(index != -1){
                return prefixLength + targetCount + suffixLength;
            }
        }else if(sourceCount < targetCount){
            int index = indexOf(target, prefixLength, targetCount, source, prefixLength, sourceCount);
            if(index != -1){
                return prefixLength + sourceCount + suffixLength;
            }
        }
        /** 没有包含关系的情况下，通过LCS算法对两个字符串去掉公共前缀和后缀后进行匹配 **/
        Node node = LCSCompare(source, prefixLength, sourceCount, target, prefixLength, targetCount);
        return prefixLength + node.getTotalMatchLength() + suffixLength;
    }

//    /**
//     *
//     * @Time:2020年11月02日
//     * @Description: LCS算法比较字符串
//     * @param source 旧字符串
//     * @param target 新字符串
//     * @return Node 返回最后一次比较结果信息
//     */
//    public static String[] LCSCompare(String source, String target) {
//        PrintSingeColorFormatUtil.println("-----------------------------------");
//        /** 先判断，至少有一个字符串为空的情况 **/
//        if(StringUtils.isEmpty(source) && StringUtils.isEmpty(target)){
//            PrintSingeColorFormatUtil.println(source);
//            PrintSingeColorFormatUtil.println(target);
//            return new String[]{source, target};
//        }
//        if(StringUtils.isEmpty(source)){
//            PrintSingeColorFormatUtil.println(source);
//            return new String[]{source, wrapChangePlace(target, SPAN_CLASS_INSERT, SPAN_END)};
//        }
//        if(StringUtils.isEmpty(target)){
//            String sourceResult = wrapChangePlace(source, SPAN_CLASS_DELETE, SPAN_END);
//            PrintSingeColorFormatUtil.println(target);
//            return new String[]{sourceResult, target};
//        }
//        /** 两个字符串是否相同 **/
//        if(Objects.equals(source, target)){
//            PrintSingeColorFormatUtil.println(source);
//            PrintSingeColorFormatUtil.println(target);
//            return new String[]{source, target};
//        }
//        /** 再判断，两个字符串是否有公共前缀和后缀 **/
//        int prefixLength = getPrefixLength(source, target);
//        if(prefixLength == source.length()){
//            /** source是target的子字符串 **/
//            PrintSingeColorFormatUtil.println(source);
//            return new String[]{source, wrapChangePlace(target, prefixLength, SPAN_CLASS_INSERT, SPAN_END)};
//        }else if(prefixLength == target.length()){
//            /** target是source的子字符串 **/
//            String sourceResult = wrapChangePlace(source, prefixLength, SPAN_CLASS_DELETE, SPAN_END);
//            PrintSingeColorFormatUtil.println(target);
//            return new String[]{sourceResult, target};
//        }
//        int suffixLength = getSuffixLength(source, target);
//        int sourceCount = source.length() - prefixLength - suffixLength;
//        int targetCount = target.length() - prefixLength - suffixLength;
//        if(sourceCount == 0){
//            PrintSingeColorFormatUtil.println(source);
//            return new String[]{source, wrapChangePlace(target, prefixLength, suffixLength, SPAN_CLASS_INSERT, SPAN_END)};
//        }
//        if(targetCount == 0){
//            String sourceResult = wrapChangePlace(source, prefixLength, suffixLength, SPAN_CLASS_DELETE, SPAN_END);
//            PrintSingeColorFormatUtil.println(target);
//            return new String[]{sourceResult, target};
//        }
//        /** 再判断，两个字符串去掉公共前缀和后缀后，是否是包含关系 **/
//        if(sourceCount > targetCount){
//            int index = indexOf(source, prefixLength, sourceCount, target, prefixLength, targetCount);
//            if(index != -1){
//                String sourceResult = wrapChangePlace(source, prefixLength, index, targetCount, suffixLength, SPAN_CLASS_DELETE, SPAN_END);
//                PrintSingeColorFormatUtil.println(target);
//                return new String[]{sourceResult, target};
//            }
//        }else if(sourceCount < targetCount){
//            int index = indexOf(target, prefixLength, targetCount, source, prefixLength, sourceCount);
//            if(index != -1){
//                PrintSingeColorFormatUtil.println(source);
//                return new String[]{source, wrapChangePlace(target, prefixLength, index, sourceCount, suffixLength, SPAN_CLASS_INSERT, SPAN_END)};
//            }
//        }
////        System.out.println("执行LCS");
//        /** 没有包含关系的情况下，通过LCS算法对两个字符串去掉公共前缀和后缀后进行匹配 **/
//        Node node = LCSCompare(source, prefixLength, sourceCount, target, prefixLength, targetCount);
//
//        List<SegmentRange> oldSegmentRangeList = new ArrayList<>();
//        List<SegmentRange> newSegmentRangeList = new ArrayList<>();
//        for(SegmentPair segmentpair : getSegmentPairList(node, sourceCount, targetCount)) {
////            oldSegmentRangeList.add(segmentpair.getOldSegmentRange());
////            newSegmentRangeList.add(segmentpair.getNewSegmentRange());
//            oldSegmentRangeList.add(new SegmentRange(segmentpair.getOldBeginIndex(), segmentpair.getOldEndIndex(), segmentpair.isMatch()));
//            newSegmentRangeList.add(new SegmentRange(segmentpair.getNewBeginIndex(), segmentpair.getNewEndIndex(), segmentpair.isMatch()));
//        }
//
//        String oldResult = wrapChangePlace(source, prefixLength, sourceCount, oldSegmentRangeList, SPAN_CLASS_DELETE, SPAN_END);
//        String newResult = wrapChangePlace(target, prefixLength, targetCount, newSegmentRangeList, SPAN_CLASS_INSERT, SPAN_END);
//        return new String[]{oldResult, newResult};
//    }
    /**
     *
     * @Time:2020年11月02日
     * @Description: LCS算法比较字符串
     * @param source 旧字符串
     * @param target 新字符串
     * @return Node 返回最后一次比较结果信息
     */
    public static List<SegmentPair> LCSCompare(String source, String target) {
        PrintSingeColorFormatUtil.println("-----------------------------------");
        List<SegmentPair> resultList = new ArrayList<>();
        /** 先判断，至少有一个字符串为空的情况 **/
        if(StringUtils.isEmpty(source) && StringUtils.isEmpty(target)){
            return resultList;
        }
        if(StringUtils.isEmpty(source)){
            resultList.add(new SegmentPair(0, 0, 0, target.length(), false));
            return resultList;
        }
        if(StringUtils.isEmpty(target)){
            resultList.add(new SegmentPair(0, source.length(), 0, 0, false));
            return resultList;
        }
        /** 两个字符串是否相同 **/
        if(Objects.equals(source, target)){
            resultList.add(new SegmentPair(0, source.length(), 0, target.length(), true));
            return resultList;
        }
        /** 再判断，两个字符串是否有公共前缀和后缀 **/
        int prefixLength = getPrefixLength(source, target);
        if(prefixLength == source.length()){
            /** source是target的子字符串 **/
            resultList.add(new SegmentPair(0, prefixLength, 0, prefixLength, true));
            resultList.add(new SegmentPair(prefixLength, prefixLength, prefixLength, target.length(), false));
            return resultList;
        }else if(prefixLength == target.length()){
            /** target是source的子字符串 **/
            resultList.add(new SegmentPair(0, prefixLength, 0, prefixLength, true));
            resultList.add(new SegmentPair(prefixLength, source.length(), prefixLength, prefixLength, false));
            return resultList;
        }
        int suffixLength = getSuffixLength(source, target);
        int sourceCount = source.length() - prefixLength - suffixLength;
        int targetCount = target.length() - prefixLength - suffixLength;
        if(sourceCount == 0){
            if(prefixLength > 0){
                resultList.add(new SegmentPair(0, prefixLength, 0, prefixLength, true));
            }
            resultList.add(new SegmentPair(prefixLength, source.length() - suffixLength, prefixLength, target.length() - suffixLength, false));
            resultList.add(new SegmentPair(source.length() - suffixLength, source.length(), target.length() - suffixLength, target.length(), true));
            return resultList;
        }
        if(targetCount == 0){
            if(prefixLength > 0){
                resultList.add(new SegmentPair(0, prefixLength, 0, prefixLength, true));
            }
            resultList.add(new SegmentPair(prefixLength, source.length() - suffixLength, prefixLength, target.length() - suffixLength, false));
            resultList.add(new SegmentPair(source.length() - suffixLength, source.length(), target.length() - suffixLength, target.length(), true));
            return resultList;
        }
        /** 再判断，两个字符串去掉公共前缀和后缀后，是否是包含关系 **/
        if(sourceCount > targetCount){
            int index = indexOf(source, prefixLength, sourceCount, target, prefixLength, targetCount);
            if(index != -1){
                if(prefixLength > 0) {
                    resultList.add(new SegmentPair(0, prefixLength, 0, prefixLength, true));
                }
                resultList.add(new SegmentPair(prefixLength, prefixLength + index, prefixLength, prefixLength, false));
                resultList.add(new SegmentPair(prefixLength + index, prefixLength + index + targetCount, prefixLength, prefixLength + targetCount, true));
                resultList.add(new SegmentPair(prefixLength + index + targetCount, source.length() - suffixLength, prefixLength + targetCount, target.length() - suffixLength, false));
                if(suffixLength > 0) {
                    resultList.add(new SegmentPair(source.length() - suffixLength, source.length(), target.length() - suffixLength, target.length(), true));
                }
                return resultList;
            }
        }else if(sourceCount < targetCount){
            int index = indexOf(target, prefixLength, targetCount, source, prefixLength, sourceCount);
            if(index != -1){
                if(prefixLength > 0) {
                    resultList.add(new SegmentPair(0, prefixLength, 0, prefixLength, true));
                }
                resultList.add(new SegmentPair(prefixLength, prefixLength, prefixLength, prefixLength + index, false));
                resultList.add(new SegmentPair(prefixLength, prefixLength + sourceCount, prefixLength + index, prefixLength + index + sourceCount, true));
                resultList.add(new SegmentPair(prefixLength + sourceCount, source.length() - suffixLength, prefixLength + index + sourceCount, target.length() - suffixLength, false));
                if(suffixLength > 0) {
                    resultList.add(new SegmentPair(source.length() - suffixLength, source.length(), target.length() - suffixLength, target.length(), true));
                }
                return resultList;
            }
        }
        if(prefixLength > 0){
            resultList.add(new SegmentPair(0, prefixLength, 0, prefixLength, true));
        }
//        System.out.println("执行LCS");
        /** 没有包含关系的情况下，通过LCS算法对两个字符串去掉公共前缀和后缀后进行匹配 **/
        Node node = LCSCompare(source, prefixLength, sourceCount, target, prefixLength, targetCount);
        List<SegmentPair> segmentPairList = getSegmentPairList(node, sourceCount, targetCount);
        for(SegmentPair segmentpair : segmentPairList) {
            if(prefixLength > 0){
                segmentpair.moveRight(prefixLength);
            }
            resultList.add(segmentpair);
        }
        if(suffixLength > 0){
            resultList.add(new SegmentPair(source.length() - suffixLength, source.length(), target.length() - suffixLength, target.length(), true));
        }
        return resultList;
    }

    private static Node LCSCompare(String source, int sourceOffset, int sourceCount, String target, int targetOffset, int targetCount) {
        System.out.println(source.substring(sourceOffset, sourceOffset + sourceCount));
        System.out.println(target.substring(targetOffset, targetOffset + targetCount));
//        Node[][] nodes = new Node[sourceCount][targetCount];
//        System.out.println("--------------------------------------------------------------------------");
        NodePool nodePool = new NodePool(sourceCount, targetCount);
        for(int i = sourceCount - 1; i >= 0; i--) {
            for(int j = targetCount - 1; j >= 0; j--) {
                Node currentNode = new Node(i, j);
                if(source.charAt(sourceOffset + i) == target.charAt(targetOffset + j)) {
                    int totalMatchLength = 1;
                    currentNode.setMatch(true);
                    Node upperLeftNode = nodePool.getOldNode(i + 1, j + 1);
                    if(upperLeftNode != null) {
                        totalMatchLength = upperLeftNode.getTotalMatchLength() + 1;
                        currentNode.setNext(upperLeftNode);
                    }
                    currentNode.setTotalMatchLength(totalMatchLength);
                    Node leftNode = nodePool.getOldNode(i, j + 1);
                    if(leftNode != null) {
                        if(totalMatchLength == leftNode.getTotalMatchLength()){
                            currentNode.setAnotherNext(leftNode);
                        }
                    }else {
                        Node topNode = nodePool.getOldNode(i + 1, j);
                        if(topNode != null) {
                            if(totalMatchLength == topNode.getTotalMatchLength()){
                                currentNode.setAnotherNext(topNode);
                            }
                        }
                    }
                }else {
                    int left = 0;
                    int top = 0;
                    Node leftNode = nodePool.getOldNode(i, j + 1);
                    if(leftNode != null) {
                        left = leftNode.getTotalMatchLength();
                    }
                    Node topNode = nodePool.getOldNode(i + 1, j);
                    if(topNode != null) {
                        top = topNode.getTotalMatchLength();
                    }
                    if(top > left) {
                        currentNode.setTotalMatchLength(top).setNext(topNode);
                    }else if(left > top) {
                        currentNode.setTotalMatchLength(left).setNext(leftNode);
                    }else {
                        currentNode.setTotalMatchLength(left).setNext(leftNode);
//                        if(top > 0 ){
//                            currentNode.setAnotherPrevious(topNode);
//                        }
                    }
                }
//                nodes[currentNode.getOldIndex()][currentNode.getNewIndex()] = currentNode;
//                System.out.print(currentNode);
//                System.out.print("\t");
                nodePool.addNode(currentNode);
            }
//            System.out.println();
        }
//        for (int i = 0; i < sourceCount; i++) {
//            for (int j = 0; j < targetCount; j++) {
//                System.out.print(nodes[i][j]);
//                System.out.print("\t");
//            }
//            System.out.println();
//        }
//        System.out.println("===============================================================================");
        return nodePool.getOldNode(0, 0);
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
        int mismatchCount = 0;
        for(SegmentRange segmentRange : segmentList) {
            if(!segmentRange.isMatch() && segmentRange.getSize() > 0) {
                mismatchCount++;
            }
        }
        int capacity = str.length() + mismatchCount * (startMark.length() + endMark.length());
        StringBuilder stringBuilder = new StringBuilder(capacity);
        for(SegmentRange segmentRange : segmentList) {
            if(segmentRange.getSize() > 0) {
                if(!segmentRange.isMatch()) {
                    stringBuilder.append(startMark);
                }
                for(int i = segmentRange.getBeginIndex(); i < segmentRange.getEndIndex(); i++){
                    stringBuilder.append(str.charAt(i));
                    if(segmentRange.isMatch()) {
                        PrintSingeColorFormatUtil.print(str.charAt(i));
                    }else{
                        PrintSingeColorFormatUtil.print(str.charAt(i), startMark);
                    }
                }
                if(!segmentRange.isMatch()) {
                    stringBuilder.append(endMark);
                }
            }
        }
        PrintSingeColorFormatUtil.println();
        String result = stringBuilder.toString();
        if(result.length() != capacity){
            System.out.println("wrapChangePlace:result.length()" + result.length() + " != " + capacity + "capacity");
        }
        return result;
    }

//    /**
//     *
//    * @Time:2020年10月22日
//    * @Description: 对字符串不匹配的地方做标记
//    * @param source 字符串数据
//    * @param segmentList 分段列表
//    * @param startMark 开始标记
//    * @param endMark 结束标记
//    * @return String 返回一个已做标记的新字符串
//     */
//    private static String wrapChangePlace(String source, int offset, int count, List<SegmentRange> segmentList, String startMark, String endMark) {
//        int mismatchCount = 0;
//        for(SegmentRange segmentRange : segmentList) {
//            if(!segmentRange.isMatch() && segmentRange.getSize() > 0) {
//                mismatchCount++;
//            }
//        }
//        int capacity = source.length() + mismatchCount * (startMark.length() + endMark.length());
//        StringBuilder stringBuilder = new StringBuilder(capacity);
//        for(int i = 0; i < offset; i++){
//            stringBuilder.append(source.charAt(i));
//            PrintSingeColorFormatUtil.print(source.charAt(i));
//        }
//        for(SegmentRange segmentRange : segmentList) {
//            if(segmentRange.getSize() > 0) {
//                if(!segmentRange.isMatch()) {
//                    stringBuilder.append(startMark);
//                }
//                for(int i = segmentRange.getBeginIndex() + offset; i < segmentRange.getEndIndex() + offset; i++){
//                    stringBuilder.append(source.charAt(i));
//                    if(segmentRange.isMatch()) {
//                        PrintSingeColorFormatUtil.print(source.charAt(i));
//                    }else{
//                        PrintSingeColorFormatUtil.print(source.charAt(i), startMark);
//                    }
//                }
//                if(!segmentRange.isMatch()) {
//                    stringBuilder.append(endMark);
//                }
//            }
//        }
//
//        for(int i = offset + count; i < source.length(); i++){
//            stringBuilder.append(source.charAt(i));
//            PrintSingeColorFormatUtil.print(source.charAt(i));
//        }
//        String result = stringBuilder.toString();
//        if(result.length() != capacity){
//            System.out.println("wrapChangePlace:result.length()" + result.length() + " != " + capacity + "capacity");
//        }
//        PrintSingeColorFormatUtil.println();
//        return result;
//    }
//    private static String wrapChangePlace(String source, String startMark, String endMark){
//        return wrapChangePlace(source, 0, 0, source.length(), 0, startMark, endMark);
//    }
//    private static String wrapChangePlace(String source, int prefixLength, String startMark, String endMark){
//        return wrapChangePlace(source, prefixLength, 0, source.length() - prefixLength, 0, startMark, endMark);
//    }
//    private static String wrapChangePlace(String source, int prefixLength, int suffixLength,String startMark, String endMark){
//        return wrapChangePlace(source, prefixLength, 0, source.length() - prefixLength - suffixLength, suffixLength, startMark, endMark);
//    }
//    private static String wrapChangePlace(String source, int prefixLength, int index, int count, int suffixLength, String startMark, String endMark){
//        int sourceCount = source.length() - prefixLength - suffixLength;
//        int startIndex = prefixLength + index;
//        int endIndex = startIndex + count;
//        int capacity = startMark.length() + source.length() + endMark.length();
//        if(count < sourceCount){
//            capacity += startMark.length() + endMark.length();
//        }
//        StringBuilder targetBuilder = new StringBuilder(capacity);
//        for (int i = 0; i < prefixLength; i++) {
//            targetBuilder.append(source.charAt(i));
//            PrintSingeColorFormatUtil.print(source.charAt(i));
//        }
//        if(index != -1){
//            targetBuilder.append(startMark);
//            if(count < sourceCount){
//                for (int i = prefixLength; i < startIndex; i++) {
//                    targetBuilder.append(source.charAt(i));
//                    PrintSingeColorFormatUtil.print(source.charAt(i), startMark);
//                }
//                targetBuilder.append(endMark);
//            }
//            for (int i = startIndex; i < endIndex; i++) {
//                targetBuilder.append(source.charAt(i));
//                if(count == sourceCount){
//                    PrintSingeColorFormatUtil.print(source.charAt(i), startMark);
//                }else{
//                    PrintSingeColorFormatUtil.print(source.charAt(i));
//                }
//            }
//            if(count < sourceCount){
//                targetBuilder.append(startMark);
//                for (int i = endIndex; i < source.length() - suffixLength; i++) {
//                    targetBuilder.append(source.charAt(i));
//                    PrintSingeColorFormatUtil.print(source.charAt(i), startMark);
//                }
//            }
//            targetBuilder.append(endMark);
//        }
//
//        for (int i = source.length() - suffixLength; i < source.length(); i++) {
//            targetBuilder.append(source.charAt(i));
//            PrintSingeColorFormatUtil.print(source.charAt(i));
//        }
//        PrintSingeColorFormatUtil.println();
//        String result = targetBuilder.toString();
//        if(result.length() != capacity){
//            System.out.println("wrapChangePlace:result.length()" + result.length() + " != " + capacity + "capacity");
//        }
//        return result;
//    }

    @Deprecated
    private static List<SegmentPair> getSegmentPairList_old(Node lastNode){
        int maxOldIndex = lastNode.getOldIndex();
        int maxNewIndex = lastNode.getNewIndex();
        /** 在保证公共子序列匹配度最高的情况下，找出所有匹配方案 **/
        List<List<SegmentPair>> segmentPairListList = new ArrayList<>();
        Stack<Node> stack = new Stack<>();
        stack.push(lastNode);
        while(!stack.empty()){
            Node node = stack.pop();
            while(node.getPrevious() != null) {
//            System.out.println(node);
                Node current = node;
                //current.setNext(null);
                node = node.getPrevious();
                if(current.getAnotherPrevious() != null){
                    current.setPrevious(current.getAnotherPrevious());
                    current.setAnotherPrevious(null);
                    stack.push(current);
                }
                node.setNext(current);
            }
            segmentPairListList.add(getSegmentPairList_old(node, maxOldIndex, maxNewIndex));
        }
        /** 所有匹配方案中，找出分隔段数最小的方案作为最终匹配方案 **/
        int minSize = Integer.MAX_VALUE;
        List<SegmentPair> resultList = null;
        for(List<SegmentPair> list : segmentPairListList){
            if(list.size() < minSize){
                resultList = list;
                minSize = list.size();
            }
        }
        return resultList;
    }
    @Deprecated
    private static List<SegmentPair> getSegmentPairList_old(Node node, int maxOldIndex, int maxNewIndex){
//        System.out.println("++++++++++++++++++++++++++++++++");
        List<SegmentPair> trueSegmentPairList = new ArrayList<>();
        int prevMatchOldIndex = -1;
        int prevMatchNewIndex = -1;
        SegmentPair segmentPair = null;
        boolean lastIsMatch = false;
        do {
//            System.out.println(node);
            if(node.isMatch() && node.getOldIndex() > prevMatchOldIndex && node.getNewIndex() > prevMatchNewIndex){
                if(lastIsMatch){
                    segmentPair.setEndIndex(node.getOldIndex() + 1, node.getNewIndex() + 1);
                }else{
                    if(segmentPair != null) {
                        trueSegmentPairList.add(segmentPair);
                    }
                    segmentPair = new SegmentPair(node.getOldIndex(), node.getOldIndex() + 1, node.getNewIndex(), node.getNewIndex() + 1, true);
                }
                prevMatchOldIndex = node.getOldIndex();
                prevMatchNewIndex = node.getNewIndex();
            }
            lastIsMatch = node.isMatch();
            node = node.getNext();
        }while(node != null);
        trueSegmentPairList.add(segmentPair);
//        trueList.forEach(System.out::println);
        List<SegmentPair> resultList = new ArrayList<>();
        int oldBeginIndex = 0;
        int newBeginIndex = 0;
        for(SegmentPair trueSegmentPair : trueSegmentPairList){
            if(oldBeginIndex != trueSegmentPair.getOldBeginIndex() || newBeginIndex != trueSegmentPair.getNewBeginIndex()){
                resultList.add(new SegmentPair(oldBeginIndex, trueSegmentPair.getOldBeginIndex(), newBeginIndex, trueSegmentPair.getNewBeginIndex(), false));
            }
            oldBeginIndex = trueSegmentPair.getOldEndIndex();
            newBeginIndex = trueSegmentPair.getNewEndIndex();
            resultList.add(trueSegmentPair);
        }
        if(oldBeginIndex != maxOldIndex + 1 || newBeginIndex != maxNewIndex + 1){
            resultList.add(new SegmentPair(oldBeginIndex, maxOldIndex + 1, newBeginIndex, maxNewIndex + 1, false));
        }
//        resultList.forEach(System.out::println);
//        System.out.println("++++++++++++++++++++++++++++++++");
        return resultList;
    }

    public static List<SegmentPair> getSegmentPairList(Node firstNode, int sourceCount, int targetCount){
        /** 在保证公共子序列匹配度最高的情况下，找出所有匹配方案 **/
        List<Node> matchNodeList = new ArrayList<>();
        Stack<Node> nodeStack = new Stack<>();
        Stack<Node> previousNodeStack = new Stack<>();
        Stack<List<Node>> matchNodeListStack = new Stack<>();
        Stack<Integer> segmentCountStack = new Stack<>();
        Node node = firstNode;
        Node previousNode = null;
        int minSegmentCount = Integer.MAX_VALUE;
        List<Node> minMatchNodeList = new ArrayList<>();
        int segmentCount = 0;
        int count = 0;
        while(true){
            do{
                if(node.isMatch()){
                    if(matchNodeList.isEmpty() || matchNodeList.get(matchNodeList.size() - 1).getTotalMatchLength() > node.getTotalMatchLength()){
                        matchNodeList.add(node);
                    }
                }
                if(previousNode == null){
                    segmentCount++;
                }else if(node.isMatch()){
                    if(previousNode.isMatch()){
                        if(matchNodeList.contains(previousNode) ^ matchNodeList.contains(node)){
                            segmentCount++;
                        }
                    }else{
                        if(matchNodeList.contains(node)){
                            segmentCount++;
                        }
                    }
                }else{
                    if(previousNode.isMatch()){
                        if(matchNodeList.contains(previousNode)){
                            segmentCount++;
                        }
                    }
                }
                if(segmentCount >= minSegmentCount){
                    break;
                }
                if(node.getAnotherNext() != null){
                    nodeStack.push(node.getAnotherNext());
                    previousNodeStack.push(node);
                    matchNodeListStack.push(new ArrayList<>(matchNodeList));
                    segmentCountStack.push(segmentCount);
                }
                previousNode = node;
                node = node.getNext();
            }while(node != null);
//            System.out.println("matchSegmentCount=" + matchSegmentCount);
//            System.out.println("mismatchSegmentCount=" + mismatchSegmentCount);
//            System.out.println("-------------------");
            if(segmentCount < minSegmentCount){
                minSegmentCount = segmentCount;
                minMatchNodeList = matchNodeList;
            }
            if(nodeStack.empty()){
                break;
            }
            count++;
            node = nodeStack.pop();
            previousNode = previousNodeStack.pop();
            matchNodeList = matchNodeListStack.pop();
            segmentCount = segmentCountStack.pop();
//            System.out.print("previousNode=" + previousNode);
//            System.out.print("\tnode=" + node);
//            System.out.print("\tmatchSegmentCount=" + matchSegmentCount);
//            System.out.println("\tmismatchSegmentCount=" + mismatchSegmentCount);
        }
        System.out.println("count = " + count);
        /** 所有匹配方案中，找出分隔段数最小的方案作为最终匹配方案 **/
//        System.out.println("++++++++++++++++++++++++++++++++");
//        minMatchNodeList.forEach(System.out::println);
        int prevMatchOldIndex = 0;
        int prevMatchNewIndex = 0;
        SegmentPair segmentPair = null;
        int size = minMatchNodeList.size();
        List<SegmentPair> resultList = new ArrayList<>();
        for(int i = 0; i < size; i++){
            node = minMatchNodeList.get(i);
            if(segmentPair == null){
                if(prevMatchOldIndex != node.getOldIndex() || prevMatchNewIndex != node.getNewIndex()){
                    resultList.add(new SegmentPair(prevMatchOldIndex, node.getOldIndex(), prevMatchNewIndex, node.getNewIndex(), false));
                }
                segmentPair = new SegmentPair(node.getOldIndex(), node.getOldIndex() + 1, node.getNewIndex(), node.getNewIndex() + 1, true);
            }else if(node.getOldIndex() == prevMatchOldIndex && node.getNewIndex() == prevMatchNewIndex){
                segmentPair.setEndIndex(node.getOldIndex() + 1, node.getNewIndex() + 1);
            }else{
                resultList.add(segmentPair);
                resultList.add(new SegmentPair(prevMatchOldIndex, node.getOldIndex(), prevMatchNewIndex, node.getNewIndex(), false));
                segmentPair = new SegmentPair(node.getOldIndex(), node.getOldIndex() + 1, node.getNewIndex(), node.getNewIndex() + 1, true);
            }
            prevMatchOldIndex = node.getOldIndex() + 1;
            prevMatchNewIndex = node.getNewIndex() + 1;
        }
        if(segmentPair != null){
            resultList.add(segmentPair);
        }
        if(CollectionUtils.isEmpty(resultList) || prevMatchOldIndex != sourceCount || prevMatchNewIndex != targetCount){
            resultList.add(new SegmentPair(prevMatchOldIndex, sourceCount, prevMatchNewIndex, targetCount, false));
        }

//        resultList.forEach(System.out::println);
//        System.out.println("++++++++++++++++++++++++++++++++");
        return resultList;
    }

    @Deprecated
    private static List<SegmentPair> getSegmentPairList2(List<Node> matchNodeList, int sourceCount, int targetCount){
//        System.out.println("++++++++++++++++++++++++++++++++");
//        matchNodeList.forEach(System.out::println);
//        List<SegmentPair> trueSegmentPairList = new ArrayList<>();
//        int prevMatchOldIndex = -1;
//        int prevMatchNewIndex = -1;
//        SegmentPair segmentPair = null;
//        int size = matchNodeList.size();
//        for(int i = 0; i < size; i++){
//            Node node = matchNodeList.get(i);
//            if(segmentPair == null){
//                segmentPair = new SegmentPair(node.getOldIndex(), node.getOldIndex() + 1, node.getNewIndex(), node.getNewIndex() + 1, true);
//            }else if(node.getOldIndex() == prevMatchOldIndex + 1 && node.getNewIndex() == prevMatchNewIndex + 1){
//                segmentPair.setEndIndex(node.getOldIndex() + 1, node.getNewIndex() + 1);
//            }else{
//                trueSegmentPairList.add(segmentPair);
//                segmentPair = new SegmentPair(node.getOldIndex(), node.getOldIndex() + 1, node.getNewIndex(), node.getNewIndex() + 1, true);
//            }
//            prevMatchOldIndex = node.getOldIndex();
//            prevMatchNewIndex = node.getNewIndex();
//        }
//        if(segmentPair != null){
//            trueSegmentPairList.add(segmentPair);
//        }
//        trueSegmentPairList.forEach(System.out::println);
//        System.out.println("————————————————————————————");
//        List<SegmentPair> resultList = new ArrayList<>();
//        int oldBeginIndex = 0;
//        int newBeginIndex = 0;
//        for(SegmentPair trueSegmentPair : trueSegmentPairList){
//            if(oldBeginIndex != trueSegmentPair.getOldBeginIndex() || newBeginIndex != trueSegmentPair.getNewBeginIndex()){
//                resultList.add(new SegmentPair(oldBeginIndex, trueSegmentPair.getOldBeginIndex(), newBeginIndex, trueSegmentPair.getNewBeginIndex(), false));
//            }
//            oldBeginIndex = trueSegmentPair.getOldEndIndex();
//            newBeginIndex = trueSegmentPair.getNewEndIndex();
//            resultList.add(trueSegmentPair);
//        }
//        if(CollectionUtils.isEmpty(resultList) || oldBeginIndex != sourceCount || newBeginIndex != targetCount){
//            resultList.add(new SegmentPair(oldBeginIndex, sourceCount, newBeginIndex, targetCount, false));
//        }
//        resultList.forEach(System.out::println);
//        System.out.println("++++++++++++++++++++++++++++++++");
//        return resultList;
        System.out.println("++++++++++++++++++++++++++++++++");
        matchNodeList.forEach(System.out::println);
        int prevMatchOldIndex = 0;
        int prevMatchNewIndex = 0;
        SegmentPair segmentPair = null;
        int size = matchNodeList.size();
        List<SegmentPair> resultList = new ArrayList<>();
        for(int i = 0; i < size; i++){
            Node node = matchNodeList.get(i);
            if(segmentPair == null){
                if(prevMatchOldIndex != node.getOldIndex() || prevMatchNewIndex != node.getNewIndex()){
                    resultList.add(new SegmentPair(prevMatchOldIndex, node.getOldIndex(), prevMatchNewIndex, node.getNewIndex(), false));
                }
                segmentPair = new SegmentPair(node.getOldIndex(), node.getOldIndex() + 1, node.getNewIndex(), node.getNewIndex() + 1, true);
            }else if(node.getOldIndex() == prevMatchOldIndex && node.getNewIndex() == prevMatchNewIndex){
                segmentPair.setEndIndex(node.getOldIndex() + 1, node.getNewIndex() + 1);
            }else{
                resultList.add(segmentPair);
                resultList.add(new SegmentPair(prevMatchOldIndex, node.getOldIndex(), prevMatchNewIndex, node.getNewIndex(), false));
                segmentPair = new SegmentPair(node.getOldIndex(), node.getOldIndex() + 1, node.getNewIndex(), node.getNewIndex() + 1, true);
            }
            prevMatchOldIndex = node.getOldIndex() + 1;
            prevMatchNewIndex = node.getNewIndex() + 1;
        }
        if(segmentPair != null){
            resultList.add(segmentPair);
        }
        if(CollectionUtils.isEmpty(resultList) || prevMatchOldIndex != sourceCount || prevMatchNewIndex != targetCount){
            resultList.add(new SegmentPair(prevMatchOldIndex, sourceCount, prevMatchNewIndex, targetCount, false));
        }

        resultList.forEach(System.out::println);
        System.out.println("++++++++++++++++++++++++++++++++");
        return resultList;
    }

    /**
     * @Description: 查找字符串source与target的公共前缀长度
     * @Author: linbq
     * @Date: 2021/2/28 14:29
     * @Params:[source, target]
     * @Returns:int
     **/
    private static int getPrefixLength(String source, String target){
        return getPrefixLength(source, 0, source.length(), target, 0, target.length());
    }
    /**
     * @Description: 查找字符串A与B的公共前缀长度，其中字符串A是source从sourceOffset下标开始sourceCount长度的子串，字符串B是target从targetOffset下标开始targetCount长度的子串
     * @Author: linbq
     * @Date: 2021/2/28 14:29
     * @Params:[source, target]
     * @Returns:int
     **/
    private static int getPrefixLength(String source, int sourceOffset, int sourceCount, String target, int targetOffset, int targetCount){
        int lim = Math.min(sourceCount, targetCount);

        int k = 0;
        while (k < lim) {
            char c1 = source.charAt(sourceOffset + k);
            char c2 = target.charAt(targetOffset + k);
            if (c1 != c2) {
                return k;
            }
            k++;
        }
        return k;
    }
    /**
     * @Description: 查找字符串source与target的公共后缀长度
     * @Author: linbq
     * @Date: 2021/2/28 14:29
     * @Params:[source, target]
     * @Returns:int
     **/
    private static int getSuffixLength(String source, String target){
        return getSuffixLength(source, 0, source.length(), target, 0, target.length());
    }
    /**
     * @Description: 查找字符串A与B的公共后缀长度，其中字符串A是source从sourceOffset下标开始sourceCount长度的子串，字符串B是target从targetOffset下标开始targetCount长度的子串
     * @Author: linbq
     * @Date: 2021/2/28 14:29
     * @Params:[source, target]
     * @Returns:int
     **/
    private static int getSuffixLength(String source, int sourceOffset, int sourceCount, String target, int targetOffset, int targetCount){
        int lim = Math.min(sourceCount, targetCount);

        int k = 0;
        while (k < lim) {
            char c1 = source.charAt(sourceCount - k - 1);
            char c2 = target.charAt(targetCount - k - 1);
            if (c1 != c2) {
                return k;
            }
            k++;
        }
        return k;
    }
    /**
     * @Description:判断字符串B是不是字符串A的子串，其中字符串A是source从sourceOffset下标开始sourceCount长度的子串，字符串B是target从targetOffset下标开始targetCount长度的子串
     * @Author: linbq
     * @Date: 2021/2/28 14:20
     * @Params:[source, sourceOffset, sourceCount, target, targetOffset, targetCount]
     * @Returns:int 返回开始下标
     **/
    private static int indexOf(String source, int sourceOffset, int sourceCount, String target, int targetOffset, int targetCount) {
        char first = target.charAt(targetOffset);
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset; i <= max; i++) {
            /* 寻找第一个字符 */
            if (source.charAt(i) != first) {
                while (++i <= max && source.charAt(i) != first);
            }

            /* 找到第一个字符，现在看看target其余部分 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source.charAt(j)
                        == target.charAt(k); j++, k++);

                if (j == end) {
                    /* 找到target整个字符串 */
                    return i - sourceOffset;
                }
            }
        }
        return -1;
    }

    /**
     * @Description: 查找字符串source与target的公共前缀长度
     * @Author: linbq
     * @Date: 2021/2/28 14:29
     * @Params:[source, target]
     * @Returns:int
     **/
    private static <T> int getPrefixLength(List<T> source, List<T> target){
        return getPrefixLength(source, 0, source.size(), target, 0, target.size());
    }
    /**
     * @Description: 查找字符串A与B的公共前缀长度，其中字符串A是source从sourceOffset下标开始sourceCount长度的子串，字符串B是target从targetOffset下标开始targetCount长度的子串
     * @Author: linbq
     * @Date: 2021/2/28 14:29
     * @Params:[source, target]
     * @Returns:int
     **/
    private static <T> int getPrefixLength(List<T> source, int sourceOffset, int sourceCount, List<T> target, int targetOffset, int targetCount){
        int lim = Math.min(sourceCount, targetCount);

        int k = 0;
        while (k < lim) {
            T c1 = source.get(sourceOffset + k);
            T c2 = target.get(targetOffset + k);
            if (!Objects.equals(c1, c2)) {
                return k;
            }
            k++;
        }
        return k;
    }
    /**
     * @Description: 查找字符串source与target的公共后缀长度
     * @Author: linbq
     * @Date: 2021/2/28 14:29
     * @Params:[source, target]
     * @Returns:int
     **/
    private static <T> int getSuffixLength(List<T> source, List<T> target){
        return getSuffixLength(source, 0, source.size(), target, 0, target.size());
    }
    /**
     * @Description: 查找字符串A与B的公共后缀长度，其中字符串A是source从sourceOffset下标开始sourceCount长度的子串，字符串B是target从targetOffset下标开始targetCount长度的子串
     * @Author: linbq
     * @Date: 2021/2/28 14:29
     * @Params:[source, target]
     * @Returns:int
     **/
    private static <T> int getSuffixLength(List<T> source, int sourceOffset, int sourceCount, List<T> target, int targetOffset, int targetCount){
        int lim = Math.min(sourceCount, targetCount);

        int k = 0;
        while (k < lim) {
            T c1 = source.get(sourceCount - k - 1);
            T c2 = target.get(targetCount - k - 1);
            if (!Objects.equals(c1, c2)) {
                return k;
            }
            k++;
        }
        return k;
    }
    /**
     * @Description:判断字符串B是不是字符串A的子串，其中字符串A是source从sourceOffset下标开始sourceCount长度的子串，字符串B是target从targetOffset下标开始targetCount长度的子串
     * @Author: linbq
     * @Date: 2021/2/28 14:20
     * @Params:[source, sourceOffset, sourceCount, target, targetOffset, targetCount]
     * @Returns:int 返回开始下标
     **/
    private static <T> int indexOf(List<T> source, int sourceOffset, int sourceCount, List<T> target, int targetOffset, int targetCount) {
        T first = target.get(targetOffset);
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset; i <= max; i++) {
            /* 寻找第一个字符 */
            if (!Objects.equals(source.get(i), first)) {
                while (++i <= max && !Objects.equals(source.get(i), first));
            }

            /* 找到第一个字符，现在看看target其余部分 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && Objects.equals(source.get(j), target.get(k)); j++, k++);

                if (j == end) {
                    /* 找到target整个字符串 */
                    return i - sourceOffset;
                }
            }
        }
        return -1;
    }
    public static void main(String[] args){
//        String source = "sasdfweghjklr";
//        String target = "sasdfghwejklr";
//        System.out.println(getPrefixLength(source, target));
//        System.out.println(getSuffixLength(source, target));

//        String source = "abcgdefghijklmnopq";
//        String target = "aghijkld";
//        System.out.println(indexOf(source, 0, source.length(), target, 1, target.length() - 2));
//
System.out.println(true ^ false);
    }
}
