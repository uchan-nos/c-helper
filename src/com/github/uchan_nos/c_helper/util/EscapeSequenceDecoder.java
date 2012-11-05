package com.github.uchan_nos.c_helper.util;

import com.github.uchan_nos.c_helper.util.PrintfFormatAnalyzer.CharReader;

public class EscapeSequenceDecoder {
    public static enum ErrorReason {
        NO_ERROR, UNKNOWN_ESCAPE_SEQUENCE, OUT_OF_ASCIIRANGE, UNEXPECTED_NULL_CHAR
    }

    public static class Result {
        // デコード結果（デコードが途中で失敗した場合は，そこまでのデコード結果）
        private final String decodedString;
        // デコード中に見つかったエラーの場所（デコード対象文字列での位置．エラーがないなら-1）
        private final int errorLocation;
        // デコードエラーの理由
        private final ErrorReason errorReason;

        public Result(String decodedString) {
            this.decodedString = decodedString;
            this.errorLocation = -1;
            this.errorReason = ErrorReason.NO_ERROR;
        }

        public Result(String decodedString, int errorLocation, ErrorReason errorReason) {
            this.decodedString = decodedString;
            this.errorLocation = errorLocation;
            this.errorReason = errorReason;
        }

        public String decodedString() {
            return decodedString;
        }

        public int errorLocation() {
            return errorLocation;
        }

        public ErrorReason errorReason() {
            return errorReason;
        }
    }

    public static Result decode(String escapedSequence) {
        StringBuilder sb = new StringBuilder();
        CharReader reader = new CharReader(escapedSequence);

        int esIndex = -1; // エスケープシーケンスの中でのインデックス
        int octIndex = -1; // 8進数エスケープシーケンスの場合のインデックス
        int hexIndex = -1; // 16進数エスケープシーケンスの場合のインデックス
        int num = 0;

        final String ONE_CHAR_ES = "'?\"\\";
        final String OCT_CHARS = "01234567";
        final String HEX_CHARS = "0123456789abcdef";

        ErrorReason errorReason = ErrorReason.NO_ERROR;
        char c = reader.read();
        while (true) {

            if (esIndex < 0) {
                if (c == '\\') {
                    esIndex = 0;
                } else {
                    sb.append(c);
                }
            } else if (esIndex == 0) {
                if (ONE_CHAR_ES.indexOf(c) >= 0) {
                    sb.append(ONE_CHAR_ES.charAt(ONE_CHAR_ES.indexOf(c)));
                    esIndex = -1;
                } else if (c == 'x') {
                    hexIndex = 0;
                    esIndex++;
                    num = 0;
                } else if (OCT_CHARS.indexOf(c) >= 0) {
                    octIndex = 0;
                    esIndex++;
                    num = OCT_CHARS.indexOf(c);
                } else {
                    errorReason = ErrorReason.UNKNOWN_ESCAPE_SEQUENCE;
                }
            } else {
                if (hexIndex >= 0) {
                    if (HEX_CHARS.indexOf(c) >= 0) {
                        hexIndex++;
                        num = 16 * num + HEX_CHARS.indexOf(c);
                        esIndex++;

                        assert num >= 0;
                        if (num >= 256) {
                            errorReason = ErrorReason.OUT_OF_ASCIIRANGE;
                        }
                    } else {
                        // 16進数は終了
                        assert 0 <= num && num < 255;
                        esIndex = -1;
                        hexIndex = -1;
                        if (num == 0) {
                            errorReason = ErrorReason.UNEXPECTED_NULL_CHAR;
                        } else {
                            sb.append((char) num);
                            continue; // cを消費せず終了
                        }
                    }
                } else if (octIndex >= 0) {
                    if (OCT_CHARS.indexOf(c) >= 0) {
                        octIndex++;
                        num = 8 * num + OCT_CHARS.indexOf(c);
                        esIndex++;

                        if (num >= 256) {
                            errorReason = ErrorReason.OUT_OF_ASCIIRANGE;
                        }

                        if (octIndex == 2) {
                            // 8進数は終了
                            esIndex = -1;
                            octIndex = -1;
                            if (num == 0) {
                                errorReason = ErrorReason.UNEXPECTED_NULL_CHAR;
                            } else {
                                sb.append((char) num);
                            }
                        }
                    } else {
                        // 8進数は終了
                        esIndex = -1;
                        if (num == 0) {
                            errorReason = ErrorReason.UNEXPECTED_NULL_CHAR;
                        } else {
                            sb.append((char) num);
                            continue; // cを消費せず終了
                        }
                    }
                } else {
                    errorReason = ErrorReason.UNKNOWN_ESCAPE_SEQUENCE;
                }
            }

            if (errorReason != ErrorReason.NO_ERROR) {
                break;
            }

            if (reader.eof()) {
                if (esIndex >= 0 && (hexIndex >= 0 || octIndex >= 0)) {
                    if (num == 0) {
                        errorReason = ErrorReason.UNEXPECTED_NULL_CHAR;
                    } else if (num >= 256) {
                        errorReason = ErrorReason.OUT_OF_ASCIIRANGE;
                    } else {
                        sb.append((char) num);
                    }
                }
                break;
            }
            c = reader.read();
        }

        if (errorReason != ErrorReason.NO_ERROR) {
            return new Result(sb.toString(), reader.index() - 1, errorReason);
        } else {
            return new Result(sb.toString());
        }
    }
}
