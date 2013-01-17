package com.rca;

import java.util.*;

public class FunctionDeclComparator implements Comparator<FunctionDecl> {
	@Override
	public int compare(FunctionDecl f1, FunctionDecl f2) {
		return f1.ppt_decl.compareTo(f2.ppt_decl);
	}
}