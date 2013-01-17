package com.rca;

public class VariableDesc {
	private String var_name = "";
	private String var_kind = "";
	private String dec_type = "";
	private String rep_type = "";
	private boolean m_isGlobal = false;
	
	public VariableDesc(String name, String kind, String decType, String repType, boolean global) {
		this.setVarName(name);
		this.setVarKind(kind);
		this.setDecType(decType);
		this.setRepType(repType);
		this.m_isGlobal = global;
	}
	
	public void setVarName(String name) {
		this.var_name = name;
	}
	
	public void setVarKind(String kind) {
		this.var_kind = kind;
	}
	
	public void setDecType(String decType) {
		this.dec_type = decType;
	}
	
	public void setRepType(String repType) {
		this.rep_type = repType;
	}
	
	public String getVarName() {
		return this.var_name;
	}
	
	public String getVarKind() {
		return this.var_kind;
	}
	
	public String getDecType() {
		return this.dec_type;
	}
	
	public String getRepType() {
		return this.rep_type;
	}
	
	public boolean isGlobal() {
		return this.m_isGlobal;
	}
}