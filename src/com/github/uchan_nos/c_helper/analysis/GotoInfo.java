package com.github.uchan_nos.c_helper.analysis;

public class GotoInfo {
	private CFG.Vertex from;
	private String toName;
	
	public GotoInfo(CFG.Vertex from, String toName) {
		this.from = from;
		this.toName = toName;
	}
	
	public CFG.Vertex from() {
		return this.from;
	}
	
	public String toName() {
		return this.toName;
	}
}
