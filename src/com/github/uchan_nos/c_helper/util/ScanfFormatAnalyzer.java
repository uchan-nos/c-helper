package com.github.uchan_nos.c_helper.util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ScanfFormatAnalyzer {
    public static class FormatSpecifier {
        public final char assign;
        public final String width;
        public final String length;
        public final char type;
        public final char[] scanlist; // ブラケットに囲まれた文字列。[^..]の場合は^も含む

        private final boolean error;

        public FormatSpecifier(char assign, String width,
                String length, char type, char[] scanlist, boolean error) {
            this.assign = assign;
            this.width = width;
            this.length = length;
            this.type = type;
            this.scanlist = scanlist;
            this.error = error;
        }

        public boolean hasError() {
            return error;
        }
    }

    public static class CharReader {
        public final String data;
        private int i;
        public CharReader(String data) {
            this.data = data;
            this.i = 0;
        }
        public char read() {
            if (i < data.length()) {
                char c = data.charAt(i);
                ++i;
                return c;
            } else {
                return '\0';
            }
        }
        public boolean eof() {
            return i == data.length();
        }
        public int index() {
            return i;
        }
    }

    public static enum Type {
        INT, UINT, DOUBLE, CHAR, STRING, VOIDPTR, INTPTR
    }

    public static final Map<Character, Type> EXPECTED_TYPE;

    static {
        Map<Character, Type> expectedType =
                new HashMap<Character, Type>();
        expectedType.put('d', Type.INT);
        expectedType.put('i', Type.INT);
        expectedType.put('u', Type.UINT);
        expectedType.put('f', Type.DOUBLE);
        expectedType.put('F', Type.DOUBLE);
        expectedType.put('e', Type.DOUBLE);
        expectedType.put('E', Type.DOUBLE);
        expectedType.put('g', Type.DOUBLE);
        expectedType.put('G', Type.DOUBLE);
        expectedType.put('x', Type.UINT);
        expectedType.put('X', Type.UINT);
        expectedType.put('o', Type.UINT);
        expectedType.put('s', Type.STRING);
        expectedType.put('c', Type.CHAR);
        expectedType.put('p', Type.VOIDPTR);
        expectedType.put('n', Type.INTPTR);
        expectedType.put('[', Type.STRING);
        EXPECTED_TYPE = expectedType;
    }

    /**
     * 指定された書式文字列に存在する書式指定子をすべて抽出して返す.
     * @param formatString 書式文字列
     * @return 書式指定子の順番が保存された配列
     */
    public FormatSpecifier[] analyze(String formatString) {
        ArrayList<FormatSpecifier> formatSpecifiers =
                new ArrayList<ScanfFormatAnalyzer.FormatSpecifier>();

        CharReader reader = new CharReader(formatString);
        while (!reader.eof()) {
            char c = reader.read();
            if (c == '%') {
                formatSpecifiers.add(extractOneSpecifier(reader));
            }
        }

        return formatSpecifiers.toArray(new FormatSpecifier[] {});
    }

    /**
     * 指定された書式指定子の配列から %% を取り除いた配列を返す.
     * @param specifiers %%を含むかもしれない書式指定子の配列
     * @return %%を含まない書式指定子の配列
     */
    public static FormatSpecifier[] removePercentSpecifier(FormatSpecifier[] specifiers) {
        ArrayList<FormatSpecifier> filtered =
                new ArrayList<ScanfFormatAnalyzer.FormatSpecifier>();
        for (int i = 0; i < specifiers.length; ++i) {
            if (specifiers[i].type != '%') {
                filtered.add(specifiers[i]);
            }
        }
        return filtered.toArray(new FormatSpecifier[] {});
    }

    /**
     * 書式指定子を1つだけ抽出するヘルパーメソッド.
     * @param reader
     * @return
     */
    private FormatSpecifier extractOneSpecifier(CharReader reader) {
        char assign = '\0';
        String width = "";
        String length = "";
        char type = '\0';
        ArrayList<Character> scanlist = null;
        boolean error = false;

        char c;

        c = reader.read();
        if (c == '*') {
            assign = c;
            c = reader.read();
        }

        while (c != '\0' && !error && Character.isDigit(c)) {
            width += c;
            c = reader.read();
        }
        if (c == '\0') error = true;

        if (c != '\0' && !error && "hlLzjt".indexOf(c) >= 0) {
            length += c;
            if (c == 'h') {
                c = reader.read();
                if (c != '\0' && !error && c == 'h') {
                    length += c;
                    c = reader.read();
                }
            } else if (c == 'l') {
                c = reader.read();
                if (c != '\0' && !error && c == 'l') {
                    length += c;
                    c = reader.read();
                }
            }
        }
        if (c == '\0') error = true;

        if (c != '\0' && "diufFeEgGxXoscpn%".indexOf(c) >= 0) {
            type = c;
        } else if (c == '[') {
            type = c;
            ReadScanlistResult result = readScanlist(reader);
            scanlist = result.scanlist;
            error |= result.error;
        } else {
            error = true;
        }

        char[] scanlist_ = null;
        if (scanlist != null) {
            scanlist_ = new char[scanlist.size()];
            for (int i = 0; i < scanlist.size(); ++i) {
                scanlist_[i] = scanlist.get(i);
            }
        }
        return new FormatSpecifier(assign, width, length, type, scanlist_, error);
    }

    private static class ReadScanlistResult {
        public final ArrayList<Character> scanlist;
        public final boolean error;
        public ReadScanlistResult(ArrayList<Character> scanlist, boolean error) {
            this.scanlist = scanlist;
            this.error = error;
        }
    }

    /**
     * 閉じブラケット ] まで読み込み、ブラケットの直前までの文字列を返す.
     * readerの現在位置が開始ブラケット [ の直後であることを仮定している.
     * @return ブラケットの中身
     */
    private static ReadScanlistResult readScanlist(CharReader reader) {
        ArrayList<Character> scanlist = new ArrayList<Character>();
        boolean error = false;

        char c = reader.read();
        if (c == '^') {
            scanlist.add(c);
            c = reader.read();
        }

        if (c == ']') {
            scanlist.add(c);
            c = reader.read();
        }

        while (c != '\0' && !error && c != ']') {
            scanlist.add(c);
            c = reader.read();
        }
        if (c == '\0') error = true;

        return new ReadScanlistResult(scanlist, error);
    }
}
