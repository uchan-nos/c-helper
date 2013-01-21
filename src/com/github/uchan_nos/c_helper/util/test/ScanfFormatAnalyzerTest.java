package com.github.uchan_nos.c_helper.util.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.github.uchan_nos.c_helper.util.ScanfFormatAnalyzer;

public class ScanfFormatAnalyzerTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test1() {
        ScanfFormatAnalyzer.FormatSpecifier[] specs = analyze("%d");

        assertNotNull(specs);
        testFormatSpecifier(specs[0], '\0', "", "", 'd', null);
    }

    @Test
    public void test2() {
        ScanfFormatAnalyzer.FormatSpecifier[] specs = analyze("%*10lx");

        assertNotNull(specs);
        testFormatSpecifier(specs[0], '*', "10", "l", 'x', null);
    }

    @Test
    public void test3() {
        ScanfFormatAnalyzer.FormatSpecifier[] specs = analyze("%[abx]");

        assertNotNull(specs);
        testFormatSpecifier(specs[0], '\0', "", "", '[', "abx".toCharArray());
    }

    @Test
    public void test4() {
        ScanfFormatAnalyzer.FormatSpecifier[] specs = analyze("%[^8(+]");

        assertNotNull(specs);
        testFormatSpecifier(specs[0], '\0', "", "", '[', "^8(+".toCharArray());
    }

    @Test
    public void test5() {
        ScanfFormatAnalyzer.FormatSpecifier[] specs = analyze("%[]abx]");

        assertNotNull(specs);
        testFormatSpecifier(specs[0], '\0', "", "", '[', "]abx".toCharArray());
    }

    @Test
    public void test6() {
        ScanfFormatAnalyzer.FormatSpecifier[] specs = analyze("%[^]8(+]");

        assertNotNull(specs);
        testFormatSpecifier(specs[0], '\0', "", "", '[', "^]8(+".toCharArray());
    }

    private static ScanfFormatAnalyzer.FormatSpecifier[] analyze(String format) {
        return new ScanfFormatAnalyzer().analyze(format);
    }

    private static void testFormatSpecifier(ScanfFormatAnalyzer.FormatSpecifier spec,
            char assign, String width, String length, char type, char[] scanlist) {
        assertEquals(assign, spec.assign);
        assertEquals(width, spec.width);
        assertEquals(length, spec.length);
        assertEquals(type, spec.type);

        if (scanlist == null) {
            assertNull(spec.scanlist);
        } else {
            assertNotNull(spec.scanlist);
            assertEquals(scanlist.length, spec.scanlist.length);
            for (int i = 0; i < scanlist.length; ++i) {
                assertEquals(scanlist[i], spec.scanlist[i]);
            }
        }
    }
}
