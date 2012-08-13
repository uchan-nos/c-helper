package com.github.uchan_nos.c_helper.analysis;

import java.util.ArrayList;
import java.util.List;

public class LineDelimiterChecker {
    public static enum Delimiter {
        CR, LF, CRLF
    }
    public static class DelimiterPosition {
        private final Delimiter delimiter;
        private final int position;
        public DelimiterPosition(Delimiter delimiter, int position) {
            this.delimiter = delimiter;
            this.position = position;
        }

        public Delimiter delimiter() {
            return delimiter;
        }
        public int position() {
            return position;
        }
    }

    public List<DelimiterPosition> check(String source) {
        ArrayList<DelimiterPosition> delimiterPositions =
                new ArrayList<LineDelimiterChecker.DelimiterPosition>();

        for (int i = 0; i < source.length(); ++i) {
            if (source.charAt(i) == '\r') {
                if (i < source.length() - 1 && source.charAt(i + 1) == '\n') {
                    delimiterPositions.add(new DelimiterPosition(Delimiter.CRLF, i));
                    ++i;
                } else {
                    delimiterPositions.add(new DelimiterPosition(Delimiter.CR, i));
                }
            } else if (source.charAt(i) == '\n') {
                delimiterPositions.add(new DelimiterPosition(Delimiter.LF, i));
            }
        }

        return delimiterPositions;
    }
}
