package com.github.uchan_nos.c_helper;

import com.github.uchan_nos.c_helper.analysis.Analyzer;

public class Launcher {

    private static String sourceCode =
              "#include <stdio.h>\n"
            + "int main(void) {\n"
            + "  int c = '\\0';\n"
            + "  int i = 0;\n"
            + "  while (!0) {\n"
            + "    printf(\"c=%c\\n\", c);\n"
            + "    if (c == 'q') {\n"
            + "      goto fin;\n"
            + "    } else if (c == 'b') {\n"
            + "      break;\n"
            + "    }\n"
            + "    puts(\"command!\");\n"
            + "  }\n"
            + "  for (i=0;i<5;++i) {\n"
            + "    printf(\"%d\\n\", i);\n"
            + "  }\n"
            + "fin:\n"
            + "  do {\n"
            + "    puts(\"input>\");\n"
            + "  } while (getchar() == 'a');\n"
            + "  switch (getchar()) {\n"
            + "  case '0':\n"
            + "    puts(\"zero\");\n"
            + "    break;\n"
            + "  case '1':\n"
            + "    puts(\"one\");\n"
            + "    break;\n"
            + "  case '2':\n"
            + "    puts(\"two\");\n"
            + "  default:\n"
            + "    puts(\"default\");\n"
            + "  }\n"
            + "  return 0;\n"
            + "}\n";

    private static String sourceCode2 =
            "#include <stdio.h>\n"
          + "int main(void) {\n"
          + "  int x = 0;\n"
          + "  int i = -2;\n"
          + "  switch (0) {\n"
          + "    for (i = 0; i < 2; ++i) {\n"
          + "      printf(\"%d\\n\", i);\n"
          + "      case 0:\n"
          + "        puts(\"case 0\");\n"
          + "        goto label2;\n"
          + "    }\n"
          + "  }\n"
          + "  puts(\"before label\");\n"
          + "label:\n"
          + "  puts(\"hello, world\");\n"
          + "label2:\n"
          + "  return 0;\n"
          + "}\n"
          ;

    /**
     * @param args
     */
    public static void main(String[] args) {
        String sourceToParse = sourceCode2;
        System.out.println("Analyzing source code");
        System.out.println("--");
        System.out.print(sourceToParse);
        System.out.println("--");
        Analyzer analyzer = new Analyzer();
        analyzer.analyze("dummy file", sourceToParse.toCharArray());
        System.out.println("Successfully analyzed source code.");
    }

}
