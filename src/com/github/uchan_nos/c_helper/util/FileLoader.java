package com.github.uchan_nos.c_helper.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.logging.Logger;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import com.github.uchan_nos.c_helper.Activator;

public class FileLoader {
    private final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static FileLoader instance = new FileLoader();
    public static FileLoader getInstance() {
        return instance;
    }

    private final String PLUGIN_DIRECTORY;


    public FileLoader() {
        PLUGIN_DIRECTORY = System.getenv("PLUGIN_DIR");
    }

    public InputStream openStream(String path) throws FileNotFoundException, IOException {
        System.out.println("FileLoader#openStream: " + path + ", PLUGIN_DIRECTORY=" + PLUGIN_DIRECTORY);

        if (PLUGIN_DIRECTORY != null) {
            System.out.println("  loading file from PLUGIN_DIRECTORY");

            return new FileInputStream(new File(PLUGIN_DIRECTORY, path));
        } else if (Activator.getDefault() != null) {
            System.out.println("  loading file from Activator default bundle");

            URL fileURL = Activator.getDefault().getBundle().getEntry(path);

            try {
                System.out.println("  printing file URI");
                URI fileURI = FileLocator.resolve(fileURL).toURI();
                System.out.println("  fileURI: " + fileURI.toString());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            return fileURL.openStream();

        } else {
            System.out.println("  loading file from current directory");

            return new FileInputStream(new File(path));
        }
    }

    public File load(String path) {
        //logger.fine("loading file: " + path + ", PLUGIN_DIRECTORY=" + PLUGIN_DIRECTORY);
        System.out.println("FileLoader#load: " + path + ", PLUGIN_DIRECTORY=" + PLUGIN_DIRECTORY);

        File result = null;
        if (PLUGIN_DIRECTORY != null) {
            //logger.finer("loading file from PLUGIN_DIRECTORY");
            System.out.println("  loading file from PLUGIN_DIRECTORY");

            result = new File(PLUGIN_DIRECTORY, path);
        } else if (Activator.getDefault() != null) {
            //logger.finer("loading file from Activator default bundle");
            System.out.println("  loading file from Activator default bundle");

            //URL fileURL = Activator.getDefault().getBundle().getEntry(path);
            URL fileURL = Platform.getBundle("com.github.uchan_nos.c_helper").getEntry(path);
            try {
                URI fileURI = FileLocator.resolve(fileURL).toURI();

                System.out.println("  fileURI: " + fileURI.toString());

                result = new File(fileURI);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //result = Activator.getDefault().getBundle().getDataFile("/" + path);
        } else {
            //logger.finer("loading file from current directory");
            System.out.println("  loading file from current directory");

            result = new File(path);
        }

        if (result != null) {
            logger.fine("loaded: " + result.getAbsolutePath());
        }

        return result;
    }
}
