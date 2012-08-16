package com.github.uchan_nos.c_helper;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.text.Document;

import com.github.uchan_nos.c_helper.analysis.Analyzer;
import com.github.uchan_nos.c_helper.util.Util;

public class Launcher {

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 1) {
            String inputFilename = args[0];
            File inputFile = new File(inputFilename);

            try {
                String fileContent = Util.readFileAll(inputFile, "UTF-8");
                Analyzer analyzer =
                        new Analyzer();
                analyzer.analyze(inputFilename, new Document(fileContent));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
