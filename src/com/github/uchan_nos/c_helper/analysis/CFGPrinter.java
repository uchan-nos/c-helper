package com.github.uchan_nos.c_helper.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.github.uchan_nos.c_helper.util.Util;

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
        //for (Entry<String, CFG> entry : new TreeSet<Entry<String, CFG>>(procToCFG.entrySet())) {
        for (Entry<String, CFG> entry : procToCFG.entrySet()) {
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
            for (CFG.Vertex v : Util.sort(cfg.getVertices())) {
                this.vertexNames.put(v, i);
                i++;
            }
        }
    }

    private String toDot(String name, CFG cfg) {
        StringBuilder sb = new StringBuilder();
        Set<CFG.Vertex> sortedVertices = Util.sort(cfg.getVertices());

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
                for (CFG.Vertex to : Util.sort(cfg.getConnectedVerticesFrom(from))) {
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

}
