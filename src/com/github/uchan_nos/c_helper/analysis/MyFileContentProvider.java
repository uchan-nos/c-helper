package com.github.uchan_nos.c_helper.analysis;

import java.io.InputStream;
import java.io.IOException;

import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.SavedFilesProvider;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.github.uchan_nos.c_helper.util.FileLoader;
import com.github.uchan_nos.c_helper.util.Util;

@SuppressWarnings("restriction")
public class MyFileContentProvider extends InternalFileContentProvider {
    // 標準ヘッダの場所
    final private String stdHeaderDir;

    public MyFileContentProvider(String stdHeaderDir) {
        this.stdHeaderDir = stdHeaderDir;
    }

    @Override
    public InternalFileContent getContentForInclusion(String originalFilePathString,
            IMacroDictionary macroDictionary) {
        System.out.println("MyFileContentProvider#getCOntentForInclusion");

        System.out.println("  splitting file name: " + originalFilePathString);
        // split file path
        String filename = new Path(originalFilePathString).lastSegment();

        IPath constructedFilePath = new Path(this.stdHeaderDir).append(filename);
        String constructedFilePathString = constructedFilePath.toOSString();
        System.out.println("  constructedFilePath: " + constructedFilePathString);

        //File file = FileLoader.getInstance().load(stdPath.toOSString());

        InputStream inputStream = null;
        try {
            System.out.println("  opening stream for " + constructedFilePathString);
            inputStream = FileLoader.getInstance().openStream(
                    constructedFilePathString);
            System.out.println("  input stream was successfully opened");
            return (InternalFileContent) FileContent.create(
                    constructedFilePathString,
                    Util.readInputStreamAll(inputStream).toCharArray());
        } catch (IOException e) {
            System.out.println("  failed to open input stream");
            inputStream = null;
        }

        if (inputStream == null) {
            try {
                System.out.println("  opening stream for " + originalFilePathString);
                inputStream = FileLoader.getInstance().openStream(
                        originalFilePathString);
                System.out.println("  input stream was successfully opened");
            return (InternalFileContent) FileContent.create(
                    originalFilePathString,
                    Util.readInputStreamAll(inputStream).toCharArray());
            } catch (IOException e) {
                System.out.println("  failed to open input stream");
                inputStream = null;
            }
        }

        return null;

    }

    @Override
    public InternalFileContent getContentForInclusion(IIndexFileLocation ifl,
            String astPath) {
        System.err.println("handle it to SavedFilesProvider: " + astPath);
        return SavedFilesProvider.getInstance().getContentForInclusion(ifl, astPath);
    }

}
