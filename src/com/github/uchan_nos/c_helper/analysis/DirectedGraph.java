package com.github.uchan_nos.c_helper.analysis;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;


public class DirectedGraph<Vertex> implements IGraph<Vertex> {
    private Map<Vertex, Set<Vertex>> connectedToMap;

    public DirectedGraph() {
        this.connectedToMap = new HashMap<Vertex, Set<Vertex>>();
    }

    @Override
    public void add(Vertex v) {
        if (!connectedToMap.containsKey(v)) {
            connectedToMap.put(v, new HashSet<Vertex>());
        }
    }

    @Override
    public void add(Collection<Vertex> vs) {
        for (Vertex v : vs) {
            add(v);
        }
    }

    @Override
    public void remove(Vertex v) {
        if (!contains(v)) {
            throw new NoSuchElementException("remove() requires that 'v' is in this graph");
        }
        connectedToMap.remove(v);
        for (Entry<Vertex, Set<Vertex>> entry : connectedToMap.entrySet()) {
            entry.getValue().remove(v);
        }
    }

    @Override
    public void connect(Vertex from, Vertex to) {
        if (!contains(from) || !contains(to)) {
            throw new NoSuchElementException("connect() requires that 'from' and 'to' are in this graph");
        }
        connectedToMap.get(from).add(to);
    }

    @Override
    public void disconnect(Vertex from, Vertex to) {
        if (!contains(from) || !contains(to)) {
            throw new NoSuchElementException("disconnect() requires that 'from' and 'to' are in this graph");
        }
        connectedToMap.get(from).remove(to);
    }

    @Override
    public boolean contains(Vertex v) {
        return connectedToMap.containsKey(v);
    }

    @Override
    public boolean isConnected(Vertex from, Vertex to) {
        Set<Vertex> vertices = connectedToMap.get(from);
        return vertices != null && vertices.contains(to);
    }

    @Override
    public Set<Vertex> getConnectedVerticesFrom(Vertex v) {
        return connectedToMap.get(v);
    }

    @Override
    public Set<Vertex> getConnectedVerticesTo(Vertex v) {
        Set<Vertex> vertices = new HashSet<Vertex>();
        for (Entry<Vertex, Set<Vertex>> entry : connectedToMap.entrySet()) {
            if (entry.getValue().contains(v)) {
                vertices.add(entry.getKey());
            }
        }
        return vertices;
    }

    @Override
    public Set<Vertex> getVertices() {
        return connectedToMap.keySet();
    }
}
