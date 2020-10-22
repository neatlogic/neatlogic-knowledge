package codedriver.module.knowledge.lcs;

public class SegmentMapping {
    private final SegmentRange oldSegmentRange;
    private final SegmentRange newSegmentRange;
    private boolean match;
    
    public SegmentMapping(int oldBeginIndex, int newBeginIndex, boolean match) {
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
