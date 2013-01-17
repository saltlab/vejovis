package com.crawljax.plugins.aji.executiontracer;

import java.util.*;

import org.apache.log4j.Logger;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.*;

public class AssnConcatExtractor implements NodeVisitor {
	//We'll also use this to extract concatenations from
	//return statements
	
	private String assnConcat;
	
	public AssnConcatExtractor() {
		assnConcat = "";
	}
	
	@Override
	public boolean visit(AstNode node) {
		if (node instanceof Assignment) {
			Assignment assn = (Assignment)node;
			
			assnConcat = assn.getRight().toSource();
			
			return false;
		}
		else if (node instanceof ReturnStatement) {
			ReturnStatement rs = (ReturnStatement)node;
			
			assnConcat = rs.getReturnValue().toSource();
			
			return false;
		}
		else if (node instanceof VariableInitializer) {
			VariableInitializer vi = (VariableInitializer)node;
			
			assnConcat = vi.getInitializer().toSource();
			
			return false;
		}
		return true;
	}
	
	public String getAssnConcat() {
		return assnConcat;
	}
}