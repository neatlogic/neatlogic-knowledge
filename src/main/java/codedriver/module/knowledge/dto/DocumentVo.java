package codedriver.module.knowledge.dto;

import java.util.List;

import codedriver.framework.dto.TagVo;
import codedriver.framework.file.dto.FileVo;

public class DocumentVo {

    private String title;
    private List<LineVo> lineList;
    private List<FileVo> fileList;
    private List<TagVo> tagList;
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public List<LineVo> getLineList() {
        return lineList;
    }
    public void setLineList(List<LineVo> lineList) {
        this.lineList = lineList;
    }
    public List<FileVo> getFileList() {
        return fileList;
    }
    public void setFileList(List<FileVo> fileList) {
        this.fileList = fileList;
    }
    public List<TagVo> getTagList() {
        return tagList;
    }
    public void setTagList(List<TagVo> tagList) {
        this.tagList = tagList;
    }
    
}
