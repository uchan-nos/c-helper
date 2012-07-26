package com.github.uchan_nos.c_helper.analysis;

import java.io.IOException;

import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.SavedFilesProvider;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.github.uchan_nos.c_helper.util.Util;

@SuppressWarnings("restriction")
public class MyFileContentProvider extends InternalFileContentProvider {
    // 標準ヘッダの場所
    final private String stdHeaderDir;

    public MyFileContentProvider(String stdHeaderDir) {
        this.stdHeaderDir = stdHeaderDir;
    }

    @Override
    public InternalFileContent getContentForInclusion(String filePathString,
            IMacroDictionary macroDictionary) {
        // split file path
        IPath filePath = new Path(filePathString);
        String filename = filePath.lastSegment();

        IPath stdPath = new Path(this.stdHeaderDir).append(filename);
        //if (stdHeaders.containsKey(filename)) {
        if (stdPath.toFile().exists()) {
            // filenameが標準ライブラリだったら組み込みのヘッダ内容を返す
            try {
                return (InternalFileContent) FileContent.create(
                        stdPath.toOSString(),
                        Util.readFileAll(stdPath.toFile()).toCharArray());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else if (getInclusionExists(filePathString)) {
            // filenameは標準ライブラリではないが、filePathStringにはヘッダがあった
            return SavedFilesProvider.getInstance().getContentForInclusion(
                    filePathString, macroDictionary);
        } else {
            // filenameが標準ライブラリでもなく、filePathStringが見つからないならヘッダが見つからないことにする
            return null;
        }
    }

    @Override
    public InternalFileContent getContentForInclusion(IIndexFileLocation ifl,
            String astPath) {
        return SavedFilesProvider.getInstance().getContentForInclusion(ifl, astPath);
    }

}
