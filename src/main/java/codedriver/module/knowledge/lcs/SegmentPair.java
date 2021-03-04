package codedriver.module.knowledge.lcs;
/**
 * 
* @Time:2020年10月22日
* @ClassName: SegmentMapping 
* @Description: 分段对类，用于保存新旧数据的对应的两段信息
 */
public class SegmentPair {
    /** 旧数据的段 **/
//    private final SegmentRange oldSegmentRange;
    private int oldBeginIndex;
    private int oldEndIndex;
    /** 新数据的段 **/
//    private final SegmentRange newSegmentRange;
    private int newBeginIndex;
    private int newEndIndex;
    /** 这两段是否匹配 **/
    private boolean match;
    
    public SegmentPair(int oldBeginIndex, int newBeginIndex, boolean match) {
//        this.oldSegmentRange = new SegmentRange(oldBeginIndex, match);
//        this.newSegmentRange = new SegmentRange(newBeginIndex, match);
        this.oldBeginIndex = oldBeginIndex;
        this.newBeginIndex = newBeginIndex;
        this.match = match;
    }

    public SegmentPair(int oldBeginIndex, int oldEndIndex, int newBeginIndex, int newEndIndex, boolean match) {
//        this.oldSegmentRange = new SegmentRange(oldBeginIndex, oldEndIndex, match);
//        this.newSegmentRange = new SegmentRange(newBeginIndex, newEndIndex, match);
        this.oldBeginIndex = oldBeginIndex;
        this.oldEndIndex = oldEndIndex;
        this.newBeginIndex = newBeginIndex;
        this.newEndIndex = newEndIndex;
        this.match = match;
    }

//    public SegmentRange getOldSegmentRange() {
//        return oldSegmentRange;
//    }
//
//    public SegmentRange getNewSegmentRange() {
//        return newSegmentRange;
//    }

    public boolean isMatch() {
        return match;
    }
    public void setMatch(boolean match) {
        this.match = match;
    }

    public void setEndIndex(int oldEndIndex, int newEndIndex) {
//        oldSegmentRange.setEndIndex(oldEndIndex);
//        newSegmentRange.setEndIndex(newEndIndex);
        this.oldEndIndex = oldEndIndex;
        this.newEndIndex = newEndIndex;
    }

    public int getOldBeginIndex() {
        return oldBeginIndex;
    }

    public int getOldEndIndex() {
        return oldEndIndex;
    }

    public int getNewBeginIndex() {
        return newBeginIndex;
    }

    public int getNewEndIndex() {
        return newEndIndex;
    }

    @Override
    public String toString() {
        return "["  + oldBeginIndex +
                ", " + oldEndIndex +
                "], [" + newBeginIndex +
                ", " + newEndIndex +
                "], " + match;
    }

    public void moveRight(int offset) {
        this.oldBeginIndex += offset;
        this.oldEndIndex += offset;
        this.newBeginIndex += offset;
        this.newEndIndex += offset;
    }
//    @Override
//    public String toString() {
//        return "[oldSegmentRange=" + oldSegmentRange + ", newSegmentRange=" + newSegmentRange
//            + ", match=" + match + "]";
//    }
//
//    public int getOldBeginIndex() {
//        return oldSegmentRange.getBeginIndex();
//    }
//
//    public int getOldEndIndex() {
//        return oldSegmentRange.getEndIndex();
//    }
//
//    public int getNewBeginIndex() {
//        return newSegmentRange.getBeginIndex();
//    }
//
//    public int getNewEndIndex() {
//        return newSegmentRange.getEndIndex();
//    }
}
