package com.github.uchan_nos.c_helper;

import com.github.uchan_nos.c_helper.analysis.Analyzer;

public class Launcher {

    private static String sourceCode =
              "#include <stdio.h>\n"
            + "int main(void) {\n"
            + "  int x = 1;\n"
            + "label1:\n"
            + "  while (x == 1) {\n"
            + "    puts(\"hello\");\n"
            + "    //goto label1;\n"
            + "    puts(\"world\");\n"
            + "  }\n"
            + "}\n";

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("Analyzing source code");
        Analyzer analyzer = new Analyzer();
        analyzer.analyze("dummy file", sourceCode.toCharArray());
        System.out.println("Successfully analyzed source code.");
    }

}
