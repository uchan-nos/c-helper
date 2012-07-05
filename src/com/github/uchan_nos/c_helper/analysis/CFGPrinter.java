package com.github.uchan_nos.c_helper.analysis;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.IASTNode;

import com.github.uchan_nos.c_helper.analysis.CFG.Vertex;

public class CFGPrinter {
    private Map<String, CFG> procToCFG;
    private Map<CFG.Vertex, Integer> vertexNames = null;

    public CFGPrinter(Map<String, CFG> procToCFG) {
        this.procToCFG = procToCFG;
    }

    public String toDotString() {
        if (this.vertexNames == null) {
            allocateSerialNumbers();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("digraph {\n");
        for (Entry<String, CFG> entry : new TreeSet<Entry<String, CFG>>(procToCFG.entrySet())) {
            sb.append(toDot(entry.getKey(), entry.getValue()));
            sb.append('\n');
        }
        sb.append("}\n");
        return sb.toString();
    }

    private void allocateSerialNumbers() {
        int i = 0;
        this.vertexNames = new HashMap<CFG.Vertex, Integer>();
        for (CFG cfg : procToCFG.values()) {
            for (CFG.Vertex v : sort(cfg.getVertices())) {
                this.vertexNames.put(v, i);
                i++;
            }
        }
    }

    private String toDot(String name, CFG cfg) {
        StringBuilder sb = new StringBuilder();
        Set<CFG.Vertex> sortedVertices = sort(cfg.getVertices());

        // vertex attribute lines
        sb.append(name + "[shape=parallelogram];\n");
        for (CFG.Vertex v : sortedVertices) {
            String label = v.label().replace("\"", "\\\"").replace("\\n", "|n")
                    .replace("\n", "\\n").replace("\\0", "\\\\0");
            sb.append(getVertexName(v) + " [shape=box,label=\"" + label
                    + "\"];\n");
        }

        // edges
        if (cfg.entryVertex() != null) {
            sb.append(name + " -> " + getVertexName(cfg.entryVertex()) + ";\n");
            for (CFG.Vertex from : sortedVertices) {
                for (CFG.Vertex to : sort(cfg.getConnectedVerticesFrom(from))) {
                    sb.append(getVertexName(from) + " -> "
                            + getVertexName(to) + ";\n");
                }
            }
        }

        return sb.toString();
    }

    private String getVertexName(CFG.Vertex v) {
        Integer serial = this.vertexNames.get(v);
        return "v" + (serial == null ?
                getObjectAddress(v) : String.valueOf(serial));
    }

    private static String getObjectAddress(Object o) {
        String addr = o.toString();
        addr = addr.substring(addr.lastIndexOf('@') + 1);
        return addr;
    }

    private static class VertexComparator implements Comparator<CFG.Vertex> {
        @Override
        public int compare(Vertex o1, Vertex o2) {
            if (o1.getASTNodes().size() == 0) {
                return -1;
            } else if (o2.getASTNodes().size() == 0) {
                return 1;
            } else {
                IASTNode n1 = o1.getASTNodes().get(0);
                IASTNode n2 = o2.getASTNodes().get(0);
                return n1.getFileLocation().getNodeOffset() - n2.getFileLocation().getNodeOffset();
            }
        }
    }

    private static Set<CFG.Vertex> sort(Set<CFG.Vertex> vertices) {
        TreeSet<CFG.Vertex> sorted = new TreeSet<CFG.Vertex>(new VertexComparator());
        sorted.addAll(vertices);
        return sorted;
    }
}
