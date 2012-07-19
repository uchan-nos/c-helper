package com.github.uchan_nos.c_helper.analysis;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.SavedFilesProvider;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;

public class MyFileContentProvider extends InternalFileContentProvider {
    final private Map<String, char[]> stdHeaders;

    public MyFileContentProvider() {
        stdHeaders = new HashMap<String, char[]>();
        stdHeaders.put("/usr/include/stdio.h", "int puts(const char* s);".toCharArray());
        stdHeaders.put("/usr/include/stdlib.h", "".toCharArray());
    }

    @SuppressWarnings("restriction")
    @Override
    public InternalFileContent getContentForInclusion(String filePath,
            IMacroDictionary macroDictionary) {
        if (!getInclusionExists(filePath)) {
            return null;
        }

        if (stdHeaders.containsKey(filePath)) {
            return (InternalFileContent) FileContent.create(filePath, stdHeaders.get(filePath));
        }
        return SavedFilesProvider.getInstance().getContentForInclusion(filePath, macroDictionary);
    }

    @SuppressWarnings("restriction")
    @Override
    public InternalFileContent getContentForInclusion(IIndexFileLocation ifl,
            String astPath) {
        return SavedFilesProvider.getInstance().getContentForInclusion(ifl, astPath);
    }

}
