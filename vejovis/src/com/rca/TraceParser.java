package com.rca;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class TraceParser {
	public List<FunctionDecl> function_decls;
	public List<FunctionTrace> function_traces;
	public String filename;
	
	public TraceParser(String file) {
		function_decls = new ArrayList<FunctionDecl>();
		function_traces = new ArrayList<FunctionTrace>();
		
		try {
			filename = file;
			FileInputStream fstream = new FileInputStream(filename);
			DataInputStream din = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(din));
			String strLine;
			
			//Set up the FunctionDecl and FunctionTrace lists
			while ((strLine = br.readLine()) != null) {
				if (strLine.startsWith("ppt")) {
					//Must be a FunctionDecl
					FunctionDecl fd = new FunctionDecl();
					fd.setPptDecl(strLine.substring(4));
					String function_name = strLine.substring(strLine.lastIndexOf('.')+1);
					fd.setFunctionName(function_name);
					//boolean settingUpVar = false;
					
					strLine = br.readLine();
					while (strLine.length() > 0) {
						if (strLine.startsWith("ppt-type")) {
							fd.setPptType(strLine.substring(9));
						}
						else if (strLine.startsWith("\tvariable")) {
							String name = strLine.substring(10);
							int colonIndex = name.indexOf(":");
							String var_scope = "local"; //default is local
							if (colonIndex >= 0) {
								if (colonIndex == name.length()-1) {
									System.err.println("Error: malformed variable declaration");
									throw new Exception();
									//System.exit(-1);
								}
								var_scope = name.substring(colonIndex + 1);
								name = name.substring(0,colonIndex);
							}
							
							//Is it local or global?
							//First, take out the [..] in case the variable is an array
							boolean isArray = false;
							if (var_scope.endsWith("[..]")) {
								int bracketIndex = var_scope.indexOf("[..]");
								var_scope = var_scope.substring(0,bracketIndex);
								isArray = true;
							}
							boolean isGlobal = false;
							if (var_scope.equals("local")) {
								isGlobal = false;
							}
							else if (var_scope.equals("global")) {
								isGlobal = true;
							}
							else {
								System.err.println("Error: malformed variable scope");
								throw new Exception();
								//System.exit(-1);
							}
							
							//Get var-kind
							strLine = br.readLine();
							String var_kind = strLine.substring(11);
							
							if (isArray) { //skip array and enclosing-var
								strLine = br.readLine();
								strLine = br.readLine();
							}
							
							//Get dec-type
							strLine = br.readLine();
							String dec_type = strLine.substring(11);
							
							//Get rep-type
							strLine = br.readLine();
							String rep_type = strLine.substring(11);
							
							VariableDesc var_desc = new VariableDesc(name, var_kind, dec_type, rep_type, isGlobal);
							
							//Do not add the variable if its name is RCA_errorMsg and this is not the error trace
							String traceType = function_name.substring(function_name.lastIndexOf(":::"));
							if (!(!traceType.equals(":::ERROR") && name.equals("RCA_errorMsg"))) {
								fd.addVariable(var_desc);
							}
						}
						
						strLine = br.readLine();
					}
					
					function_decls.add(fd);
				}
				else if (strLine.startsWith("decl-version")) {
					continue;
				}
				else if (strLine.length() > 0) {
					//Must be a FunctionTrace
					FunctionTrace ft;
					
					//Find corresponding FunctionDecl
					String pptDecl = strLine;
					Collections.sort(function_decls, new FunctionDeclComparator());
					FunctionDecl new_fd = new FunctionDecl();
					new_fd.ppt_decl = pptDecl;
					int index = Collections.binarySearch(function_decls, new_fd, new FunctionDeclComparator());
					//FunctionDecl fd_to_add = function_decls.get(index);
					
					//Get variable values
					List<String> var_vals = new ArrayList<String>();
					strLine = br.readLine();
					while (strLine.length() > 0) {
						//Value
						strLine = br.readLine();
						var_vals.add(strLine);
						
						//Type
						strLine = br.readLine();
						
						//Next Variable
						if ((strLine = br.readLine()) == null) {
							break;
						}
					}
					
					ft = new FunctionTrace(function_decls.get(index), var_vals);
					
					//Do not add anonymous functions
					if (ft.f_decl.f_name.startsWith("anonymous")) {
						continue;
					}
					function_traces.add(ft);
				}
			}
		} catch(Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
}