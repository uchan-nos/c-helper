package com.github.uchan_nos.c_helper;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

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
        String inputFilename = null;
        String outputFilename = null;
        if (args.length >= 1) {
            inputFilename = args[0];
            if (args.length == 2) {
                outputFilename = args[1];
            } else if (args.length >= 3) {
                System.err.println("Usage: c-helper [input [output]]");
                return;
            }
        }

        try {
            String sourceToParse = sourceCode2;
            if (inputFilename != null) {
                FileReader reader = new FileReader(inputFilename);

                StringBuilder sb = new StringBuilder();
                char[] buf = new char[1024];

                while (true) {
                    int n = reader.read(buf);
                    if (n == 0 || n == -1) {
                        break;
                    } else {
                        sb.append(buf, 0, n);
                    }
                }
                reader.close();

                sourceToParse = sb.toString();
            }

            if (outputFilename != null) {
                System.out.println("Analyzing source code");
                System.out.println("--");
                System.out.print(sourceToParse);
                System.out.println("--");
            }

            Analyzer analyzer;
            if (outputFilename != null) {
                analyzer = new Analyzer(new FileWriter(outputFilename));
            } else {
                analyzer = new Analyzer(new OutputStreamWriter(System.out));
            }

            String filepath = outputFilename == null ? "dummy file" : outputFilename;

            analyzer.analyze(filepath, sourceToParse.toCharArray());
            if (outputFilename != null) {
                System.out.println("Successfully analyzed source code.");
            }

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
