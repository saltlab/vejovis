/*
    Automatic JavaScript Invariants is a plugin for Crawljax that can be
    used to derive JavaScript invariants automatically and use them for
    regressions testing.
    Copyright (C) 2010  crawljax.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
package com.crawljax.plugins.aji;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.*;

import com.crawljax.core.CrawljaxController;
import com.crawljax.plugins.aji.executiontracer.GEBIDCallFinder;
import com.crawljax.plugins.aji.executiontracer.ProgramPoint;

/**
 * Abstract class that is used to define the interface and some functionality for the NodeVisitors
 * that modify JavaScript.
 * 
 * @author Frank Groeneveld
 * @version $Id: JSASTModifier.java 6161 2009-12-16 13:47:15Z frank $
 */
public abstract class JSASTModifier implements NodeVisitor {

	private final Map<String, String> mapper = new HashMap<String, String>();

	protected static final Logger LOGGER = Logger.getLogger(CrawljaxController.class.getName());
	
	protected boolean instrumentAsyncs = true;
	
	private List<String> excludeFunctions = new ArrayList<String>(); //List of functions (in regexp form) to exclude from instrumentation
	
	//Variables used for inject mode
	private boolean injectMode = false; //can be set to true by calling setInject()
	private int injectLineNo = 0;
	private String injectOrigSrc = "";
	private String injectModSrc = "";
	private String injectFuncName = "";
	private int injectType = 0; //0 - regular, 1 - DOM node removal

	/**
	 * This is used by the JavaScript node creation functions that follow.
	 */
	private CompilerEnvirons compilerEnvirons = new CompilerEnvirons();

	/**
	 * Contains the scopename of the AST we are visiting. Generally this will be the filename
	 */
	private String scopeName = null;

	/**
	 * @param scopeName
	 *            the scopeName to set
	 */
	public void setScopeName(String scopeName) {
		this.scopeName = scopeName;
	}

	/**
	 * @return the scopeName
	 */
	public String getScopeName() {
		return scopeName;
	}

	/**
	 * Abstract constructor to initialize the mapper variable.
	 */
	protected JSASTModifier() {
		/* add -<number of arguments> to also make sure number of arguments is the same */
		mapper.put("addClass", "attr('class')");
		mapper.put("removeClass", "attr('class')");

		mapper.put("css-2", "css(%0)");
		mapper.put("attr-2", "attr(%0)");
		mapper.put("append", "html()");
	}
	
	/**
	 * Allow asynchronous transitions to be traced
	 * 
	 * @param val 
	 *			true (allow) or false (disallow)
	 */
	public void setInstrumentAsyncs(boolean val) {
		instrumentAsyncs = val;
	}
	
	public void setExcludeFunctionsList(List<String> _excludeFuncs) {
		this.excludeFunctions = _excludeFuncs;
	}

	/**
	 * Parse some JavaScript to a simple AST.
	 * 
	 * @param code
	 *            The JavaScript source code to parse.
	 * @return The AST node.
	 */
	public AstNode parse(String code) {
		Parser p = new Parser(compilerEnvirons, null);
		//compilerEnvirons.setErrorReporter(new ConsoleErrorReporter());
		//Parser p = new Parser(compilerEnvirons, new ConsoleErrorReporter());
		AstNode nodeToReturn = null;
		try {
			nodeToReturn = p.parse(code, null, 0);
		}
		catch (NullPointerException npe) {
			System.err.println("Error with parsing");
		}
		return nodeToReturn;
		//return p.parse(code, null, 0);
	}

	/**
	 * Find out the function name of a certain node and return "anonymous" if it's an anonymous
	 * function.
	 * 
	 * @param f
	 *            The function node.
	 * @return The function name.
	 */
	protected String getFunctionName(FunctionNode f) {
		Name functionName = f.getFunctionName();

		if (functionName == null) {
			return "anonymous" + f.getLineno();
		} else {
			return functionName.toSource();
		}
	}

	/**
	 * Creates a node that can be inserted at a certain point in function.
	 * 
	 * @param function
	 *            The function that will enclose the node.
	 * @param postfix
	 *            The postfix function name (enter/exit).
	 * @param lineNo
	 *            Linenumber where the node will be inserted.
	 * @return The new node.
	 */
	protected abstract AstNode createNode(FunctionNode function, String postfix, int lineNo);
	
