package com.github.uchan_nos.c_helper.analysis;

/**
 * 解析をする上での環境を表す.
 * @author uchan
 * 環境とは、様々な前提条件の集まりである。例えば整数型のビット数やポインタのサイズなど。
 */
public class AnalysisEnvironment {
    public int CHAR_BIT;
    public int SHORT_BIT;
    public int INT_BIT;
    public int LONG_BIT;
    public int LONG_LONG_BIT;
    public int POINTER_BIT;
    public int POINTER_BYTE;
}
