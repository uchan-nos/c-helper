package com.github.uchan_nos.c_helper.util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class PrintfFormatAnalyzer {
    public static class FormatSpecifier {
        public final char[] flags;
        public final String width;
        public final String precision;
        public final String length;
        public final char type;

        private final boolean error;

        public FormatSpecifier(char[] flags, String width, String precision,
                String length, char type, boolean error) {
            this.flags = flags;
            this.width = width;
            this.precision = precision;
            this.length = length;
            this.type = type;
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
        EXPECTED_TYPE = expectedType;
    }

    /**
     * 指定された書式文字列に存在する書式指定子をすべて抽出して返す.
     * @param formatString 書式文字列
     * @return 書式指定子の順番が保存された配列
     */
    public FormatSpecifier[] analyze(String formatString) {
        ArrayList<FormatSpecifier> formatSpecifiers =
                new ArrayList<PrintfFormatAnalyzer.FormatSpecifier>();

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
                new ArrayList<PrintfFormatAnalyzer.FormatSpecifier>();
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
        ArrayList<Character> flags = new ArrayList<Character>();
        String width = "";
        String precision = "";
        String length = "";
        char type = '\0';
        boolean error = false;

        char c;

        c = reader.read();
        while (c != '\0' && !error && "+- #0".indexOf(c) >= 0) {
            flags.add(c);
            c = reader.read();
        }
        if (c == '\0') error = true;

        while (c != '\0' && !error && Character.isDigit(c)) {
            width += c;
            c = reader.read();
        }
        if (c == '\0') error = true;

        if (c == '.') {
            c = reader.read();
            while (c != '\0' && !error && Character.isDigit(c)) {
                precision += c;
                c = reader.read();
            }
            if (!error && c == '*') {
                if (precision.length() == 0) {
                    precision = "*";
                    c = reader.read();
                } else {
                    error = true;
                }
            }
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
        } else {
            error = true;
        }

        char[] flags_ = new char[flags.size()];
        for (int i = 0; i < flags.size(); ++i) {
            flags_[i] = flags.get(i);
        }
        return new FormatSpecifier(flags_, width, precision, length, type, error);
    }
}
