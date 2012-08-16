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

    /**
     * ユーザーへの提案（警告）を表す.
     * IASTFileLocation#getStartingLineNumber と IASTFileLocation#getEndingLineNumber
     * は1始まりの行数を返すため注意すること.
     * @param filePath どのファイルに対する提案か
     * @param lineNumber どの行に対する提案か（0始まりの行番号）
     * @param columnNumber どの列に対する提案か（0始まりの列番号）
     * @param offset どのオフセットに対する提案か（ファイル先頭からの0始まりのオフセット位置）
     * @param length 提案の範囲（offsetからlength文字が提案の対象）
     * @param message 提案の内容
     */
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
