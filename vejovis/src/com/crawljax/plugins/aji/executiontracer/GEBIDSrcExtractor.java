package com.crawljax.plugins.aji.executiontracer;

import java.util.*;

import org.apache.log4j.Logger;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.*;

public class GEBIDSrcExtractor implements NodeVisitor {
	private String gebidSrc;
	private String gebidSrcConcat;
	
	public GEBIDSrcExtractor() {
		gebidSrc = null;
		gebidSrcConcat = null;
	}
	
	@Override
	public boolean visit(AstNode node) {
		if (node instanceof FunctionCall) {
			FunctionCall fc = (FunctionCall)node;
			
			String funcToCall = fc.getTarget().toSource();
			
			//if (fc.toSource().startsWith("document.getElementById(") || fc.toSource().startsWith("$(")) {
			if (funcToCall.endsWith("document.getElementById") || funcToCall.endsWith("$")) {
				gebidSrc = fc.toSource();
				
				//Get the parameter
				gebidSrcConcat = fc.getArguments().get(0).toSource(); //Assumption is that there is only one argument
				
				return false;
			}
		}
		return true;
	}
	
	public String getGebidSrc() {
		return gebidSrc;
	}
	
	public String getGebidSrcConcat() {
		return gebidSrcConcat;
	}
}