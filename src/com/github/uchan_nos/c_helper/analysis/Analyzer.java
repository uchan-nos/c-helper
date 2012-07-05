package com.github.uchan_nos.c_helper.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
    private OutputStreamWriter cfgWriter;

    public Analyzer() {
        IPath filePath = new Path(System.getProperty("user.home"));
        filePath = filePath.append("cfg.dot");
        OutputStreamWriter writer;
        try {
            writer = new FileWriter(filePath.toFile());
        } catch (IOException e) {
            writer = new OutputStreamWriter(System.out);
        }
        this.cfgWriter = writer;
    }

    public Analyzer(OutputStreamWriter cfgWriter) {
        this.cfgWriter = cfgWriter;
    }

    public void close() throws IOException {
        this.cfgWriter.close();
    }

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
        String graphString = new CFGPrinter(procToCFG).toDotString();
        try {
            cfgWriter.write(graphString);
        } catch (IOException e) {
            System.out.println("cannot write");
            System.out.println(graphString);
        }
    }

    private Map<String, CFG> createCFG(IASTTranslationUnit ast) {
        /*
        Map<String, CFG> procToCFG = new HashMap<String, CFG>();
        IASTDeclaration[] declarations = ast.getDeclarations();
        for (int i = 0; i < declarations.length; ++i) {
            IASTDeclaration decl = declarations[i];
            if (decl instanceof IASTFunctionDefinition) {
                IASTFunctionDefinition fd = (IASTFunctionDefinition) decl;
                String id = String.valueOf(fd.getDeclarator().getName()
                        .getSimpleID());
                ArrayList<GotoInfo> gotoInfoList = new ArrayList<GotoInfo>();
                CFG cfg = createCFG(fd.getBody(), gotoInfoList);
                // printCFG(cfg);
                applyGotoInfo(cfg, gotoInfoList);
                // printCFG(cfg);
                CFGNormalizer.normalize(cfg);
                procToCFG.put(id, cfg);
            }
        }
        return procToCFG;
        */

        return new CFGCreator(ast).create();
    }

    /*
    private static CFG createCFG(IASTStatement stmt,
            Collection<GotoInfo> gotoInfoList) {
        CFG cfg = new CFG();
        if (stmt instanceof IASTCompoundStatement) {
            IASTCompoundStatement s = (IASTCompoundStatement) stmt;
            IASTStatement[] sub = s.getStatements();

            if (sub.length > 0) {
                CFG subcfg = createCFG(sub[0], gotoInfoList);
                cfg.setEntryVertex(subcfg.entryVertex());
                cfg.add(subcfg);
                cfg.addBreakFrom(subcfg.breakFromVertices());
                Set<Vertex> exitVertices = subcfg.exitVertices();

                for (int i = 1; i < sub.length; ++i) {
                    subcfg = createCFG(sub[i], gotoInfoList);
                    cfg.add(subcfg, exitVertices, null);
                    cfg.addBreakFrom(subcfg.breakFromVertices());
                    exitVertices = subcfg.exitVertices();
                }
                cfg.setExitVertices(exitVertices);
            }
        } else if (stmt instanceof IASTIfStatement) {
            IASTIfStatement s = (IASTIfStatement) stmt;
            CFG thencfg = createCFG(s.getThenClause(), gotoInfoList);
            CFG elsecfg = s.getElseClause() != null ? createCFG(
                    s.getElseClause(), gotoInfoList) : null;
            CFG.Vertex root = new CFG.Vertex("if ("
                    + s.getConditionExpression().getRawSignature() + ")\\l");
            root.addASTNode(s);

            CFG.Vertex exitVertex = new CFG.Vertex(""); // ダミーノード
            cfg.add(exitVertex);

            cfg.add(root);
            cfg.add(thencfg);
            cfg.addBreakFrom(thencfg.breakFromVertices());
            cfg.add(new CFG.Edge(root, thencfg.entryVertex()));
            for (CFG.Vertex v : thencfg.exitVertices()) {
                cfg.add(new CFG.Edge(v, exitVertex));
            }
            // exitVertices.addAll(thencfg.exitVertices());

            if (elsecfg != null) {
                cfg.add(elsecfg);
                cfg.addBreakFrom(elsecfg.breakFromVertices());
                cfg.add(new CFG.Edge(root, elsecfg.entryVertex()));
                for (CFG.Vertex v : elsecfg.exitVertices()) {
                    cfg.add(new CFG.Edge(v, exitVertex));
                }
                // exitVertices.addAll(elsecfg.exitVertices());
            } else {
                cfg.add(new CFG.Edge(root, exitVertex));
                // exitVertices.add(root);
            }

            cfg.setEntryVertex(root);
            cfg.setExitVertices(exitVertex);
        } else if (stmt instanceof IASTLabelStatement) {
            IASTLabelStatement s = (IASTLabelStatement) stmt;
            CFG subcfg = createCFG(s.getNestedStatement(), gotoInfoList);
            CFG.Vertex labelVertex = new Vertex(s.getName().getRawSignature()
                    + ":\\l");
            labelVertex.addASTNode(s);
            cfg.add(labelVertex);

            // gotoからsubcfg.entryVertex()に入ってくるすべての辺を、labelVertexに入るように切り替える
            ArrayList<CFG.Edge> removeEdges = new ArrayList<CFG.Edge>();
            ArrayList<CFG.Edge> addEdges = new ArrayList<CFG.Edge>();
            for (CFG.Edge e : subcfg.edges()) {
                if (e.to() == subcfg.entryVertex()) {
                    if (e.from().getASTNodes().size() != 1) {
                        throw new RuntimeException("one vertex doesn't have exact one statement");
                    }
                    IASTNode n = e.from().getASTNodes().get(0);
                    if (n instanceof IASTGotoStatement) {
                        removeEdges.add(e);
                        addEdges.add(new CFG.Edge(e.from(), labelVertex));
                    }
                }
            }
            for (CFG.Edge e : removeEdges) {
                subcfg.remove(e);
            }

            cfg.add(subcfg);
            cfg.add(new CFG.Edge(labelVertex, subcfg.entryVertex()));
            for (CFG.Edge e : addEdges) {
                cfg.add(e);
            }

            cfg.setEntryVertex(labelVertex);
            cfg.setExitVertices(subcfg.exitVertices());
        } else if (stmt instanceof IASTGotoStatement) {
            IASTGotoStatement s = (IASTGotoStatement) stmt;
            CFG.Vertex v = new CFG.Vertex(stmt.getRawSignature() + "\\l");
            v.addASTNode(stmt);
            cfg.add(v);
            cfg.setEntryVertex(v);
            cfg.setExitVertices(v);
            gotoInfoList.add(new GotoInfo(v, s.getName().getRawSignature()));
        } else if (stmt instanceof IASTWhileStatement) {
            IASTWhileStatement s = (IASTWhileStatement) stmt;
            CFG subcfg = createCFG(s.getBody(), gotoInfoList);
            CFG.Vertex root = new CFG.Vertex("while ("
                    + s.getCondition().getRawSignature() + ")\\l");
            root.addASTNode(s);
            CFG.Vertex exit = new CFG.Vertex("");
            cfg.add(root);
            cfg.add(exit);
            cfg.add(subcfg);
            cfg.add(new CFG.Edge(root, subcfg.entryVertex()));
            cfg.add(new CFG.Edge(root, exit));
            for (CFG.Vertex v : subcfg.exitVertices()) {
                cfg.add(new CFG.Edge(v, root));
            }
            for (CFG.Vertex v : subcfg.breakFromVertices()) {
                cfg.add(new CFG.Edge(v, exit));
                for (CFG.Edge e : getAllBreakingEdgesToRemove(v, subcfg.edges(), exit)) {
                    cfg.remove(e);
                }
            }
            cfg.setEntryVertex(root);
            cfg.setExitVertices(exit);
        } else if (stmt instanceof IASTDoStatement) {
            IASTDoStatement s = (IASTDoStatement) stmt;
            CFG subcfg = createCFG(s.getBody(), gotoInfoList);
            CFG.Vertex root = new CFG.Vertex("do-while ("
                    + s.getCondition().getRawSignature() + ")\\l");
            root.addASTNode(s);
            CFG.Vertex exit = new CFG.Vertex("");
            cfg.add(root);
            cfg.add(exit);
            cfg.add(subcfg);
            cfg.add(new CFG.Edge(root, exit));
            for (CFG.Vertex v : subcfg.exitVertices()) {
                cfg.add(new CFG.Edge(v, root));
            }
            for (CFG.Vertex v : subcfg.breakFromVertices()) {
                cfg.add(new CFG.Edge(v, exit));
                for (CFG.Edge e : getAllBreakingEdgesToRemove(v, subcfg.edges(), exit)) {
                    cfg.remove(e);
                }
            }
            cfg.add(new CFG.Edge(root, subcfg.entryVertex()));
            cfg.setEntryVertex(subcfg.entryVertex());
            cfg.setExitVertices(exit);
        } else if (stmt instanceof IASTForStatement) {
            IASTForStatement s = (IASTForStatement) stmt;
            CFG subcfg = createCFG(s.getBody(), gotoInfoList);

            CFG.Vertex initVertex = new CFG.Vertex(s.getInitializerStatement().getRawSignature() + "\\l");
            CFG.Vertex condVertex = new CFG.Vertex(s.getConditionExpression().getRawSignature() + "\\l");
            CFG.Vertex iterVertex = new CFG.Vertex(s.getIterationExpression().getRawSignature() + "\\l");
            initVertex.addASTNode(s.getInitializerStatement());
            condVertex.addASTNode(s.getConditionExpression());
            iterVertex.addASTNode(s.getIterationExpression());

            CFG.Vertex exit = new CFG.Vertex("");

            cfg.add(initVertex);
            cfg.add(condVertex);
            cfg.add(iterVertex);
            cfg.add(exit);

            cfg.add(new CFG.Edge(initVertex, condVertex));
            cfg.add(new CFG.Edge(iterVertex, condVertex));
            cfg.add(new CFG.Edge(condVertex, exit));

            cfg.add(subcfg);
            cfg.add(new CFG.Edge(condVertex, subcfg.entryVertex()));
            for (CFG.Vertex v : subcfg.exitVertices()) {
                cfg.add(new CFG.Edge(v, iterVertex));
            }
            for (CFG.Vertex v : subcfg.breakFromVertices()) {
                cfg.add(new CFG.Edge(v, exit));
                for (CFG.Edge e : getAllBreakingEdgesToRemove(v, subcfg.edges(), exit)) {
                    cfg.remove(e);
                }
            }
            cfg.setEntryVertex(initVertex);
            cfg.setExitVertices(exit);
        } else if (stmt instanceof IASTSwitchStatement) {
            IASTSwitchStatement s = (IASTSwitchStatement) stmt;
            CFG subcfg = createCFG(s.getBody(), gotoInfoList);
            CFG.Vertex root = new CFG.Vertex("switch ("
                    + s.getControllerExpression().getRawSignature() + ")\\l");
            root.addASTNode(s);
            CFG.Vertex exit = new CFG.Vertex("");
            cfg.add(root);
            cfg.add(exit);
            cfg.add(subcfg);
            for (CFG.Vertex v : subcfg.exitVertices()) {
                cfg.add(new CFG.Edge(v, exit));
            }

            for (CFG.Vertex v : subcfg.vertices()) {
                if (v.getASTNodes().size() != 1) {
                    throw new RuntimeException("one vertex doesn't have exact one statement");
                }

                IASTNode n = v.getASTNodes().get(0);
                if (n instanceof IASTCaseStatement || n instanceof IASTDefaultStatement) {
                    cfg.add(new CFG.Edge(root, v));
                }
            }
            for (CFG.Vertex v : subcfg.breakFromVertices()) {
                cfg.add(new CFG.Edge(v, exit));
                for (CFG.Edge e : getAllBreakingEdgesToRemove(v, subcfg.edges(), subcfg.exitVertices())) {
                    cfg.remove(e);
                }
            }

            cfg.setEntryVertex(root);
            cfg.setExitVertices(exit);
        } else if (stmt instanceof IASTBreakStatement) {
            CFG.Vertex v = new CFG.Vertex(stmt.getRawSignature() + "\\l");
            v.addASTNode(stmt);
            cfg.add(v);
            cfg.addBreakFrom(v);
            cfg.setEntryVertex(v);
            cfg.setExitVertices(v);
        } else {
            CFG.Vertex v = new CFG.Vertex(stmt.getRawSignature() + "\\l");
            v.addASTNode(stmt);
            cfg.add(v);
            cfg.setEntryVertex(v);
            cfg.setExitVertices(v);
        }
        return cfg;
    }

    private static void applyGotoInfo(CFG cfg, Collection<GotoInfo> gotoInfoList) {
        Map<String, CFG.Vertex> labeledStatement = new HashMap<String, CFG.Vertex>();
        for (CFG.Vertex v : cfg.vertices()) {
            ArrayList<IASTNode> astNodes = v.getASTNodes();
            if (astNodes.size() > 0
                    && astNodes.get(0) instanceof IASTLabelStatement) {
                IASTLabelStatement s = (IASTLabelStatement) astNodes.get(0);
                labeledStatement.put(s.getName().getRawSignature(), v);
            }
        }
        for (GotoInfo gi : gotoInfoList) {
            CFG.Vertex gotoVertex = labeledStatement.get(gi.toName());

            for (Iterator<CFG.Edge> it = cfg.edges().iterator(); it.hasNext();) {
                CFG.Edge e = it.next();
                if (e.from() == gi.from()) {
                    it.remove();
                }
            }

            if (gotoVertex != null) {
                cfg.add(new CFG.Edge(gi.from(), gotoVertex));
            }
        }
    }

    private static ArrayList<CFG.Edge> getAllBreakingEdgesToRemove(CFG.Vertex breakVertex, Collection<CFG.Edge> edges, Collection<CFG.Vertex> except) {
        ArrayList<CFG.Edge> remove = new ArrayList<CFG.Edge>();
        for (CFG.Edge e : edges) {
            if (e.from() == breakVertex && !except.contains(e.to())) {
                remove.add(e);
            }
        }
        return remove;
    }

    private static ArrayList<CFG.Edge> getAllBreakingEdgesToRemove(CFG.Vertex breakVertex, Collection<CFG.Edge> edges, CFG.Vertex except) {
        ArrayList<CFG.Vertex> excepts = new ArrayList<CFG.Vertex>();
        excepts.add(except);
        return getAllBreakingEdgesToRemove(breakVertex, edges, excepts);
    }
    */
}
