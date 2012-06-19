package com.github.uchan_nos.c_helper.exceptions;

/**
 * 不正なエディタ部品が渡されたことを示す例外.
 * 例えば、ITextEditorを実装したクラスを渡されることを期待している場所で、そうでないクラスが渡された場合に発生する。
 * @author uchan
 *
 */
public class InvalidEditorPartException extends Exception {
    private static final long serialVersionUID = 1L;
    public InvalidEditorPartException() {
        super();
    }
    public InvalidEditorPartException(String message) {
        super(message);
    }
    public InvalidEditorPartException(Throwable cause) {
        super(cause);
    }
    public InvalidEditorPartException(String message, Throwable cause) {
        super(message, cause);
    }
}
