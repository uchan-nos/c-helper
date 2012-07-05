package com.github.uchan_nos.c_helper;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

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
        if (args.length != 0 && args.length != 2) {
            System.out.println("Usage: c-helper [input output]");
            return;
        }

        try {
            String sourceToParse = sourceCode;
            if (args.length == 2) {
                FileReader reader = new FileReader(args[1]);

                StringBuilder sb = new StringBuilder();
                char[] buf = new char[1024];

                while (true) {
                    int n = reader.read(buf);
                    if (n == 0) {
                        break;
                    } else {
                        sb.append(buf, 0, n);
                    }
                }
                reader.close();

                sourceToParse = sb.toString();
            }

            System.out.println("Analyzing source code");
            System.out.println("--");
            System.out.print(sourceToParse);
            System.out.println("--");

            Analyzer analyzer;
            if (args.length == 2) {
                analyzer = new Analyzer(new FileWriter(args[1]));
            } else {
                analyzer = new Analyzer();
            }

            String filepath = "dummy file";
            if (args.length == 2) {
                filepath = args[1];
            }

            analyzer.analyze(filepath, sourceToParse.toCharArray());
            System.out.println("Successfully analyzed source code.");

            analyzer.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
