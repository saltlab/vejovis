package com.crawljax.plugins.aji.executiontracer;

import java.util.List;

public class InjectionInput {
	private int gebidLineNo;
	private String origSrcCodeGebid;
	private int mutatedLineNo;
	private String origSrcCodeMutated;
	private String modSrcCodeMutated;
	private String funcNameMutated;
	private List<String> keywords;
	
	private int type; //0 - regular, 1 - DOM node removal
	
	public InjectionInput(int _gebidLineNo, String _origSrcCodeGebid, int _mutatedLineNo, String _origSrcCodeMutated, String _modSrcCodeMutated, String _funcNameMutated, List<String> _keywords) {
		this.gebidLineNo = _gebidLineNo;
		this.origSrcCodeGebid = _origSrcCodeGebid;
		this.mutatedLineNo = _mutatedLineNo;
		this.origSrcCodeMutated = _origSrcCodeMutated;
		this.modSrcCodeMutated = _modSrcCodeMutated;
		this.funcNameMutated = _funcNameMutated;
		this.keywords = _keywords;
		
		this.type = 0;
	}
	
	public int getGebidLineNo() {
		return gebidLineNo;
	}
	
	public String getOrigSrcCodeGebid() {
		return origSrcCodeGebid;
	}
	
	public int getMutatedLineNo() {
		return mutatedLineNo;
	}
	
	public String getOrigSrcCodeMutated() {
		return origSrcCodeMutated;
	}
	
	public String getModSrcCodeMutated() {
		return modSrcCodeMutated;
	}
	
	public String getFuncNameMutated() {
		return funcNameMutated;
	}
	
	public void addKeyword(String kw) {
		keywords.add(kw);
	}
	
	public List<String> getKeywordList() {
		return keywords;
	}
	
	public int getType() {
		return type;
	}
	
	public void setType() {
		this.type = 1;
	}
}