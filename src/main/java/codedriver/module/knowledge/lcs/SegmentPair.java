package codedriver.module.knowledge.lcs;
/**
 * 
* @Time:2020年10月22日
* @ClassName: SegmentMapping 
* @Description: 分段对类，用于保存新旧数据的对应的两段信息
 */
public class SegmentPair {
    /** 旧数据的段 **/
    private final SegmentRange oldSegmentRange;
    /** 新数据的段 **/
    private final SegmentRange newSegmentRange;
    /** 这两段是否匹配 **/
    private boolean match;
    
    public SegmentPair(int oldBeginIndex, int newBeginIndex, boolean match) {
        this.oldSegmentRange = new SegmentRange(oldBeginIndex, match);
        this.newSegmentRange = new SegmentRange(newBeginIndex, match);
        this.match = match;
    }

    public SegmentRange getOldSegmentRange() {
        return oldSegmentRange;
    }

    public SegmentRange getNewSegmentRange() {
        return newSegmentRange;
    }

    public boolean isMatch() {
        return match;
    }
    public void setMatch(boolean match) {
        this.match = match;
    }

    public void setEndIndex(int oldEndIndex, int newEndIndex) {
        oldSegmentRange.setEndIndex(oldEndIndex);
        newSegmentRange.setEndIndex(newEndIndex);
    }
    @Override
    public String toString() {
        return "[oldSegmentRange=" + oldSegmentRange + ", newSegmentRange=" + newSegmentRange
            + ", match=" + match + "]";
    }
}
