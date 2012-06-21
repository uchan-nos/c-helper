package com.github.uchan_nos.c_helper.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
        ArrayList<CFG.Vertex> vertices = cfg.vertices();
        ArrayList<CFG.Edge> edges = cfg.edges();
        
        // vertex attribute lines
        sb.append(name + "[shape=parallelogram];\n");
        for (CFG.Vertex v : vertices) {
            String label = v.label().replace("\"", "\\\"").replace("\\n", "|n").replace("\n", "\\n");
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
                optimizeCFG(cfg);
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
                
                cfg.setExitVertices(prev.exitVertices());
            }
        } else if (stmt instanceof IASTIfStatement) {
            IASTIfStatement s = (IASTIfStatement) stmt;
            CFG thencfg = createCFG(s.getThenClause());
            CFG elsecfg = s.getElseClause() != null ? createCFG(s.getElseClause()) : null;
            CFG.Vertex v = new CFG.Vertex("if (" + s.getConditionExpression().getRawSignature() + ")");
            v.addASTNode(s);

            ArrayList<CFG.Vertex> exitVertices = new ArrayList<CFG.Vertex>();
            exitVertices.add(v);

            cfg.add(v);
            cfg.add(thencfg);
            cfg.add(new CFG.Edge(v, thencfg.entryVertex()));
            exitVertices.addAll(thencfg.exitVertices());

            if (elsecfg != null) {
                cfg.add(elsecfg);
                cfg.add(new CFG.Edge(v, elsecfg.entryVertex()));
                exitVertices.addAll(elsecfg.exitVertices());
            }
            
            cfg.setEntryVertex(v);
            cfg.setExitVertices(exitVertices);
        } else {
            CFG.Vertex v = new CFG.Vertex(stmt.getRawSignature());
            v.addASTNode(stmt);
            cfg.add(v);
            cfg.setEntryVertex(v);
            cfg.setExitVertices(v);
        }
        return cfg;
    }
    
    private Map<CFG.Vertex, ArrayList<CFG.Edge>> getEntryEdges(CFG cfg) {
        // 指定されたノードに入ってくる辺の集合
        // entryEdges.get(v) -> vに入ってくる辺の集合
        Map<CFG.Vertex, ArrayList<CFG.Edge>> entryEdges = new HashMap<CFG.Vertex, ArrayList<CFG.Edge>>();
        
        for (CFG.Edge edge : cfg.edges()) {
            ArrayList<CFG.Edge> entry = entryEdges.get(edge.to());
            if (entry == null) {
                entry = new ArrayList<CFG.Edge>();
                entryEdges.put(edge.to(), entry);
            }
            entry.add(edge);
        }
        
        return entryEdges;
    }
    
    private Map<CFG.Vertex, ArrayList<CFG.Edge>> getExitEdges(CFG cfg) {
        
        // 指定されたノードから出ていく辺の集合
        // exitEdges.get(v) -> vから出ていく辺の集合
        Map<CFG.Vertex, ArrayList<CFG.Edge>> exitEdges = new HashMap<CFG.Vertex, ArrayList<CFG.Edge>>();
        
        for (CFG.Edge edge : cfg.edges()) {
            ArrayList<CFG.Edge> exit = exitEdges.get(edge.from());
            if (exit == null) {
                exit = new ArrayList<CFG.Edge>();
                exitEdges.put(edge.from(), exit);
            }
            exit.add(edge);
        }
         
        return exitEdges;
    }
   
    private void optimizeCFG(CFG cfg) {
        
        class Util {
            // 指定されたノードに入ってくる辺の集合
            // entryEdges.get(v) -> vに入ってくる辺の集合
            private Map<CFG.Vertex, ArrayList<CFG.Edge>> entryEdges;
            
            // 指定されたノードから出ていく辺の集合
            // exitEdges.get(v) -> vから出ていく辺の集合
            private Map<CFG.Vertex, ArrayList<CFG.Edge>> exitEdges;
            
            private CFG cfg;
            
            public Util(CFG cfg) {
                this.cfg = cfg;
                this.entryEdges = getEntryEdges(cfg);
                this.exitEdges = getExitEdges(cfg);
            }
            
            /**
             * 指定された辺の前後の頂点をマージする.
             * 辺edgeが与えられたとき、edge.to()をedge.from()にマージする.
             * @param edge マージする辺
             */
            public void merge(CFG.Edge edge) {
                CFG.Vertex from = edge.from();
                CFG.Vertex to = edge.to();
                
                // toをfromに統合
                from.setLabel(from.label() + "\\l" + to.label());
                for (IASTNode node : to.getASTNodes()) {
                    from.addASTNode(node);
                }
                
                for (CFG.Edge e : this.exitEdges.get(to)) {
                    CFG.Edge newEdge = new CFG.Edge(from, e.to());
                    this.cfg.add(newEdge);
                    this.exitEdges.get(from).add(newEdge);
                    this.entryEdges.get(e.to()).add(newEdge);
                }
            }
            
            /**
             * 指定された頂点と、その頂点に接続されている辺を削除する.
             * @param v 削除する頂点
             */
            public void remove(CFG.Vertex v) {
                this.cfg.remove(v);
                
                for (CFG.Edge e : this.entryEdges.get(v)) {
                    // e.to()は必ずvと一致する
                    this.cfg.remove(e);
                    this.exitEdges.get(e.from()).remove(e);
                }
                for (CFG.Edge e : this.exitEdges.get(v)) {
                    // e.from()は必ずvと一致する
                    this.cfg.remove(e);
                    this.entryEdges.get(e.to()).remove(e);
                }
                this.cfg.remove(v);
                this.entryEdges.remove(v);
                this.exitEdges.remove(v);
            }
            
            public boolean canMerge(CFG.Edge edge) {
                return this.exitEdges.get(edge.from()).size() == 1 &&
                        this.entryEdges.get(edge.to()).size() == 1;
            }
        }
        Util util = new Util(cfg);
                
        boolean modified;
        
        do {
            modified = false;
            CFG.Edge edgeToMerge = null;
            
            ArrayList<CFG.Edge> edgesToAdd = new ArrayList<CFG.Edge>();
            
            for (CFG.Edge edge : cfg.edges()) {
                if (util.canMerge(edge)) {
                    // まとめられる
                    edgeToMerge = edge;
                    break;
                }
            }
            
            if (edgeToMerge != null) {
                modified = true;
                
                // toをfromに統合
                util.merge(edgeToMerge);
                
                // toとtoに接続されている辺を削除
                util.remove(edgeToMerge.to());
                // 一つまとめたらまた最初から。
            }
            
            printCFG(cfg);
        } while (modified == true);
    }
}
