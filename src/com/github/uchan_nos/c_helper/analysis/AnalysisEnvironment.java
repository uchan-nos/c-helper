package com.github.uchan_nos.c_helper.analysis;

/**
 * 解析をする上での環境を表す.
 * @author uchan
 * 環境とは、様々な前提条件の集まりである。例えば整数型のビット数やポインタのサイズなど。
 */
public class AnalysisEnvironment {
    public static final int CHAR_BITS = 8;
    public static final int SHORT_BITS = 16;
    public static final int INT_BITS = 32;
    public static final int LONG_BITS = 32;
    public static final int LONG_LONG_BITS = 64;
}
