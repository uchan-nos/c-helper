package com.github.uchan_nos.c_helper.analysis;

/**
 * 解析をする上での環境を表す.
 * @author uchan
 * 環境とは、様々な前提条件の集まりである。例えば整数型のビット数やポインタのサイズなど。
 */
public class AnalysisEnvironment {
    public final int CHAR_BIT = 8;
    public final int SHORT_BIT = 16;
    public final int INT_BIT = 32;
    public final int LONG_BIT = 32;
    public final int LONG_LONG_BIT = 64;
    public final int POINTER_BIT = INT_BIT;
    public final int POINTER_BYTE = POINTER_BIT / CHAR_BIT;
}
