package com.github.uchan_nos.c_helper.analysis;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;

import com.github.uchan_nos.c_helper.util.Util;

public class ASTPrinter {
    private IASTNode ast;
    private StringBuilder dotBuilder = null;
    private HashMap<IASTNode, Integer> nodeIdMap = null;
    final private String nodePrefix;
    final private String sourceCodeLineDelimiter;
    public ASTPrinter(IASTNode ast, String nodePrefix, String sourceCodeLineDelimiter) {
        this.ast = ast;
        this.nodePrefix = nodePrefix;
        this.sourceCodeLineDelimiter = sourceCodeLineDelimiter;
    }

    public String toDot(boolean includeGraphDecl) {
        this.dotBuilder = new StringBuilder();
        this.nodeIdMap = new HashMap<IASTNode, Integer>();

        allocateId(this.ast);

        if (includeGraphDecl) {
            this.dotBuilder.append("digraph AST {\n");
        }
        createVertex(this.ast);
        createEdge(this.ast);
        if (includeGraphDecl) {
            this.dotBuilder.append("}\n");
        }

        return this.dotBuilder.toString();
    }

    private void allocateId(IASTNode node) {
        nodeIdMap.put(node, nodeIdMap.size());
        for (IASTNode child : node.getChildren()) {
            allocateId(child);
        }
    }

    private void createVertex(IASTNode node) {
        if (node.getFileLocation() != null) {
            String sig = node.getRawSignature();
            if (sig.indexOf(sourceCodeLineDelimiter) != -1) {
                sig = sig.substring(0, sig.indexOf(sourceCodeLineDelimiter));
            }
            sig = sig.replace("\"", "\\\"").replace("\\n", "|n").replace("\n", "\\n");
            this.dotBuilder.append(nodePrefix);
            this.dotBuilder.append('v');
            this.dotBuilder.append(nodeIdMap.get(node));
            this.dotBuilder.append(" [shape=box,label=\"");
            this.dotBuilder.append(node.getClass().getSimpleName());
            //this.dotBuilder.append("\\l");
            //this.dotBuilder.append(
            //    node.getRawSignature().replace("\"", "\\\"").replace("\\n", "|n").replace("\n", "\\n"));
            //this.dotBuilder.append("\\l");
            this.dotBuilder.append("\\l");
            this.dotBuilder.append(node.getFileLocation().getStartingLineNumber());
            this.dotBuilder.append(":");
            this.dotBuilder.append(sig);
            this.dotBuilder.append("\\l");
            this.dotBuilder.append("\"]\n");
            for (IASTNode child : node.getChildren()) {
                createVertex(child);
            }
        }
    }

    private void createEdge(IASTNode node) {
        int i = 0;
        for (IASTNode child : node.getChildren()) {
            this.dotBuilder.append(nodePrefix);
            this.dotBuilder.append('v');
            this.dotBuilder.append(nodeIdMap.get(node));
            this.dotBuilder.append(" -> ");
            this.dotBuilder.append(nodePrefix);
            this.dotBuilder.append('v');
            this.dotBuilder.append(nodeIdMap.get(child));
            this.dotBuilder.append(" [label=\"");
            this.dotBuilder.append(i);
            this.dotBuilder.append("\"]\n");
            createEdge(child);

            ++i;
        }
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            String inputFilename = args[0];
            File inputFile = new File(inputFilename);

            try {
                String fileContent = Util.readFileAll(inputFile, "UTF-8");
                IASTTranslationUnit translationUnit =
                        new Parser(new FileInfo(inputFilename, false), fileContent).parse();
                System.out.println("digraph AST {\n");

                int declCount = 0;
                for (IASTDeclaration declaration : translationUnit.getDeclarations()) {
                    if (declaration.isPartOfTranslationUnitFile()) {
                        String dot =
                                new ASTPrinter(declaration, "decl" + declCount, "\n").toDot(false);
                        System.out.print(dot);
                        declCount++;
                    }
                }
                System.out.println("}\n");
            } catch (CoreException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
