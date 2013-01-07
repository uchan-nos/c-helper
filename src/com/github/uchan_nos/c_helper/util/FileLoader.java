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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import com.github.uchan_nos.c_helper.Activator;

public class FileLoader {
    private final Logger logger = Activator.getLogger();

    private static FileLoader instance = new FileLoader();
    public static FileLoader getInstance() {
        return instance;
    }

    private final String PLUGIN_DIRECTORY;


    public FileLoader() {
        PLUGIN_DIRECTORY = System.getenv("PLUGIN_DIR");
    }

    public InputStream openStreamForEmbeddedFile(String path) throws FileNotFoundException, IOException {
        logger.finest("path=" + path + ", PLUGIN_DIRECTORY=" + PLUGIN_DIRECTORY);

        if (PLUGIN_DIRECTORY != null) {
            logger.finest("loading file from PLUGIN_DIRECTORY");

            return new FileInputStream(new File(PLUGIN_DIRECTORY, path));
        } else if (Activator.getDefault() != null) {
            logger.finest("  loading file from Activator default bundle");

            URL fileURL = Activator.getDefault().getBundle().getEntry(path);
            if (fileURL == null) {
                return null;
            }

            try {
                URI fileURI = FileLocator.resolve(fileURL).toURI();
                logger.finest("fileURI: " + fileURI.toString());
            } catch (URISyntaxException e) {
                logger.finest("couldn't resolve file URI: " + e);
            }

            return fileURL.openStream();

        } else {
            logger.finest("loading file from current directory");

            return new FileInputStream(new File(path));
        }

    }

    public InputStream openStreamForUserFile(String path, boolean pathIsOfWorkspace)
        throws FileNotFoundException, IOException, CoreException {
        if (pathIsOfWorkspace) {
            IWorkspace ws = ResourcesPlugin.getWorkspace();
            IFile originalFile = ws.getRoot().getFile(new Path(path));
            InputStream inputStream = originalFile.getContents();
            return inputStream;
        } else {
            return new FileInputStream(new File(path));
        }
    }

    public File load(String path) {
        logger.finest("path=" + path + ", PLUGIN_DIRECTORY=" + PLUGIN_DIRECTORY);

        File result = null;
        if (PLUGIN_DIRECTORY != null) {
            //logger.finer("loading file from PLUGIN_DIRECTORY");
            logger.finest("loading file from PLUGIN_DIRECTORY");

            result = new File(PLUGIN_DIRECTORY, path);
        } else if (Activator.getDefault() != null) {
            //logger.finer("loading file from Activator default bundle");
            logger.finest("loading file from Activator default bundle");

            //URL fileURL = Activator.getDefault().getBundle().getEntry(path);
            URL fileURL = Platform.getBundle("com.github.uchan_nos.c_helper").getEntry(path);
            try {
                URI fileURI = FileLocator.resolve(fileURL).toURI();

                logger.finest("  fileURI: " + fileURI.toString());

                result = new File(fileURI);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //result = Activator.getDefault().getBundle().getDataFile("/" + path);
        } else {
            //logger.finer("loading file from current directory");
            logger.finest("loading file from current directory");

            result = new File(path);
        }

        if (result != null) {
            logger.finest("loaded: " + result.getAbsolutePath());
        }

        return result;
    }
}
