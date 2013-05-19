package com.github.uchan_nos.c_helper.analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.logging.Logger;

import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.SavedFilesProvider;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.github.uchan_nos.c_helper.Activator;

import com.github.uchan_nos.c_helper.util.FileLoader;
import com.github.uchan_nos.c_helper.util.Util;

@SuppressWarnings("restriction")
public class MyFileContentProvider extends InternalFileContentProvider {
    private final Logger logger = Activator.getLogger();

    // 標準ヘッダの場所
    final private String stdHeaderDir;
    final private FileInfo fileInfoToParse;

    public MyFileContentProvider(String stdHeaderDir, FileInfo fileInfoToParse) {
        this.stdHeaderDir = stdHeaderDir;
        this.fileInfoToParse = fileInfoToParse;
    }

    @Override
    public InternalFileContent getContentForInclusion(String originalFilePathString,
            IMacroDictionary macroDictionary) {
        logger.finest("MyFileContentProvider#getCOntentForInclusion("
            + originalFilePathString + ", " + macroDictionary + ")");

        logger.finest("  splitting file name: " + originalFilePathString);
        // split file path
        String filename = new Path(originalFilePathString).lastSegment();

        // まず標準ライブラリのディレクトリから読み込んでみる
        IPath constructedFilePath = new Path(this.stdHeaderDir).append(filename);
        String constructedFilePathString = constructedFilePath.toString();
        logger.finest("  constructedFilePath: " + constructedFilePathString);

        //File file = FileLoader.getInstance().load(stdPath.toOSString());

        InputStream inputStream = null;
        try {
            logger.finest("  opening stream for " + constructedFilePathString);
            inputStream = FileLoader.getInstance().openStreamForEmbeddedFile(
                    constructedFilePathString);
            if (inputStream != null) {
                logger.finest("  input stream was successfully opened");
                return (InternalFileContent) FileContent.create(
                        constructedFilePathString,
                        Util.readInputStreamAll(inputStream).toCharArray());
            }
        } catch (IOException e) {
            logger.finest("  failed to open input stream: " + e);
            inputStream = null;
        }

        // 標準ライブラリのディレクトリに存在しないヘッダファイルなら、
        // ユーザが作成したヘッダと見なしてローカルから読み込んでみる
        if (inputStream == null) {
            try {
                logger.finest("  opening stream for " + originalFilePathString);
                inputStream = FileLoader.getInstance().openStreamForUserFile(
                        originalFilePathString,
                        fileInfoToParse.isFileInWorkspace());
                logger.finest("  input stream was successfully opened");
                return (InternalFileContent) FileContent.create(
                        originalFilePathString,
                        Util.readInputStreamAll(inputStream).toCharArray());

            } catch (IOException e) {
                logger.finest("failed to open input stream: " + e);
                inputStream = null;
            } catch (CoreException e) {
                logger.finest("failed to open input stream: " + e);
                inputStream = null;
            } catch (IllegalStateException e) {
                logger.finest("failed to open input stream: " + e);
                inputStream = null;
            }
        }

        return null;

    }

    @Override
    public InternalFileContent getContentForInclusion(IIndexFileLocation ifl,
            String astPath) {
        logger.finest("handle it to SavedFilesProvider: " + astPath);
        return SavedFilesProvider.getInstance().getContentForInclusion(ifl, astPath);
    }

}
