package com.github.uchan_nos.c_helper.resource;

import java.io.InputStreamReader;
import java.io.IOException;

import java.util.Properties;

/**
 * 文字列リソースを管理する.
 * @author uchan
 */
public class StringResource {
    // シングルトン
    private static StringResource instance = new StringResource();

    private Properties stringProperties;
    private StringResource() {
        this.stringProperties = new Properties();
        try {
            this.stringProperties.load(new InputStreamReader(
                    StringResource.class.getResourceAsStream("strings.txt"), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static StringResource getInstance() {
        return instance;
    }

    /**
     * リソースから指定されたキーの文字列を取得して返す.
     * キーがない場合、そのキー自身を返す.
     * getInstance().getString(key, formatArgs) と等価.
     * @param key 文字列のキー
     * @param formatArgs 文字列に埋め込むデータ（String#format の引数として使用される）
     * @return キーに対応した文字列またはそのキー自身.
     */
    public static String get(String key, Object... formatArgs) {
        return getInstance().getString(key, formatArgs);
    }

    /**
     * リソースから指定されたキーの文字列を取得して返す.
     * キーがない場合、そのキー自身を返す.
     * @param key 文字列のキー
     * @param formatArgs 文字列に埋め込むデータ（String#format の引数として使用される）
     * @return キーに対応した文字列またはそのキー自身.
     */
    public String getString(String key, Object... formatArgs) {
        if (key != null) {
            String value = this.stringProperties.getProperty(key, "!" + key);
            return String.format(value, formatArgs);
        } else {
            return null;
        }
    }
}
