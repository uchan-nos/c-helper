package com.github.uchan_nos.c_helper.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.github.uchan_nos.c_helper.analysis.CFG.Vertex;
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
    
    private void printCFG(CFG cfg) {
        try {
            IPath filePath = new Path(System.getProperty("user.home"));
            filePath = filePath.append("cfg.dot");
            FileWriter writer = new FileWriter(filePath.toFile());
            writer.write("digraph {\n");
            writer.write(toDot("debug", cfg));
            writer.write('\n');
            writer.write("}\n");
            writer.close();
        } catch (IOException e) {
            System.out.println("cannot create a file to write");
            System.out.println(toDot("debug", cfg));
        }
    }
    
    private String toDot(String name, CFG cfg) {
        StringBuilder sb = new StringBuilder();
        Set<CFG.Vertex> vertices = cfg.vertices();
        Set<CFG.Edge> edges = cfg.edges();
        
        // vertex attribute lines
        sb.append(name + "[shape=parallelogram];\n");
        for (CFG.Vertex v : vertices) {
            String label = v.label().replace("\"", "\\\"").replace("\\n", "|n").replace("\n", "\\n");
            sb.append(getVertexName(v) + " [shape=box,label=\"" + label + "\"];\n");
        }
        
        // edges
        if (cfg.entryVertex() != null) {
	        sb.append(name + " -> " + getVertexName(cfg.entryVertex()) + ";\n");
	        for (CFG.Edge edge : edges) {
	            sb.append(getVertexName(edge.from()) + " -> " + getVertexName(edge.to()) + ";\n");
	        }
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
                //optimizeCFG(cfg);
                CFGNormalizer.normalize(cfg);
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
                Set<Vertex> exitVertices = subcfg.exitVertices();
                
                for (int i = 1; i < sub.length; ++i) {
                	subcfg = createCFG(sub[i]);
                	cfg.add(subcfg, exitVertices, null);
                	exitVertices = subcfg.exitVertices();
                }
                cfg.setExitVertices(exitVertices);
            }
        } else if (stmt instanceof IASTIfStatement) {
            IASTIfStatement s = (IASTIfStatement) stmt;
            CFG thencfg = createCFG(s.getThenClause());
            CFG elsecfg = s.getElseClause() != null ? createCFG(s.getElseClause()) : null;
            CFG.Vertex root = new CFG.Vertex("if (" + s.getConditionExpression().getRawSignature() + ")\\l");
            root.addASTNode(s);

            ArrayList<CFG.Vertex> exitVertices = new ArrayList<CFG.Vertex>();
            CFG.Vertex exitVertex = new CFG.Vertex(""); // ダミーノード
            cfg.add(exitVertex);
            exitVertices.add(exitVertex);

            cfg.add(root);
            cfg.add(thencfg);
            cfg.add(new CFG.Edge(root, thencfg.entryVertex()));
            for (CFG.Vertex v : thencfg.exitVertices()) {
            	cfg.add(new CFG.Edge(v, exitVertex));
            }
            //exitVertices.addAll(thencfg.exitVertices());

            if (elsecfg != null) {
                cfg.add(elsecfg);
                cfg.add(new CFG.Edge(root, elsecfg.entryVertex()));
                for (CFG.Vertex v : elsecfg.exitVertices()) {
                	cfg.add(new CFG.Edge(v, exitVertex));
                }
                //exitVertices.addAll(elsecfg.exitVertices());
            } else {
            	cfg.add(new CFG.Edge(root, exitVertex));
            	//exitVertices.add(root);
            }
            
            cfg.setEntryVertex(root);
            cfg.setExitVertices(exitVertices);
        } else {
            CFG.Vertex v = new CFG.Vertex(stmt.getRawSignature() + "\\l");
            v.addASTNode(stmt);
            cfg.add(v);
            cfg.setEntryVertex(v);
            cfg.setExitVertices(v);
        }
        return cfg;
    }
}
