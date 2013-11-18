package com.github.uchan_nos.c_helper;

import java.io.File;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.text.Document;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.ParseException;

import com.github.uchan_nos.c_helper.analysis.Analyzer;
import com.github.uchan_nos.c_helper.analysis.FileInfo;
import com.github.uchan_nos.c_helper.util.Util;

public class Launcher {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Logger logger = Activator.getLogger();
        logger.setLevel(Level.INFO);

        // コマンドラインオプションの定義
        Options options = new Options();
        options
            .addOption("s", "suggester", true, "A suggester to be executed")
            .addOption("v", false, "Output verbose log message")
            .addOption("l", "log-level", true, "Set Log Level")
            ;

        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(options, args);

            Analyzer.RunOption opt = new Analyzer.RunOption();

            for (Option option : cmd.getOptions()) {
                switch (option.getId()) {
                case 's':
                    opt.suggester = option.getValue();
                    break;
                case 'v': {
                        Level level = Level.ALL;
                        logger.setLevel(level);
                        Util.GetConsoleHandler(logger).setLevel(level);
                    }
                    break;
                case 'l': {
                        String val = option.getValue();
                        Level level = Level.parse(val);
                        logger.setLevel(level);
                    }
                    break;
                }
            }

            String inputFilename = cmd.getArgs()[0];
            File inputFile = new File(inputFilename);

            try {
                String fileContent = Util.readFileAll(inputFile, "UTF-8");
                Analyzer analyzer =
                        new Analyzer();
                analyzer.analyze(new FileInfo(inputFilename, false), new Document(fileContent), opt);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
