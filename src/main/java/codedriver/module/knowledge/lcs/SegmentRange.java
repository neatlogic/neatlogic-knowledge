package codedriver.module.knowledge.lcs;
/**
 * 
* @Time:2020年10月22日
* @ClassName: SegmentRange 
* @Description: TODO
 */
public class SegmentRange {
    private final int beginIndex;
    private int endIndex;
    private boolean match;

    public SegmentRange(int beginIndex, boolean match) {
        this.beginIndex = beginIndex;
        this.match = match;
    }
    public int getBeginIndex() {
        return beginIndex;
    }
    public int getEndIndex() {
        return endIndex;
    }
    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
    public int getSize() {
        return endIndex - beginIndex;
    }
    public boolean isMatch() {
        return match;
    }
    public void setMatch(boolean match) {
        this.match = match;
    }
    @Override
    public String toString() {
        return "[" + beginIndex + ", " + endIndex + "]";
    }
}
