package com.github.uchan_nos.c_helper.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.internal.core.envvar.EnvironmentVariableManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.omg.CORBA.Environment;

import com.github.uchan_nos.c_helper.exceptions.InvalidEditorPartException;

public class Analyzer {
    public void analyze(IEditorPart activeEditorPart)
            throws InvalidEditorPartException {
        if (activeEditorPart instanceof ITextEditor) {
            ITextEditor textEditorPart = (ITextEditor) activeEditorPart;
            IDocument documentToAnalyze = textEditorPart.getDocumentProvider()
                    .getDocument(textEditorPart.getEditorInput());
            analyze(textEditorPart.getTitle(), documentToAnalyze.get()
                    .toCharArray());
        } else {
            throw new InvalidEditorPartException(
                    "We need a class implementing ITextEditor");
        }
    }

    public void analyze(String filePath, char[] source) {
        ILanguage language = GCCLanguage.getDefault();

        FileContent reader = FileContent.create(filePath, source);

        Map<String, String> macroDefinitions = null;
        String[] includeSearchPath = null;
        IScannerInfo scanInfo = new ScannerInfo(macroDefinitions,
                includeSearchPath);

        IncludeFileContentProvider fileCreator = IncludeFileContentProvider
                .getEmptyFilesProvider();
        IIndex index = null;
        int options = 0;
        IParserLogService log = new DefaultLogService();

        try {
            IASTTranslationUnit translationUnit = language
                    .getASTTranslationUnit(reader, scanInfo, fileCreator,
                            index, options, log);
            Map<String, CFG> procToCFG = createCFG(translationUnit);
            printCFG(procToCFG);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }
    
    private void printCFG(Map<String, CFG> procToCFG) {
        try {
            IPath filePath = new Path(System.getProperty("user.home"));
            filePath = filePath.append("cfg.dot");
            FileWriter writer = new FileWriter(filePath.toFile());
            writer.write("digraph {\n");
            for (Entry<String, CFG> entry : procToCFG.entrySet()) {
                writer.write(toDot(entry.getKey(), entry.getValue()));
                writer.write('\n');
            }
            writer.write("}\n");
            writer.close();
        } catch (IOException e) {
            System.out.println("cannot create a file to write");
            for (Entry<String, CFG> entry : procToCFG.entrySet()) {
                System.out.println(toDot(entry.getKey(), entry.getValue()));
            }
        }
    }
    
    private String toDot(String name, CFG cfg) {
        StringBuilder sb = new StringBuilder();
        ArrayList<CFG.Vertex> vertices = cfg.vertices();
        ArrayList<CFG.Edge> edges = cfg.edges();
        
        // vertex attribute lines
        sb.append(name + "[shape=parallelogram];\n");
        for (CFG.Vertex v : vertices) {
            String label = v.label().replace("\"", "\\\"").replace("\n", "\\n");
            sb.append(getVertexName(v) + " [shape=box,label=\"" + label + "\"];\n");
        }
        
        // edges
        sb.append(name + " -> " + getVertexName(cfg.entryVertex()) + ";\n");
        for (CFG.Edge edge : edges) {
            sb.append(getVertexName(edge.from()) + " -> " + getVertexName(edge.to()) + ";\n");
        }
        
        return sb.toString();
    }
    
    private String getVertexName(CFG.Vertex v) {
        return "v" + getObjectAddress(v);
    }
    
    private String getObjectAddress(Object o) {
        String addr = o.toString();
        addr = addr.substring(addr.lastIndexOf('@') + 1);
        return addr;
    }

    private Map<String, CFG> createCFG(IASTTranslationUnit ast) {
        Map<String, CFG> procToCFG = new HashMap<String, CFG>();
        IASTDeclaration[] declarations = ast.getDeclarations();
        for (int i = 0; i < declarations.length; ++i) {
            IASTDeclaration decl = declarations[i];
            if (decl instanceof IASTFunctionDefinition) {
                IASTFunctionDefinition fd = (IASTFunctionDefinition) decl;
                String id = String.valueOf(fd.getDeclarator().getName().getSimpleID());
                CFG cfg = createCFG(fd.getBody());
                procToCFG.put(id, cfg);
            }
        }
        return procToCFG;
    }

    private CFG createCFG(IASTStatement stmt) {
        CFG cfg = new CFG();
        if (stmt instanceof IASTCompoundStatement) {
            IASTCompoundStatement s = (IASTCompoundStatement) stmt;
            IASTStatement[] sub = s.getStatements();

            if (sub.length > 0) {
                CFG subcfg = createCFG(sub[0]);
                cfg.setEntryVertex(subcfg.entryVertex());
                cfg.add(subcfg);
                CFG prev = subcfg;

                for (int i = 1; i < sub.length; ++i) {
                    subcfg = createCFG(sub[i]);
                    cfg.add(subcfg, prev.exitVertices(), null);
                    prev = subcfg;
                }
            }
        } else {
            CFG.Vertex v = new CFG.Vertex(stmt.getRawSignature());
            v.addASTNode(stmt);
            cfg.add(v);
            cfg.setEntryVertex(v);
            cfg.setExitVertices(v);
        }
        return cfg;
    }
}
