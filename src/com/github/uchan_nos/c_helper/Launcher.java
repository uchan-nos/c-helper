package com.github.uchan_nos.c_helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Document;

import com.github.uchan_nos.c_helper.analysis.Analyzer;
import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.analysis.CFGCreator;
import com.github.uchan_nos.c_helper.analysis.Parser;
import com.github.uchan_nos.c_helper.analysis.RD;
import com.github.uchan_nos.c_helper.analysis.RDAnalyzer;
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
