package com.github.uchan_nos.c_helper.suggest;

/**
 * ユーザーに提案する事柄を表すクラス.
 * @author uchan
 *
 */
public class Suggestion {
    private final String filePath;
    private final int lineNumber;
    private final int columnNumber;
    private final int offset;
    private final int length;
    private final String message;

    public Suggestion(
            String filePath, int lineNumber, int columnNumber,
            int offset, int length, String message) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.offset = offset;
        this.length = length;
        this.message = message;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public String getMessage() {
        return message;
    }
}
