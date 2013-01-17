package com.rca;

import java.util.List;
//import java.util.ArrayList;

public class FunctionTrace {
	public List<String> var_values;
	public FunctionDecl f_decl;
	
	public FunctionTrace(FunctionDecl fd, List<String> ls) {
		//TODO: Ensure that ls has the same number of elements as the number of variables in fd
		var_values = ls;
		f_decl = fd;
	}
}