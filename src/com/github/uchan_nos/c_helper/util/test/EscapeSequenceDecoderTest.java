package com.github.uchan_nos.c_helper.util.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.github.uchan_nos.c_helper.util.EscapeSequenceDecoder;
import com.github.uchan_nos.c_helper.util.EscapeSequenceDecoder.ErrorReason;

public class EscapeSequenceDecoderTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testNormal() {
        EscapeSequenceDecoder.Result r;

        r = EscapeSequenceDecoder.decode("");
        assertEquals("", r.decodedString());
        assertEquals(-1, r.errorLocation());

        r = EscapeSequenceDecoder.decode("abc");
        assertEquals("abc", r.decodedString());
        assertEquals(-1, r.errorLocation());
    }

    @Test
    public void testOneCharEscape() {
        EscapeSequenceDecoder.Result r;

        r = EscapeSequenceDecoder.decode("\\\\");
        assertEquals("\\", r.decodedString());
        assertEquals(-1, r.errorLocation());

        r = EscapeSequenceDecoder.decode("l\\\\");
        assertEquals("l\\", r.decodedString());
        assertEquals(-1, r.errorLocation());

        r = EscapeSequenceDecoder.decode("\\\\r");
        assertEquals("\\r", r.decodedString());
        assertEquals(-1, r.errorLocation());

        r = EscapeSequenceDecoder.decode("l\\\\r");
        assertEquals("l\\r", r.decodedString());
        assertEquals(-1, r.errorLocation());

        r = EscapeSequenceDecoder.decode("\\\\\\\\");
        assertEquals("\\\\", r.decodedString());
        assertEquals(-1, r.errorLocation());

        r = EscapeSequenceDecoder.decode("\\?");
        assertEquals("?", r.decodedString());
        assertEquals(-1, r.errorLocation());

        r = EscapeSequenceDecoder.decode("\\\"");
        assertEquals("\"", r.decodedString());
        assertEquals(-1, r.errorLocation());

        r = EscapeSequenceDecoder.decode("\\h");
        assertEquals("", r.decodedString());
        assertEquals(1, r.errorLocation());
        assertEquals(ErrorReason.UNKNOWN_ESCAPE_SEQUENCE, r.errorReason());
    }

    @Test
    public void testOctEscape() {
        EscapeSequenceDecoder.Result r;

        r = EscapeSequenceDecoder.decode("hoge\\100foo");
        assertEquals("hoge@foo", r.decodedString());
        assertEquals(-1, r.errorLocation());

        r = EscapeSequenceDecoder.decode("hoge\\091foo");
        assertEquals("hoge", r.decodedString());
        assertEquals(6, r.errorLocation());
        assertEquals(ErrorReason.UNEXPECTED_NULL_CHAR, r.errorReason());

        r = EscapeSequenceDecoder.decode("hoge\\91foo");
        assertEquals("hoge", r.decodedString());
        assertEquals(5, r.errorLocation());
        assertEquals(ErrorReason.UNKNOWN_ESCAPE_SEQUENCE, r.errorReason());

        r = EscapeSequenceDecoder.decode("hoge\\100\\41foo");
        assertEquals("hoge@!foo", r.decodedString());
        assertEquals(-1, r.errorLocation());
    }

    @Test
    public void testHexEscape() {
        EscapeSequenceDecoder.Result r;

        r = EscapeSequenceDecoder.decode("hoge\\x23");
        assertEquals("hoge#", r.decodedString());
        assertEquals(-1, r.errorLocation());

        r = EscapeSequenceDecoder.decode("hoge\\x100");
        assertEquals("hoge", r.decodedString());
        assertEquals(8, r.errorLocation());
        assertEquals(ErrorReason.OUT_OF_ASCIIRANGE, r.errorReason());
    }

}