	/**
	 * Creates a node that can be inserted at a certain point in the AST root.
	 * @param root
	 * 			The AST root that will enclose the node.
	 * @param postfix
	 * 			The postfix name.
	 * @param lineNo
	 * 			Linenumber where the node will be inserted.
	 * @param rootCount
	 * 			Unique integer that identifies the AstRoot
	 * @return The new node
	 */
	protected abstract AstNode createNode(AstRoot root, String postfix, int lineNo, int rootCount);

	/**
	 * Creates a node that can be inserted before and after a DOM modification statement (such as
	 * jQuery('#test').addClass('bla');).
	 * 
	 * @param shouldLog
	 *            The variable that should be logged (for example jQuery('#test').attr('style'))
	 * @param lineNo
	 *            The line number where this will be inserted.
	 * @return The new node.
	 */
	protected abstract AstNode createPointNode(String shouldLog, int lineNo);

	/**
	 * Create a new block node with two children.
	 * 
	 * @param node
	 *            The child.
	 * @return The new block.
	 */
	private Block createBlockWithNode(AstNode node) {
		Block b = new Block();

		b.addChild(node);

		return b;
	}

	/**
	 * @param node
	 *            The node we want to have wrapped.
	 * @return The (new) node parent (the block probably)
	 */
	private AstNode makeSureBlockExistsAround(AstNode node) {
		AstNode parent = node.getParent();

		if (parent instanceof IfStatement) {
			/* the parent is an if and there are no braces, so we should make a new block */
			IfStatement i = (IfStatement) parent;

			/* replace the if or the then, depending on what the current node is */
			if (i.getThenPart().equals(node)) {
				i.setThenPart(createBlockWithNode(node));
			} else {
				i.setElsePart(createBlockWithNode(node));
			}
		} else if (parent instanceof WhileLoop) {
			/* the parent is a while and there are no braces, so we should make a new block */
			/* I don't think you can find this in the real world, but just to be sure */
			WhileLoop w = (WhileLoop) parent;
			w.setBody(createBlockWithNode(node));
		} else if (parent instanceof ForLoop) {
			/* the parent is a for and there are no braces, so we should make a new block */
			/* I don't think you can find this in the real world, but just to be sure */
			ForLoop f = (ForLoop) parent;
			f.setBody(createBlockWithNode(node));
		}

		/**
		 * TODO: Frank, find a way to do this without concurrent modification exceptions.
		 */
		// s.setStatements(statements);
		// break;
		// }
		// }

		// }
		return node.getParent();
	}
	
	private boolean shouldModifyFunction(String name) {
		/* try all patterns and if 1 matches, return false */
		for (String pattern : excludeFunctions) {
			if (name.matches(pattern)) {
				return false;
			}
		}

		return true;
	}
	
