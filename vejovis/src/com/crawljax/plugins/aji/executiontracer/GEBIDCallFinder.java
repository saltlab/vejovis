/**
 * The purpose of this class is to traverse an AST rooted at some
 * node to find the getElementById call (if it exists). If the
 * GEBID call exists, the source code of the parameter passed to
 * GEBID is saved
 */
package com.crawljax.plugins.aji.executiontracer;

import java.util.*;

import org.apache.log4j.Logger;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.*;

public class GEBIDCallFinder implements NodeVisitor {
	private boolean gebidCallFound;
	private String gebidParam;
	
	public GEBIDCallFinder() {
		gebidCallFound = false;
		gebidParam = null;
	}
	
	@Override
	public boolean visit(AstNode node) {
		if (node instanceof FunctionCall) {
			FunctionCall fc = (FunctionCall)node;
			String fcSource = fc.toSource();
			if (fcSource.startsWith("document.getElementById(")) {
				//Get the parameter (parameter starts at index 24)
				int lpCounter = 1; //left parentheses counter
				int rpCounter = 0; //right parentheses counter
				int lastRpCounterIndex = fcSource.length()-1;
				for (int i = 24; i < fcSource.length(); i++) {
					if (fcSource.substring(i, i+1).equals("(")) {
						lpCounter++;
					}
					else if (fcSource.substring(i, i+1).equals(")")) {
						rpCounter++;
						if (rpCounter == lpCounter) {
							lastRpCounterIndex = i;
							break;
						}
					}
				}
				
				gebidParam = fcSource.substring(24, lastRpCounterIndex);
				gebidCallFound = true;
			}
			else if (fcSource.startsWith("$(")) {
				//Get the parameter (parameter starts at index 2)
				int lpCounter = 1; //left parentheses counter
				int rpCounter = 0; //right parentheses counter
				int lastRpCounterIndex = fcSource.length()-1;
				for (int i = 2; i < fcSource.length(); i++) {
					if (fcSource.substring(i, i+1).equals("(")) {
						lpCounter++;
					}
					else if (fcSource.substring(i, i+1).equals(")")) {
						rpCounter++;
						if (rpCounter == lpCounter) {
							lastRpCounterIndex = i;
							break;
						}
					}
				}
				
				gebidParam = fcSource.substring(2, lastRpCounterIndex);
				gebidCallFound = true;
			}
		}
		return !gebidCallFound;
	}
	
	public boolean gebidCallIsFound() {
		return gebidCallFound;
	}
	
	public String getGebidParam() {
		return gebidParam;
	}
}