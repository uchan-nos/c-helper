package com.github.uchan_nos.c_helper.analysis;

public class FileInfo {
    private String path;
    private boolean fileIsInWorkspace;

    public FileInfo(String path, boolean fileIsInWorkspace) {
        this.path = path;
        this.fileIsInWorkspace = fileIsInWorkspace;
    }

    public String getPath() {
        return path;
    }

    public boolean isFileInWorkspace() {
        return fileIsInWorkspace;
    }
}
