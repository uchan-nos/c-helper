package com.github.uchan_nos.c_helper.pointer;

/**
 * 無効なアドレス値を表す.
 * NULL やメモリブロックへのアドレス以外の無効なアドレス
 * （アドレスではない整数をキャストしたアドレスなど）
 */
public class InvalidAddress extends Address {

    public static final InvalidAddress NULL = new InvalidAddress();

}
