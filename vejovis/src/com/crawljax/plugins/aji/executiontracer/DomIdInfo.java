package com.crawljax.plugins.aji.executiontracer;

public class DomIdInfo implements Comparable<DomIdInfo> {
	private String idStr;
	private int parentHash;
	private String parentIdStr;
	private int stateId;
	private String parentXpath;
	private String tagName;
	
	private int type; //0 - previous state, 1 - current state, 2 - future state
	
	public DomIdInfo(String _idStr, int _parentHash, String _parentIdStr, String _parentXpath, String _tagName, int _stateId) {
		this.idStr = _idStr;
		this.parentHash = _parentHash;
		this.parentIdStr = _parentIdStr;
		this.stateId = _stateId;
		this.parentXpath = _parentXpath;
		this.tagName = _tagName;
		
		this.type = -1;
	}
	
	public String getIdStr() {
		return idStr;
	}
	
	public int getParentHash() {
		return parentHash;
	}
	
	public String getParentIdStr() {
		return parentIdStr;
	}
	
	public String getParentXpath() {
		return parentXpath;
	}
	
	public String getTagName() {
		return tagName;
	}
	
	public int getStateId() {
		return stateId;
	}
	
	public void setIdStr(String _idStr) {
		this.idStr = _idStr;
	}
	
	public void setParentHash(int _parentHash) {
		this.parentHash = _parentHash;
	}
	
	public void setParentIdStr(String _parentIdStr) {
		this.parentIdStr = _parentIdStr;
	}
	
	public void setParentXpath(String _parentXpath) {
		this.parentXpath = _parentXpath;
	}
	
	public void setTagName(String _tagName) {
		this.tagName = _tagName;
	}
	
	public void setType(int _type) {
		this.type = _type;
	}
	
	@Override
	public int compareTo(DomIdInfo dii) {
		if (this.idStr.compareTo(dii.idStr) < 0) {
			return -1;
		}
		else if (this.idStr.compareTo(dii.idStr) > 0) {
			return 1;
		}
		else {
			return 0;
		}
	}
}