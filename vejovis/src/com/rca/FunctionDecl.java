package com.rca;

import java.util.List;
import java.util.ArrayList;

public class FunctionDecl {
	//List of variables
	//Function name
	//Ppt declaration in dtrace file
	//Ppt type
	public List<VariableDesc> var_descs;
	public String f_name;
	public String ppt_decl;
	public String ppt_type;
	
	public FunctionDecl() {
		//Constructor
		this.var_descs = new ArrayList<VariableDesc>();
	}
	
	public void setFunctionName(String name) {
		this.f_name = name;
	}
	
	public void setPptDecl(String pptDecl) {
		this.ppt_decl = pptDecl;
	}
	
	public void setPptType(String pptType) {
		this.ppt_type = pptType;
	}
	
	public void addVariable(VariableDesc newVar) {
		var_descs.add(newVar);
	}
}