	private boolean dontModifyRootAndAnon() {
		/* try all patterns and if 1 matches, return false */
		for (String pattern : excludeFunctions) {
			if (pattern.equals("dontModifyRootAndAnon")) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Actual visiting method.
	 * 
	 * @param node
	 *            The node that is currently visited.
	 * @return Whether to visit the children.
	 */
	@Override
	public boolean visit(AstNode node) {
		FunctionNode func;
		
		if (!((node instanceof FunctionNode || node instanceof ReturnStatement || node instanceof SwitchCase || node instanceof AstRoot || node instanceof ExpressionStatement || node instanceof BreakStatement || node instanceof ContinueStatement || node instanceof ThrowStatement || node instanceof VariableDeclaration || node instanceof FunctionCall))) {// || node instanceof ExpressionStatement || node instanceof BreakStatement || node instanceof ContinueStatement || node instanceof ThrowStatement || node instanceof VariableDeclaration || node instanceof ReturnStatement || node instanceof SwitchCase)) {
			return true;
		}
		
		/*Get enclosing function if not null. Return if function in exclude list*/
		FunctionNode encFunc = node.getEnclosingFunction();
		if (encFunc != null) {
			String funcName = encFunc.getName();
			if (!shouldModifyFunction(funcName)) {
				return true;
			}
		}

		if (node instanceof FunctionNode) {
			func = (FunctionNode) node;

			/* this is function enter */
			AstNode newNode = createNode(func, ProgramPoint.ENTERPOSTFIX, func.getLineno());

			func.getBody().addChildToFront(newNode);
			
			node = (AstNode) func.getBody().getFirstChild();
			node = (AstNode) node.getNext(); //The first node is the node just added in front, so get next node
			int firstLine = 0;
			if (node != null) {
				firstLine = node.getLineno();
			}

			/* get last line of the function */
			node = (AstNode) func.getBody().getLastChild();
			/* if this is not a return statement, we need to add logging here also */
			if (!(node instanceof ReturnStatement)) {
				AstNode newNode_end = createNode(func, ProgramPoint.EXITPOSTFIX, node.getLineno()-firstLine+1);
				/* add as last statement */
				func.getBody().addChildToBack(newNode_end);
			}
			
			/* enclose function with try...catch */
			Block b = new Block();
			AstNode curr_node = (AstNode) func.getBody().getFirstChild();
			while (curr_node != null) {
				AstNode peeked_node = (AstNode) curr_node.getNext(); //curr_node's parent will change, so getNext() will also change. Thus, peek the next node first
				b.addStatement(curr_node);
				curr_node = peeked_node;
			}
			TryStatement ts = new TryStatement();
			ts.setTryBlock(b);
			
			CatchClause cc = new CatchClause();
			Name catchVar = new Name(0,"err");
			cc.setVarName(catchVar);
			Block catch_b = new Block();
			AstNode newNode_try = createNode(func,":::ERROR",func.getLineno());
			Name errorObj = new Name(0,"Error");
			StringLiteral errStr = new StringLiteral();
			errStr.setValue("Root Cause Analyzer: Error detected");
			errStr.setQuoteCharacter('\"');
			NewExpression newExpr = new NewExpression();
			newExpr.setTarget(errorObj);
			newExpr.addArgument(errStr);
			ThrowStatement throw_st = new ThrowStatement();
			throw_st.setExpression(newExpr);
			catch_b.addStatement(newNode_try);
			catch_b.addStatement(throw_st);
			cc.setBody(catch_b);
			ts.addCatchClause(cc);
			
			Block func_block = new Block();
			func_block.addStatement(ts);
			func.setBody(func_block);
			
			if (!instrumentAsyncs)
				return true;
			
			/*add RCA_timerID parameter*/
			Name rca_timer_id = new Name(0,"RCA_timerID");
			func.addParam(rca_timer_id);
			
			/*add an if statement prior to the catch block in case this is async call*/
			String async_if_code = "if (typeof RCA_timerID != 'undefined') { }";
			AstNode async_if_tmp = parse(async_if_code);
			async_if_tmp = (AstNode)async_if_tmp.getFirstChild();
			if (!(async_if_tmp instanceof IfStatement)) {
				System.err.println("Error instrumenting function");
				return false;
				//System.exit(-1);
			}
			else {
				//Add "then" part of if statement
				//The marker's line
				IfStatement async_if = (IfStatement)async_if_tmp;
				AstNode async_marker = createNode(func,":::ASYNC",func.getLineno());
				async_if.setThenPart(async_marker);
				makeSureBlockExistsAround(async_marker);
				func.getBody().addChildToFront(async_if);
			}
		}
		else if (node instanceof AstRoot) {
			AstRoot rt = (AstRoot) node;
			
			//if (dontModifyRootAndAnon()) {
				//return true;
			//}
			
			if (rt.getSourceName() == null) { //make sure this is an actual AstRoot, not one we created
				return true;
			}
			
			//this is the entry point of the AST root
			m_rootCount++;
			AstNode newNode = createNode(rt, ProgramPoint.ENTERPOSTFIX, rt.getLineno(), m_rootCount);
			
			rt.addChildToFront(newNode);
			
			node = (AstNode) rt.getFirstChild();
			node = (AstNode) node.getNext(); //The first node is the node just added in front, so get next node
			int firstLine = 0;
			if (node != null) {
				firstLine = node.getLineno();
			}
			
			// get last line of the function
			node = (AstNode) rt.getLastChild();
			//if this is not a return statement, we need to add logging here also
			if (!(node instanceof ReturnStatement)) {
				AstNode newNode_end = createNode(rt, ProgramPoint.EXITPOSTFIX, node.getLineno()-firstLine+1, m_rootCount);
				//add as last statement
				rt.addChildToBack(newNode_end);
			}
			
			//enclose with try...catch
			Block b = new Block();
			AstNode curr_node = (AstNode) rt.getFirstChild();
			while (curr_node != null) {
				AstNode peeked_node = (AstNode) curr_node.getNext(); //curr_node's parent will change, so getNext() will also change. Thus, peek the next node first
				b.addStatement(curr_node);
				curr_node = peeked_node;
			}
			TryStatement ts = new TryStatement();
			ts.setTryBlock(b);
			
			CatchClause cc = new CatchClause();
			Name catchVar = new Name(0,"err");
			cc.setVarName(catchVar);
			Block catch_b = new Block();
			AstNode newNode_try = createNode(rt,":::ERROR",rt.getLineno(),m_rootCount);
			Name errorObj = new Name(0,"Error");
			StringLiteral errStr = new StringLiteral();
			errStr.setValue("Root Cause Analyzer: Error detected");
			errStr.setQuoteCharacter('\"');
			NewExpression newExpr = new NewExpression();
			newExpr.setTarget(errorObj);
			newExpr.addArgument(errStr);
			ThrowStatement throw_st = new ThrowStatement();
			throw_st.setExpression(newExpr);
			catch_b.addStatement(newNode_try);
			catch_b.addStatement(throw_st);
			cc.setBody(catch_b);
			ts.addCatchClause(cc);
			
			Block func_block = new Block();
			func_block.addStatement(ts);
			rt.removeChildren();
			rt.addChild(func_block);
		}
		else if (node instanceof ExpressionStatement || node instanceof BreakStatement || node instanceof ContinueStatement || node instanceof ThrowStatement || node instanceof VariableDeclaration) {
			AstNode instrumentedNode = null;
			if (node instanceof VariableDeclaration) {
				//Make sure this variable declaration is not part of a for loop
				if (node.getParent() instanceof ForLoop) {
					return true;
				}
			}
			/* check if node is the ThrowStatement added in the instrumentation */
			if (node instanceof ThrowStatement) {
				ThrowStatement throw_s = (ThrowStatement) node;
				if (throw_s.getExpression() instanceof NewExpression) {
					NewExpression n_ex = (NewExpression) throw_s.getExpression();
					if (n_ex.getTarget() instanceof Name) {
						Name target = (Name)n_ex.getTarget();
						if (target.getIdentifier().equals("Error")) {
							List<AstNode> list_ast = n_ex.getArguments();
							if (list_ast.iterator().hasNext()) {
								AstNode strLit_node = list_ast.iterator().next();
								if (strLit_node instanceof StringLiteral) {
									StringLiteral strLit = (StringLiteral)strLit_node;
									if (strLit.getValue().equals("Root Cause Analyzer: Error detected")) {
										return true;
									}
								}
							}
							
						}
					}
				}
			}
			
			//Make sure additional try statement is not instrumented
			if (node instanceof TryStatement) {
				return true; //no need to add instrumentation before try statement anyway since we only instrument what's inside the blocks
			}
			
			//FROLIN - Instrument GEBID Calls (place here?)
			String nodeSourceCode = node.toSource();
			if (!nodeSourceCode.contains("function(")) {
				//Assume it's part of an anonymous function definition, so don't 
				//instrument if there's a space
				instrumentGEBIDCalls(node);
			}
			
			func = node.getEnclosingFunction();
			//String s = node.getParent().toSource();
			
			int firstLine = 0;
			if (func != null) {
				AstNode firstLine_node = (AstNode) func.getBody().getFirstChild();
				if (func instanceof FunctionNode && firstLine_node instanceof IfStatement) { //Perform extra check due to addition if statement
					firstLine_node = (AstNode) firstLine_node.getNext();
				}
				if (func instanceof FunctionNode && firstLine_node instanceof TryStatement) {
					TryStatement firstLine_node_try = (TryStatement) firstLine_node;
					firstLine_node = (AstNode) firstLine_node_try.getTryBlock().getFirstChild();
				}
				firstLine_node = (AstNode) firstLine_node.getNext();
				firstLine = 0;
				if (firstLine_node != null) {
					//If first child is an ExpressionStatement or VariableDeclaration, then there might be multiple instances of the instrumented node at the beginning of the FunctionNode's list of children
					while (firstLine_node != null) {
						firstLine = firstLine_node.getLineno();
						if (firstLine > 0) {
							break;
						}
						else {
							firstLine_node = (AstNode) firstLine_node.getNext();
						}
					}
				}
				
				/*GET FIRST LINE FROM line number of function definition itself*/
				firstLine = func.getBody().getLineno() + 1;
				/*END GET FIRST LINE FROM line number of function definition itself*/
				
				if (node.getLineno() >= firstLine) {
					AstNode newNode = createNode(func, ":::INTERMEDIATE", node.getLineno()-firstLine+1);
					instrumentedNode = newNode;
					//AstNode parent = node.getParent();
					
					AstNode parent = makeSureBlockExistsAround(node);
					
					//parent.addChildAfter(newNode, node);
					try {
						parent.addChildBefore(newNode, node);
					}
					catch (NullPointerException npe) {
						System.err.println(npe.getMessage());
					}
				}
			}
			else { //The expression must be outside a function
				AstRoot rt = node.getAstRoot();
				if (rt == null || rt.getSourceName() == null) {
					return true;
				}
				AstNode firstLine_node = (AstNode) rt.getFirstChild();
				if (firstLine_node instanceof Block) {
					firstLine_node = (AstNode)firstLine_node.getFirstChild(); //Try statement
				}
				if (firstLine_node instanceof TryStatement) {
					TryStatement firstLine_node_try = (TryStatement) firstLine_node;
					firstLine_node = (AstNode) firstLine_node_try.getTryBlock().getFirstChild();
				}
				firstLine_node = (AstNode) firstLine_node.getNext();
				firstLine = 0;
				if (firstLine_node != null) {
					//If first child is an ExpressionStatement or VariableDeclaration, then there might be multiple instances of the instrumented node at the beginning of the FunctionNode's list of children
					while (firstLine_node != null) {
						firstLine = firstLine_node.getLineno();
						if (firstLine > 0) {
							break;
						}
						else {
							firstLine_node = (AstNode) firstLine_node.getNext();
						}
					}
				}
				
				/*GET FIRST LINE FROM line number of function definition itself*/
				firstLine = rt.getLineno() + 1;
				/*END GET FIRST LINE FROM line number of function definition itself*/
				
				if (node.getLineno() >= firstLine) {
					AstNode newNode = createNode(rt, ":::INTERMEDIATE", node.getLineno()-firstLine+1, m_rootCount);
					instrumentedNode = newNode;
					//AstNode parent = node.getParent();
					
					AstNode parent = makeSureBlockExistsAround(node);
					
					//parent.addChildAfter(newNode, node);
					try {
						parent.addChildBefore(newNode, node);
						/*DELETE WHEN DONE DEBUGGING*/
						/*
						if (node.toSource().contains("var promo_changer = setTimeout(change_promo, rotate_delay);")) {
						String alertNodeStr = "sendReally();\nvar m = setTimeout(\"change_promo()\", 0)";
						AstNode alertNode = parse(alertNodeStr);
						parent.addChildBefore(alertNode, node);
						}
						*/
						/*END DELETE WHEN DONE DEBUGGING*/
					}
					catch (NullPointerException npe) {
						System.err.println(npe.getMessage());
					}
				}
			}
			
			/*START INJECT MODE*/
			if (injectMode) {
				//Check if this is the node to replace
				boolean sameSrc = node.toSource().trim().equals(injectOrigSrc.trim());
				boolean sameLineNo = (node.getLineno()-firstLine+1 == injectLineNo);
				boolean sameFunction = false;
				if (func != null) { //No AstRoot injections done here
					Name funcName = func.getFunctionName();
					if (funcName != null) {
						sameFunction = funcName.toSource().equals(injectFuncName);
					}
				}
				else { //Must be in AstRoot
					sameFunction = injectFuncName.equals("root" + m_rootCount);
				}
				
				if (sameSrc && sameLineNo && sameFunction) {
					//Create a new node consisting of the modified line and replace
					//this node with that
					AstNode newNode = (AstRoot)(parse(injectModSrc));
					AstNode parent = makeSureBlockExistsAround(node);
					try {
						parent.addChildBefore(newNode, node);
						parent.removeChild(node);
						newNode.setParent(parent);
						
						//Add the new dummy GEBID parameter line
						if (instrumentedNode != null) {
							if (!nodeSourceCode.contains("function(")) {
								//Assume it's part of an anonymous function definition, so don't 
								//instrument if there's a space
								reInstrumentGEBIDCalls(newNode, instrumentedNode);
							}
						}
					}
					catch (NullPointerException npe) {
						System.err.println(npe.getMessage());
					}
				}
			}
			/*END INJECT MODE*/
		}
		else if (node instanceof ReturnStatement) {
			AstNode instrumentedNode = null;
			func = node.getEnclosingFunction();
			AstNode firstLine_node = (AstNode) func.getBody().getFirstChild();
			if (func instanceof FunctionNode && firstLine_node instanceof IfStatement) { //Perform extra check due to addition if statement
				firstLine_node = (AstNode) firstLine_node.getNext();
			}
			if (func instanceof FunctionNode && firstLine_node instanceof TryStatement) {
				TryStatement firstLine_node_try = (TryStatement) firstLine_node;
				firstLine_node = (AstNode) firstLine_node_try.getTryBlock().getFirstChild();
			}
			firstLine_node = (AstNode) firstLine_node.getNext();
			int firstLine = 0;
			if (firstLine_node != null) {
				//If first child is an ExpressionStatement or VariableDeclaration, then there might be multiple instances of the instrumented node at the beginning of the FunctionNode's list of children
				while (firstLine_node != null) {
					firstLine = firstLine_node.getLineno();
					if (firstLine > 0) {
						break;
					}
					else {
						firstLine_node = (AstNode) firstLine_node.getNext();
					}
				}
			}
			
			/*GET FIRST LINE FROM line number of function definition itself*/
			firstLine = func.getBody().getLineno() + 1;
			/*END GET FIRST LINE FROM line number of function definition itself*/
			
			//FROLIN - Instrument GEBID Calls (place here?)
			String nodeSourceCode = node.toSource();
			if (!nodeSourceCode.contains("function(")) {
				//Assume it's part of an anonymous function definition, so don't 
				//instrument if there's a space
				instrumentGEBIDCalls(node);
			}
			
			AstNode parent = makeSureBlockExistsAround(node);
			
			/*PARTITION NODE - FROLIN*/
			//Create Name node
			ReturnStatement r_node = (ReturnStatement)node;
			//if (r_node.getReturnValue() instanceof FunctionCall) {
			Name name_node = new Name(0,"invarscope_reserved_var_name");
			AstNode right_node = r_node.getReturnValue();
			if (right_node != null) {
				Assignment assn_node = new Assignment(90,name_node,right_node,0);
				ExpressionStatement expr_node = new ExpressionStatement(assn_node,true);
			
				parent.addChildBefore(expr_node, node);
			
				//Remove change return value
				r_node.setReturnValue(name_node);
			}
			/*END PARTITION NODE*/

			AstNode newNode = createNode(func, ProgramPoint.EXITPOSTFIX, node.getLineno()-firstLine+1);
			instrumentedNode = newNode;

			/* the parent is something we can prepend to */
			parent.addChildBefore(newNode, node);

			/*START INJECT MODE*/
			if (injectMode) {
				//Check if this is the node to replace
				boolean sameSrc = node.toSource().trim().equals(injectOrigSrc.trim());
				boolean sameLineNo = (node.getLineno()-firstLine+1 == injectLineNo);
				boolean sameFunction = false;
				if (func != null) { //No AstRoot injections done here
					Name funcName = func.getFunctionName();
					if (funcName != null) {
						sameFunction = funcName.toSource().equals(injectFuncName);
					}
				}
				else { //Must be in AstRoot
					sameFunction = injectFuncName.equals("root" + m_rootCount);
				}
				
				if (sameSrc && sameLineNo && sameFunction) {
					//Create a new node consisting of the modified line and replace
					//this node with that
					newNode = (AstRoot)(parse(injectModSrc));
					parent = makeSureBlockExistsAround(node);
					try {
						parent.addChildBefore(newNode, node);
						parent.removeChild(node);
						newNode.setParent(parent);
						
						//Add the new dummy GEBID parameter line
						if (instrumentedNode != null) {
							if (!nodeSourceCode.contains("function(")) {
								//Assume it's part of an anonymous function definition, so don't 
								//instrument if there's a space
								reInstrumentGEBIDCalls(newNode, instrumentedNode);
							}
						}
					}
					catch (NullPointerException npe) {
						System.err.println(npe.getMessage());
					}
				}
			}
			/*END INJECT MODE*/
		}
		else if (node instanceof FunctionCall) {
			FunctionCall fc = (FunctionCall)node;
			
			//Get arguments
			List<AstNode> args = fc.getArguments();
			String targetName = fc.getTarget().toSource();
			
			if (targetName.equals("setTimeout")) {
				//Check if first arg is a call to a function without
				//arguments (assume single variable without quotation marks
				//is a function call)
				AstNode firstArg = args.get(0);
				if (firstArg instanceof Name) {
					//Replace first argument with a function call with quotation marks
					Name firstArgName = (Name)firstArg;
					String funcCallName = firstArgName.getIdentifier();
					String newFuncCall = funcCallName + "()";
					StringLiteral sl = new StringLiteral();
					sl.setQuoteCharacter('\"');
					sl.setValue(newFuncCall);
					args.set(0,sl);
				}
			}
		}
		else if (node instanceof SwitchCase) {
			//Add block around all statements in the switch case
			SwitchCase sc = (SwitchCase)node;
			List<AstNode> statements = sc.getStatements();
			List<AstNode> blockStatement = new ArrayList<AstNode>();
			Block b = new Block();
			
			if (statements != null) {
				Iterator<AstNode> it = statements.iterator();
				while (it.hasNext()) {
					AstNode stmnt = it.next();
					b.addChild(stmnt);
				}
				
				blockStatement.add(b);
				sc.setStatements(blockStatement);
			}
		}
		/* have a look at the children of this node */
		return true;
	}

	private AstNode getLineNode(AstNode node) {
		while ((!(node instanceof ExpressionStatement) && !(node instanceof Assignment))
		        || node.getParent() instanceof ReturnStatement) {
			node = node.getParent();
		}
		return node;
	}
	
	/**
	 * If node contains a GEBID call, include an assignment to
	 * a dummy variable
	 * 
	 * @param node Must be an ExpressionStatement, VariableDeclaration, or any
	 * node that could represent a line in the JS code
	 */
	private void instrumentGEBIDCalls(AstNode node) {
		GEBIDCallFinder gcf = new GEBIDCallFinder();
		node.visit(gcf);
		
		if (gcf.gebidCallIsFound() && gcf.getGebidParam() != null) {
			String param = gcf.getGebidParam();
			String dummyLine = "vejovisDummyVar = " + param + ";\n";
			
			//Insert the dummy line before the current node's line
			AstNode parent = makeSureBlockExistsAround(node);
			AstNode newNode = parse(dummyLine);
			parent.addChildBefore(newNode, node);
		}
	}
	
	/**
	 * Instrument injected GEBID calls
	 * @param node
	 */
	private void reInstrumentGEBIDCalls(AstNode node, AstNode instrumentedNode) {
		GEBIDCallFinder gcf = new GEBIDCallFinder();
		node.visit(gcf);
		
		if (gcf.gebidCallIsFound() && gcf.getGebidParam() != null) {
			String param = gcf.getGebidParam();
			String dummyLine = "vejovisDummyVar = " + param + ";\n";
			
			//Insert the dummy line before the current node's line
			AstNode parent = makeSureBlockExistsAround(node);
			AstNode newNode = parse(dummyLine);
			parent.addChildBefore(newNode, instrumentedNode);
		}
	}

	/**
	 * This method is called when the complete AST has been traversed.
	 * 
	 * @param node
	 *            The AST root node.
	 */
	public abstract void finish(AstRoot node);

	/**
	 * This method is called before the AST is going to be traversed.
	 */
	public abstract void start();
	
	private int m_rootCount = 0;
	
	//Injection Methods
	public void setInject() {
		injectMode = true;
	}
	
	public void setInjectLineNo(int _lineNo) {
		injectLineNo = _lineNo;
	}
	
	public void setInjectOrigSrc(String _origSrc) {
		injectOrigSrc = _origSrc;
	}
	
	public void setInjectModSrc(String _modSrc) {
		injectModSrc = _modSrc;
	}
	
	public void setInjectFuncName(String _funcName) {
		injectFuncName = _funcName;
	}
	
	public void setInjectType(int _type) {
		injectType = _type;
	}
}
