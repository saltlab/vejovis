package com.crawljax.plugins.aji.executiontracer;

public class DirectDOMAccess {
	private int lineNo; //line number relative to the function
	private String srcLine;
	private String funcName;
	
	public DirectDOMAccess(int _lineNo, String _srcLine, String _funcName) {
		this.lineNo = _lineNo;
		this.srcLine = _srcLine;
		this.funcName = _funcName;
	}
	
	public int getLineNo() {
		return lineNo;
	}
	
	public String getSrcLine() {
		return srcLine;
	}
	
	public String funcName() {
		return funcName;
	}
}