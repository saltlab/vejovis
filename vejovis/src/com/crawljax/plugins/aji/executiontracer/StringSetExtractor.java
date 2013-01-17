package com.crawljax.plugins.aji.executiontracer;

import java.io.*;
import java.util.*;
import org.mozilla.javascript.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawljaxException;
import com.crawljax.core.plugin.GeneratesOutput;
import com.crawljax.core.plugin.PostCrawlingPlugin;
import com.crawljax.core.plugin.PreCrawlingPlugin;
import com.crawljax.core.plugin.PreStateCrawlingPlugin;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.util.Helper;

import org.w3c.dom.*;
import com.rca.*;
import org.mozilla.javascript.ast.*;

import com.exceptions.*;

public class StringSetExtractor {
	private static String JS_SOURCE_FOLDER;
	private static String TRACES_FOLDER;
	private static DirectDOMAccess dda;
	
	private String null_var; //The current variable being tracked
	private FunctionTrace null_var_ft; //The FunctionTrace corresponding to the current null_var
	
	//TODO: Include output variable here containing string set
	private List<StringSetLine> strSet;
	private String erroneousID;
	private int currentState;
	
	private Stack<String> funcStack; //The function stack
	private String last_func; //Set to the last function popped before the stack becomes empty
	
	private int indexToChange; //index to change the iterator's starting position in relevantSeq
	private boolean indexToChangeModified; //set to true if indexToChange has just been modified
	private boolean wasReturned; //Indicates if the previous return statement's return value is a function call
	
	//Members used in inject mode
	private boolean injectMode = false; //can be set to true by calling setInject()
	private int injectLineNo = 0;
	private String injectOrigSrc = "";
	private String injectModSrc = "";
	private String injectFuncName = "";
	private int injectType = 0; //0 - regular, 1 - DOM node removal
	
	public StringSetExtractor(String jsSourceFolder, String tracesFolder, DirectDOMAccess _dda) {
		JS_SOURCE_FOLDER = jsSourceFolder;
		TRACES_FOLDER = tracesFolder;
		dda = _dda;
		
		indexToChangeModified = false;
		wasReturned = false;
		last_func = "";
		funcStack = new Stack<String>();
		
		strSet = new ArrayList<StringSetLine>();
		erroneousID = null;
		currentState = 0;
	}
	
	/**
	 * Parse some JavaScript to a simple AST.
	 * 
	 * @param code
	 *            The JavaScript source code to parse.
	 * @return The AST node.
	 */
	public AstNode parse(String code) {
		Parser p = new Parser(new CompilerEnvirons(), null);
		return p.parse(code, null, 0);
	}
	
	public int extractStringSet() throws Exception {
		//Find direct DOM access by going through each file 
		//in the trace folder
		boolean directDomAccessFound = false;
		List<FunctionTrace> pertinentSeq = null;
		String srcLine = "";
		FunctionTrace theFT = null;
		int ddaIdx = -1; //index of direct DOM access in pertinent sequence
		
		File dir = new File(TRACES_FOLDER);
		for (File child : dir.listFiles()) {
			//Ignore the self and parent aliases, and files beginning with "."
			if (".".equals(child.getName()) || "..".equals(child.getName()) || child.getName().indexOf(".") == 0) {
			      continue; 
			}
			
			String fullPath = TRACES_FOLDER + "/" + child.getName();
			
			//Find the current state index based on the filename
			int lastDash = child.getName().lastIndexOf("-");
			int dotDtrace = child.getName().lastIndexOf(".dtrace");
			String currStateStr = child.getName().substring(lastDash + 1, dotDtrace);
			int currState = 0;
			try {
				currState = Integer.parseInt(currStateStr);
			}
			catch (NumberFormatException nfe) {
				System.err.println("Error: Incorrectly formatted trace file name");
				throw new Exception();
				//System.exit(-1);
			}
			
			//Parse the trace file and extract sequences
			TraceParser tp = new TraceParser(fullPath);
			List<List<FunctionTrace>> sequences = extractSequences(tp.function_traces);
			
			//Now that we have the sequences, go through each sequence
			//For each trace in the sequence, find the corresponding
			//line (should be of type INTERMEDIATE), acquire the
			//source line, and compare with the given source line
			for (int i = 0; i < sequences.size(); i++) {
				List<FunctionTrace> seq = sequences.get(i);
				for (int j = 0; j < seq.size(); j++) {
					FunctionTrace nextTrace = seq.get(j);
					
					String funcName = getFunctionName(nextTrace);
					int funcLineno = getFunctionLineno(nextTrace);
					if (funcLineno == -1) {
						continue;
					}
					String lineType = getLineType(nextTrace);
					
					if (funcName.equals(dda.funcName()) && funcLineno == dda.getLineNo() && lineType.equals("INTERMEDIATE")) {
						//Get the source code for this line
						srcLine = getLine(nextTrace);
						String srcLineNoLTSpaces = parse(srcLine).toSource().trim();
						String ddaLineNoLTSpaces = dda.getSrcLine().trim();
						
						if (srcLineNoLTSpaces.equals(ddaLineNoLTSpaces)) {
							//Found
							directDomAccessFound = true;
							ddaIdx = j;
							pertinentSeq = seq;
							srcLine = srcLineNoLTSpaces;
							theFT = nextTrace;
							currentState = currState;
							
							//Get the erroneousID
							List<VariableDesc> ftVars = theFT.f_decl.var_descs;
							List<String> ftVarValues = theFT.var_values;
							for (int k = 0; k < ftVars.size(); k++) {
								VariableDesc vd = ftVars.get(k);
								if (vd.getVarName().equals("vejovisDummyVar")) {
									String erroneousIDWithQuotes = ftVarValues.get(k);
									erroneousID = erroneousIDWithQuotes.substring(1, erroneousIDWithQuotes.length()-1);
									break;
								}
							}
							if (erroneousID == null) {
								System.err.println("Error: Erroneous ID not found");
								throw new Exception();
								//System.exit(-1);
							}
								
							break;
						}
					}
				}
				if (directDomAccessFound) {
					break;
				}
			}
			if (directDomAccessFound) {
				break;
			}
		}
		
		if (!directDomAccessFound) {
			return -1;
		}
		
		//If this point is reached, that means the direct DOM access
		//was found in one of the sequences
		
		//Get the actual GEBID function call
		GEBIDSrcExtractor gse = new GEBIDSrcExtractor();
		AstNode srcLineAst = parse(srcLine);
		srcLineAst.visit(gse);
		String gebidSrc = "";
		String gebidSrcConcat = "";
		if (gse.getGebidSrc() != null) {
			gebidSrc = gse.getGebidSrc();
			gebidSrcConcat = gse.getGebidSrcConcat();
		}
		else {
			return -1;
		}
		
		//Look for all variables/objects in the concatenation (use "+" as the delimiter)
		//Resolve all variables/objects by finding their last assignment
		//These variables/objects may be split into multiple variables/objects
		//Standalone numbers should be considered string literals
		try {
			traceVariables(gebidSrcConcat, ddaIdx, this.getFunctionLineno(theFT), pertinentSeq, strSet);
		}
		catch (Exception e) {
			System.err.println("String Set Cannot be Extracted");
			return -1;
		}
		
		//Return if successful
		return 0;
	}
	
	public void traceVariables(String concat, int seqIndex, int lineno, List<FunctionTrace> pertinentSeq, List<StringSetLine> ss) throws Exception {
		//Use "+" as delimeter and split concat into different tokens
		StringTokenizer st = new StringTokenizer(concat, "+");
		while (st.hasMoreTokens()) {
			String next = st.nextToken();
			
			next = next.trim(); //get rid of trailing and leading whitespace
			
			//Is it a string literal?
			if (next.startsWith("\"") || next.startsWith("\'") || strIsInteger(next)) {
				String finalStringLiteral = next.trim(); //trim whitespace
				if (next.startsWith("\"") || next.startsWith("\'")) {
					finalStringLiteral = finalStringLiteral.substring(1,finalStringLiteral.length()-1);
				}
				StringSetLine newSSL = new StringSetLine(lineno, null, finalStringLiteral, StringSetLine.Type.STRING_LITERAL.ordinal(), null, seqIndex, getFunctionName(pertinentSeq.get(seqIndex)));
				ss.add(newSSL);
			}
			else {
				//If it's not a string literal, then we'll assume it's a variable/object
				//that needs to be traced
				List<StringSetLine> newList = new ArrayList<StringSetLine>();
				StringSetLine newSSL = new StringSetLine(lineno, next.trim(), null, StringSetLine.Type.STRING_SET_LINE_LIST.ordinal(), newList, seqIndex, getFunctionName(pertinentSeq.get(seqIndex)));
				ss.add(newSSL);
			}
		}
		
		//Trace through all entries of ss so far
		//If entry is of type STRING_SET_LINE_LIST, call traceVar
		//on that entry to find its origin
		for (StringSetLine ssl : ss) {
			if (ssl.getType() == StringSetLine.Type.STRING_SET_LINE_LIST.ordinal()) {
				try {
					traceVar(ssl.getVarIdent(), ssl.getSeqIndex(), pertinentSeq, ssl);
				}
				catch (UnsuccessfulEnterException uee) {
					//Mark this ssl as possibly originating from a bad function call with
					//missing parameters
					FunctionTrace possiblyBadFuncCall = uee.getPossibleBadFuncCall();
					int funcCallLineNo = this.getFunctionLineno(possiblyBadFuncCall);
					String funcCallfname = this.getFunctionName(possiblyBadFuncCall);
					ssl.setHasPossiblyBadFuncCall(funcCallLineNo, funcCallfname, uee.getParamNum());
				}
			}
		}
	}
	
	public void traceVar(String varName, int seqIndex, List<FunctionTrace> pertinentSeq, StringSetLine ssl) throws Exception {
		null_var = varName;
		null_var_ft = null;
		
		//Reset values
		indexToChangeModified = false;
		wasReturned = false;
		last_func = "";
		funcStack = new Stack<String>();
		
		//Set up iterator for the sequence and determine the name of the current function
		ListIterator<FunctionTrace> seqIterator = pertinentSeq.listIterator(seqIndex); //start at seqIndex
		if (pertinentSeq.isEmpty()) {
			System.err.println("Error: Pertinent sequence is empty");
			throw new Exception();
			//System.exit(-1);
		}
		else {
			FunctionTrace lastFt = seqIterator.previous();
			String firstFunctionName = this.getFunctionName(lastFt);
			pushFunc(firstFunctionName);
			null_var_ft = lastFt;
		}
		
		//Reset iterator
		seqIterator = pertinentSeq.listIterator(seqIndex);
		int currIndex = seqIndex;
		int paramNum = -1;
		
		//Variables to handle asynchronous calls
		boolean foundAsync = false; //true if a trace of type ASYNC has just been encountered
		boolean foundAsyncCall = false; //true if a trace of type ASYNC_CALL has just been encountered
		String callbackFunctionCall = null; //the call to the asynchronous function made by the caller function
		
		boolean firstLine = true;
		
		while (seqIterator.hasPrevious()) {
			//Get next FunctionTrace in list
			FunctionTrace ft = seqIterator.previous();
			currIndex--;
			
			String currType = getLineType(ft);
			String currLine = "";
			List<String> lineTokens = new ArrayList<String>();
			if (!(currType.equals("ASYNC_CALL") || currType.equals("ASYNC"))) {
				currLine = getLine(ft);
				lineTokens = getTokenStream(currLine);
				
				//Check the first line as this line itself might be the one containing the assignment line
				//No need to check if it's of the form "return <expr>" or an assignment - subsequent checks will take care of that
				if (firstLine) {
					if (isJSAssignment(lineTokens, null_var)) {
						//Get RHS and call traceVariables
						AssnConcatExtractor ace = new AssnConcatExtractor();
						AstNode assnNode = parse(currLine);
						assnNode.visit(ace);
						if (!(ace.getAssnConcat().equals(""))) {
							traceVariables(ace.getAssnConcat(), currIndex, this.getFunctionLineno(ft), pertinentSeq, ssl.getSSLL());
						}
						return;
					}
				}
			}
			String currFunction = getFunctionName(ft);
			
			firstLine = false; //so that next line doesn't get recognized as the first line
			
			/*ASYNC START*/
			//Check if it is an extraneous ASYNC_CALL
			if (currType.equals("ASYNC_CALL") && !foundAsync) {
				continue;
			}
			
			//Check if the current line is of type ASYNC
			if (currType.equals("ASYNC")) {
				foundAsync = true;
				continue;
			}
			
			//Check if an ASYNC has just been encountered
			if (foundAsync) {
				//First, assure that the current line is an ASYNC_CALL
				if (!currType.equals("ASYNC_CALL")) {
					System.err.println("Error: ASYNC not preceded by an ASYNC_CALL");
					throw new Exception();
					//System.exit(-1);
				}
				else {
					foundAsync = false;
					foundAsyncCall = true;
					callbackFunctionCall = this.getCallbackFunctionCall(ft);
				}
				continue;
			}
			
			//Check if an ASYNC_CALL has just been encountered
			if (foundAsyncCall) {
				//First, assure that the stack is empty
				assert(funcStackIsEmpty());
				
				//Also, assert that callbackFunctionCall is not null
				assert(callbackFunctionCall != null);
				
				//Change the current line to the corresponding callback function call
				currLine = callbackFunctionCall;
				lineTokens = getTokenStream(currLine);
				
				//Reset foundAsyncCall
				foundAsyncCall = false;
				callbackFunctionCall = null;
			}
			/*ASYNC END*/
			
			//Check if the stack is empty
			if (funcStackIsEmpty()) {
				//The primary function will change. First, verify if the current line is indeed a function call
				if (!isJSFunctionCall(lineTokens, last_func)) {
					System.err.println("Error: Primary function changed, but no corresponding function call");
					throw new Exception();
					//System.exit(-1);
				}
				else {
					pushFunc(currFunction);
					
					//If the current null_var is local, check the argument the current
					//null_var corresponds to in the function call
					try {
						if (isVarLocal(null_var_ft,null_var)) {
							assert(paramNum != -1);
							String newNullVar = getArgument(lineTokens, paramNum, last_func);
							setNullVar(ft, newNullVar);
						}
					}
					catch (Exception e) {
						return; //Can't resolve this one because it's not a single variable
					}
				}
				continue;
			}
			
			//Check if the current line is an ENTER
			if (currType.equals("ENTER")) {
				//We must've reached the point where the current function was called, so pop this function from the stack
				//First, check if the top of the stack equals the function name of the current line
				if (!currFunction.equals(funcStack.peek())) {
					System.err.println("Error: Function name mismatch");
					throw new Exception();
					//System.exit(-1);
				}
				else {
					popFunc();
					try {
						if (isVarLocal(null_var_ft, null_var)) {
							String funcDecl = getJSFuncDecl(ft);
							List<String> funcDeclTokens = getTokenStream(funcDecl);
							paramNum = getParamNumber(funcDeclTokens, currFunction);
						}
					}
					catch (Exception e) {
						//Check if this ENTER is preceded by an ASYNC, an ASYNC_CALL, and an INTERMEDIATE
						//If so, throw an UnsuccessfulEnterException with the INTERMEDIATE as the function trace
						if (seqIterator.hasPrevious()) {
							FunctionTrace precedingFt = seqIterator.previous();
							String precedingFtType = getLineType(precedingFt);
							if (precedingFtType.equals("ASYNC") && seqIterator.hasPrevious()) {
								FunctionTrace asyncCallFt = seqIterator.previous();
								String asyncCallFtType = getLineType(asyncCallFt);
								if (asyncCallFtType.equals("ASYNC_CALL") && seqIterator.hasPrevious()) {
									FunctionTrace interFt = seqIterator.previous();
									String interFtType = getLineType(interFt);
									if (interFtType.equals("INTERMEDIATE")) {
										throw new UnsuccessfulEnterException(interFt, -1, currFunction);
									}
								}
							}
							else if (precedingFtType.equals("INTERMEDIATE")) {
								throw new UnsuccessfulEnterException(precedingFt, -1, currFunction);
							}
						}
						return; //Can't resolve this one because it's not a single variable
					}
				}
				continue;
			}
			
			//If this point is reached, the stack should be non-empty
			//Assumption: No recursive functions
			
			//Check if the current function does not match the function at the top of the stack
			if (!currFunction.equals(funcStack.peek())) {
				//There must have been a function call in the function at the top of the stack calling the current function
				//Thus, push the current function at the top of the stack
				assert(currType.equals("EXIT"));
				String callingFunction = funcStack.peek();
				pushFunc(currFunction);
				
				//Peek ahead to see if the corresponding function call is of the form <null var> = <function call>
				if (isFuncCallAfterPeek(pertinentSeq, currIndex, currFunction, callingFunction)) {
					//First, verify that the current expression is a return expression
					if (!isReturnExpr(lineTokens)) {
						System.err.println("Error: Function from which return value is expected does not end with return expression");
						throw new Exception();
						//System.exit(-1);
					}
					else {
						//Get the return expression
						List<String> retExpr = returnExpr(lineTokens);
						
						//Check if the return expression is a concatenation (i.e., not a function call)
						if (!isFuncCall(retExpr)) {
							//Get return expression and call traceVariables
							AssnConcatExtractor ace = new AssnConcatExtractor();
							AstNode assnNode = parse(currLine);
							assnNode.visit(ace);
							if (!(ace.getAssnConcat().equals(""))) {
								traceVariables(ace.getAssnConcat(), currIndex, this.getFunctionLineno(ft), pertinentSeq, ssl.getSSLL());
							}
							return;
						}
						else { //if (isFuncCall(retExpr))
							wasReturned = true;
							continue;
						}
					}
				}
				else {
					try {
						if (wasReturned) {
							wasReturned = false;
							
							//First, verify that the current expression is a return expression
							if (!isReturnExpr(lineTokens)) {
								System.err.println("Error: Function from which return value is expected does not end with return expression");
								throw new Exception();
								//System.exit(-1);
							}
							else {
								//Get the return expression
								List<String> retExpr = returnExpr(lineTokens);
								
								//Check if the return expression is a concatenation (i.e., not a function call)
								if (!isFuncCall(retExpr)) {
									//Get return expression and call traceVariables
									AssnConcatExtractor ace = new AssnConcatExtractor();
									AstNode assnNode = parse(currLine);
									assnNode.visit(ace);
									if (!(ace.getAssnConcat().equals(""))) {
										traceVariables(ace.getAssnConcat(), currIndex, this.getFunctionLineno(ft), pertinentSeq, ssl.getSSLL());
									}
									return;
								}
								else { //if (isFuncCall(retExpr))
									wasReturned = true;
									continue;
								}
							}
						}
						else if (isVarLocal(null_var_ft, null_var)) {
							//Move iterator to next line of the calling function
							if (!indexToChangeModified) {
								System.err.println("Error: Invalid indexToChange value about to be used");
								throw new Exception();
								//System.exit(-1);
							}
							seqIterator = pertinentSeq.listIterator(indexToChange);
							currIndex = indexToChange;
							indexToChangeModified = false;
							
							//Pop the current function off the stack
							popFunc();
							continue;
						}
						else { //If null_var is global
							continue;
						}
					}
					catch (Exception e) {
						return; //Can't resolve this one because it's not a single variable
					}
				}
			}
			
			//At this point, the current function must have matched the top of the stack
			
			//Check if the current line is of the form <null var> = expression
			if (isJSAssignment(lineTokens, null_var)) {
				//Get RHS and call traceVariables
				AssnConcatExtractor ace = new AssnConcatExtractor();
				AstNode assnNode = parse(currLine);
				assnNode.visit(ace);
				if (!(ace.getAssnConcat().equals(""))) {
					traceVariables(ace.getAssnConcat(), currIndex, this.getFunctionLineno(ft), pertinentSeq, ssl.getSSLL());
				}
				return;
			}
			else {
				continue;
			}
		}
	}
	
	/**
	 * Tests if the function stack is empty
	 * 
	 * @return true if the function stack is empty; false otherwise
	 */
	public boolean funcStackIsEmpty() {
		return funcStack.empty();
	}
	
	/**
	 * Pushes a JavaScript function name onto the stack
	 * 
	 * @param func The JavaScript function name
	 */
	private void pushFunc(String func) {
		funcStack.push(func);
	}
	
	/**
	 * Pops a function name from the top of the function stack
	 * If the function name popped is the only one remaining in the stack,
	 * set last_func to this function name.
	 */
	private void popFunc() {
		String funcToPop = funcStack.pop();
		if (this.funcStackIsEmpty()) {
			last_func = funcToPop;
		}
	}
	
	private boolean isJSAssignment(List<String> tokenList, String varName) throws Exception {
		List<String> lhsTokens = getTokenStream(varName);
		Iterator lhsTokensIt = lhsTokens.iterator();
		
		final int
			St_START = 1,
			St_VAR_DECL = 2,
			St_LP_START = 3,
			St_MATCH_FOUND = 4,
			St_EQ_FOUND = 5;
		
		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;
		
		while (tokenIt.hasNext()) {
			String nextToken = (String)tokenIt.next();
			switch(currentState) {
				case St_START:
					if (nextToken.equals("LP")) {
						currentState = St_LP_START;
					}
					else if (nextToken.equals("VAR")) {
						currentState = St_VAR_DECL;
					}
					else {
						if (lhsTokensIt.hasNext()) {
							String nextLhsToken = (String)lhsTokensIt.next();
							if (nextToken.equals(nextLhsToken)) {
								currentState = St_MATCH_FOUND;
							}
							else {
								return false;
							}
						}
						else {
							return false;
						}
					}
					break;
				case St_VAR_DECL:
					if (lhsTokensIt.hasNext()) {
						String nextLhsToken = (String)lhsTokensIt.next();
						if (nextToken.equals(nextLhsToken)) {
							currentState = St_MATCH_FOUND;
						}
						else {
							return false;
						}
					}
					else {
						return false;
					}
					break;
				case St_LP_START:
					if (nextToken.equals("LP")) {
						currentState = St_LP_START;
					}
					else if (nextToken.equals("VAR")) {
						currentState = St_VAR_DECL;
					}
					else {
						if (lhsTokensIt.hasNext()) {
							String nextLhsToken = (String)lhsTokensIt.next();
							if (nextToken.equals(nextLhsToken)) {
								currentState = St_MATCH_FOUND;
							}
							else {
								return false;
							}
						}
						else {
							return false;
						}
					}
					break;
				case St_MATCH_FOUND:
					if (lhsTokensIt.hasNext()) {
						String nextLhsToken = (String)lhsTokensIt.next();
						if (nextToken.equals(nextLhsToken)) {
							currentState = St_MATCH_FOUND;
						}
						else {
							return false;
						}
					}
					else if (nextToken.equals("ASSIGN")) {
						currentState = St_EQ_FOUND;
					}
					else {
						return false;
					}
					break;
				case St_EQ_FOUND:
					return true;
				default:
					System.err.println("Error: Incorrect state in isJSAssignment");
					throw new Exception();
					//System.exit(-1);
					//break;
			}
		}	
		return false;
	}
	
	/**
	 * Parses a line of JavaScript code based on its tokens and determines if
	 * this line contains a function call to the specified function
	 * 
	 * @param tokenList The tokens to parse
	 * @param funcName The name of the function expected to be called in the JS line tested
	 * @return true if the JavaScript line contains a function call to funcName; false otherwise
	 */
	private boolean isJSFunctionCall(List<String> tokenList, String funcName) throws Exception {
		//Set up states
		final int
			St_START = 1,
			St_VAR_DECL = 2,
			St_LP_START = 3,
			St_FALSE = 4,
			St_NAME_FOUND = 5,
			St_ERROR = 6,
			St_FUNC_NAME_FOUND = 7,
			St_NAME_STR = 8,
			St_TRUE = 9,
			St_ASSN = 10,
			St_NAME_FOUND_AFTER_ASSN = 11,
			St_NAME_STR_AFTER_ASSN = 12,
			St_CONT_LOOKING = 13;
		
		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;
		
		while (tokenIt.hasNext()) {
			String nextToken = (String)tokenIt.next();
			switch(currentState) {
				case St_START:
					if (nextToken.equals("LP")) {
						currentState = St_LP_START;
					}
					else if (nextToken.equals("VAR")) {
						currentState = St_VAR_DECL;
					}
					else if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND;
					}
					else {
						return false;
					}
					break;
				case St_VAR_DECL:
					if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND;
					}
					else {
						return false;
					}
					break;
				case St_LP_START:
					if (nextToken.equals("LP")) {
						//state stays the same
					}
					else if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND;
					}
					else if (nextToken.equals("VAR")) {
						currentState = St_VAR_DECL;
					}
					else {
						return false;
					}
					break;
				case St_NAME_FOUND:
					if (nextToken.equals(funcName)) {
						currentState = St_FUNC_NAME_FOUND;
					}
					else {
						currentState = St_NAME_STR;
					}
					break;
				case St_FUNC_NAME_FOUND:
					if (nextToken.equals("LP")) {
						return true;
					}
					else {
						currentState = St_CONT_LOOKING;
					}
					break;
				case St_NAME_STR:
					if (nextToken.equals("ASSIGN")) {
						currentState = St_ASSN;
					}
					else {
						currentState = St_CONT_LOOKING;
					}
					break;
				case St_ASSN:
					if (nextToken.equals("LP")) {
						//state stays the same
					}
					else if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND_AFTER_ASSN;
					}
					else {
						currentState = St_CONT_LOOKING;
					}
					break;
				case St_NAME_FOUND_AFTER_ASSN:
					if (nextToken.equals(funcName)) {
						currentState = St_FUNC_NAME_FOUND;
					}
					else {
						currentState = St_NAME_STR_AFTER_ASSN;
					}
					break;
				case St_NAME_STR_AFTER_ASSN:
					currentState = St_CONT_LOOKING;
					break;
				case St_CONT_LOOKING:
					if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND_AFTER_ASSN;
					}
					else {
						//state stays the same
					}
					break;
				default:
					System.err.println("Error: Incorrect state in isJSFunctionCall");
					throw new Exception();
					//System.exit(-1);
					//break;
			}
		}
		
		return false;
	}
	
	/**
	 * Tests if a variable is local in the function containing the specified
	 * function trace
	 * 
	 * @param ft The function trace
	 * @param varName The variable to test
	 * @return true if varName is local in the function corresponding to ft; false otherwise
	 */
	private boolean isVarLocal(FunctionTrace ft, String varName) throws Exception {
		if (ft == null || varName == null || varName == "") {
			System.err.println("Error: Empty variable or trace passed to isVarLocal");
			throw new Exception();
			//System.exit(-1);
		}
		
		//Find varName in ft's variable list
		List<VariableDesc> vDesc_list = ft.f_decl.var_descs;
		Iterator<VariableDesc> vDesc_it = vDesc_list.iterator();
		VariableDesc vDesc = null;
		boolean varFound = false;
		while (vDesc_it.hasNext()) {
			vDesc = vDesc_it.next();
			if (vDesc.getVarName().equals(varName)) {
				varFound = true;
				break;
			}
		}
		
		if (!varFound) {
			//Might not be single variable
			//System.err.println("Error: variable name not found in function declaration");
			throw new Exception();
			//System.exit(-1);
		}
		
		//If this point is reached, the variable must have been found
		//and vDesc should contain the correct VariableDesc
		boolean isLocal = false;
		if (!vDesc.isGlobal()) {
			isLocal = true;
		}
		
		return isLocal;
	}
	
	/**
	 * Determines the parameter name at a certain position in the function call
	 * <p>
	 * The name of the function must match the name in the function call tokens
	 * 
	 * @param tokenList The tokens for the function call (may contain extra tokens at the end, which could be neglected)
	 * @param paramNum The index of the argument to fetch (starting from 1)
	 * @param funcName The name of the function called
	 * @return A string representing the parameter name
	 */
	private String getArgument(List<String> tokenList, int paramNum, String funcName) throws Exception {
		//Set up states
		final int
			St_START = 1,
			St_VAR_DECL = 2,
			St_LP_START = 3,
			St_FALSE = 4,
			St_NAME_FOUND = 5,
			St_ERROR = 6,
			St_FUNC_NAME_FOUND = 7,
			St_NAME_STR = 8,
			St_TRUE = 9,
			St_ASSN = 10,
			St_NAME_FOUND_AFTER_ASSN = 11,
			St_NAME_STR_AFTER_ASSN = 12,
			St_CONT_LOOKING = 13,
			St_FUNC_FOUND = 14,
			St_FIND_ARG = 15,
			St_FOUND_ARG_NAME_TOKEN = 16;
		
		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;
		
		int paramCounter = 1;
		
		while (tokenIt.hasNext()) {
			String nextToken = (String)tokenIt.next();
			switch(currentState) {
				case St_START:
					if (nextToken.equals("LP")) {
						currentState = St_LP_START;
					}
					else if (nextToken.equals("VAR")) {
						currentState = St_VAR_DECL;
					}
					else if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND;
					}
					else {
						System.err.println("Error: Invalid function call tokens");
						throw new Exception();
						//System.exit(-1);
					}
					break;
				case St_VAR_DECL:
					if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND;
					}
					else {
						System.err.println("Error: Invalid function call tokens");
						throw new Exception();
						//System.exit(-1);
					}
					break;
				case St_LP_START:
					if (nextToken.equals("LP")) {
						//state stays the same
					}
					else if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND;
					}
					else if (nextToken.equals("VAR")) {
						currentState = St_VAR_DECL;
					}
					else {
						System.err.println("Error: Invalid function call tokens");
						throw new Exception();
						//System.exit(-1);
					}
					break;
				case St_NAME_FOUND:
					if (nextToken.equals(funcName)) {
						currentState = St_FUNC_NAME_FOUND;
					}
					else {
						currentState = St_NAME_STR;
					}
					break;
				case St_FUNC_NAME_FOUND:
					if (nextToken.equals("LP")) {
						if (paramCounter == paramNum) {
							currentState = St_FIND_ARG;
						}
						else {
							currentState = St_FUNC_FOUND;
						}
					}
					else {
						currentState = St_CONT_LOOKING;
					}
					break;
				case St_NAME_STR:
					if (nextToken.equals("ASSIGN")) {
						currentState = St_ASSN;
					}
					else {
						currentState = St_CONT_LOOKING;
					}
					break;
				case St_ASSN:
					if (nextToken.equals("LP")) {
						//state stays the same
					}
					else if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND_AFTER_ASSN;
					}
					else {
						currentState = St_CONT_LOOKING;
					}
					break;
				case St_NAME_FOUND_AFTER_ASSN:
					if (nextToken.equals(funcName)) {
						currentState = St_FUNC_NAME_FOUND;
					}
					else {
						currentState = St_NAME_STR_AFTER_ASSN;
					}
					break;
				case St_NAME_STR_AFTER_ASSN:
					currentState = St_CONT_LOOKING;
					break;
				case St_CONT_LOOKING:
					if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND_AFTER_ASSN;
					}
					else {
						//state stays the same
					}
					break;
				case St_FUNC_FOUND:
					if (nextToken.equals("COMMA")) {
						paramCounter++;
						if (paramCounter == paramNum) {
							currentState = St_FIND_ARG;
						}
					}
					else {
						//state stays the same
					}
					break;
				case St_FIND_ARG:
					if (nextToken.equals("NAME")) {
						currentState = St_FOUND_ARG_NAME_TOKEN;
					}
					else {
						System.err.println("Error: Invalid function call tokens");
						throw new Exception();
						//System.exit(-1);
					}
					break;
				case St_FOUND_ARG_NAME_TOKEN:
					return nextToken;
				default:
					System.err.println("Error: Incorrect state in getArgument");
					throw new Exception();
					//System.exit(-1);
					//break;
			}
		}
		
		//If this point is reached, we must've run out of tokens
		System.err.println("Error: Invalid function call tokens");
		throw new Exception();
		//System.exit(-1);
		
		//return ""; //This won't be reached
	}
	
	/**
	 * Gets the function declaration string from the corresponding JS function
	 * 
	 * @param ft The function trace
	 * @return A string representing the declaration of the function corresponding to ft
	 */
	private String getJSFuncDecl(FunctionTrace ft) throws Exception {
		String current_function = getFunctionName(ft);
		int lineno = 1;
		String strLine = null;
		try {
			FileInputStream fstream = new FileInputStream(JS_SOURCE_FOLDER + "/" + current_function + ".js");
			DataInputStream din = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(din));
			
			String functionDeclaration = null;
			List<String> functionDeclarationTokens = null;
			if ((strLine = br.readLine()) != null) {
				List<String> token_stream = getTokenStream(strLine);
				Iterator token_stream_it = token_stream.iterator();
				if (token_stream_it.hasNext()) {
					String firstToken = (String)token_stream_it.next();
					if (!firstToken.equals("FUNCTION")) {
						System.err.println("Error: First line is not a function declaration");
						throw new Exception();
						//System.exit(-1);
					}
				}
				else {
					System.err.println("Error: No tokens in function declaration");
					throw new Exception();
					//System.exit(-1);
				}
			}
			else {
				System.err.println("Error: Empty function");
				throw new Exception();
				//System.exit(-1);
			}
		}
		catch (Exception e) {
			System.err.println("Error reading function file");
			throw new Exception();
			//System.exit(-1);
		}
		
		return strLine;
	}
	
	/**
	 * Determines the position of the null_var in the function declaration of the current function
	 * 
	 * @param tokenList The tokens representing the function declaration
	 * @param funcName The name of the current function
	 * @return The index (starting from 1) representing the position of the current null_var in the function declaration
	 */
	private int getParamNumber(List<String> tokenList, String funcName) throws Exception {
		final int
			St_START = 1,
			St_FUNC_PREFIX = 2,
			St_FUNC_NAME_TOKEN = 3,
			St_FUNC_NAME = 4,
			St_IN_PARAMS = 5,
			St_CHECK_PARAM = 6,
			St_FOUND_COMMA = 7;
		
		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;
		
		int paramCount = 0;
		
		while (tokenIt.hasNext()) {
			String nextToken = (String)tokenIt.next();
			switch(currentState) {
				case St_START:
					if (nextToken.equals("FUNCTION")) {
						currentState = St_FUNC_PREFIX;
					}
					else {
						System.err.println("Error: Invalid function declaration tokens");
						throw new Exception();
						//System.exit(-1);
					}
					break;
				case St_FUNC_PREFIX:
					if (nextToken.equals("NAME")) {
						currentState = St_FUNC_NAME_TOKEN;
					}
					else {
						System.err.println("Error: Invalid function declaration tokens");
						throw new Exception();
						//System.exit(-1);
					}
					break;
				case St_FUNC_NAME_TOKEN:
					if (nextToken.equals(funcName)) {
						currentState = St_FUNC_NAME;
					}
					else {
						System.err.println("Error: Invalid function declaration tokens");
						throw new Exception();
						//System.exit(-1);
					}
					break;
				case St_FUNC_NAME:
					if (nextToken.equals("LP")) {
						currentState = St_IN_PARAMS;
						paramCount++;
					}
					else {
						System.err.println("Error: Invalid function declaration tokens");
						throw new Exception();
						//System.exit(-1);
					}
					break;
				case St_IN_PARAMS:
					if (nextToken.equals("NAME")) {
						currentState = St_CHECK_PARAM;
					}
					else if (nextToken.equals("COMMA")) {
						currentState = St_FOUND_COMMA;
					}
					else {
						//state stays the same
					}
					break;
				case St_CHECK_PARAM:
					if (nextToken.equals(null_var)) {
						return paramCount;
					}
					else {
						currentState = St_IN_PARAMS;
					}
					break;
				case St_FOUND_COMMA:
					paramCount++;
					if (nextToken.equals("NAME")) {
						currentState = St_CHECK_PARAM;
					}
					else {
						currentState = St_IN_PARAMS;
					}
					break;
				default:
					System.err.println("Error: Incorrect state in getParamNumber");
					throw new Exception();
					//System.exit(-1);
					//break;
			}
		}
		
		System.err.println("Error: Invalid function declaration tokens");
		throw new Exception();
		//System.exit(-1);
		
		//return -1; //this won't be reached
	}
	
	/**
	 * Starting from a specified index, trace back through the trace sequence until the
	 * corresponding ENTER of the current function is found. If found, go back another trace
	 * and check if it corresponds to the calling function. If not, throw an error
	 * 
	 * @param trace_list The list of JS traces
	 * @param curr_index The index in the trace list currently being analyzed
	 * @param currFunction The function to which the current trace belongs
	 * @param callingFunc The function that called the line in the current execution trace
	 * @return true if the call to the current function is of the form "<null var> = <function call>"; false otherwise
	 */
	private boolean isFuncCallAfterPeek(List<FunctionTrace> trace_list, int curr_index, String currFunction, String callingFunc) throws Exception {
		ListIterator<FunctionTrace> l_itr = trace_list.listIterator(curr_index);
		boolean enterFound = false;
		int newIndex = curr_index;
		FunctionTrace theTrace;
		String theLine;
		while (l_itr.hasPrevious()) {
			FunctionTrace ft = l_itr.previous();
			newIndex--;
			String functionName = getFunctionName(ft);
			String traceType = getLineType(ft);
			
			if (functionName.equals(currFunction) && traceType.equals("ENTER")) {
				enterFound = true;
				theTrace = ft;
				indexToChange = newIndex; //index just "after" the function call ("before" in our order of analysis)
				this.indexToChangeModified = true;
				break;
			}
		}
		
		if (!enterFound) {
			System.err.println("Error: No corresponding ENTER");
			throw new Exception();
			//System.exit(-1);
		}
		else {
			if (!l_itr.hasPrevious()) {
				System.err.println("Error: No corresponding function call");
				throw new Exception();
				//System.exit(-1);
			}
			else {
				FunctionTrace ft = l_itr.previous();
				theLine = getLine(ft);
				List<String> lineTokens = getTokenStream(theLine);
				return isNullVarEqualsFuncCallAssn(lineTokens, currFunction);
			}
		}
		//return false;
	}
	
	/**
	 * Checks if the current line is of the form <null var> = <function call>
	 * 
	 * @param tokenList The list of tokens
	 * @param funcName The name of the function that is being called
	 * @return true if the corresponding list of tokens corresponds to a function call to funcName; false otherwise
	 */
	private boolean isNullVarEqualsFuncCallAssn(List<String> tokenList, String funcName) throws Exception {
		final int
			St_START = 1,
			St_VAR_DECL = 2,
			St_LP_START = 3,
			St_NAME_FOUND = 4,
			St_NULL_VAR_FOUND = 5,
			St_ASSN = 6,
			St_NAME_FOUND_AFTER_ASSN = 7,
			St_FUNC_NAME_FOUND = 8;
		
		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;
		
		while (tokenIt.hasNext()) {
			String nextToken = (String)tokenIt.next();
			switch(currentState) {
				case St_START:
					if (nextToken.equals("VAR")) {
						currentState = St_VAR_DECL;
					}
					else if (nextToken.equals("LP")) {
						currentState = St_LP_START;
					}
					else if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND;
					}
					else {
						return false;
					}
					break;
				case St_VAR_DECL:
					if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND;
					}
					else {
						return false;
					}
					break;
				case St_LP_START:
					if (nextToken.equals("LP")) {
						//state stays the same
					}
					else if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND;
					}
					else {
						return false;
					}
					break;
				case St_NAME_FOUND:
					if (nextToken.equals(null_var)) {
						currentState = St_NULL_VAR_FOUND;
					}
					else {
						return false;
					}
					break;
				case St_NULL_VAR_FOUND:
					if (nextToken.equals("ASSIGN")) {
						currentState = St_ASSN;
					}
					else {
						return false;
					}
					break;
				case St_ASSN:
					if (nextToken.equals("LP")) {
						//state stays the same
					}
					else if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND_AFTER_ASSN;
					}
					else {
						return false;
					}
					break;
				case St_NAME_FOUND_AFTER_ASSN:
					if (nextToken.equals(funcName)) {
						currentState = St_FUNC_NAME_FOUND;
					}
					else {
						return false;
					}
					break;
				case St_FUNC_NAME_FOUND:
					if (nextToken.equals("LP")) {
						return true;
					}
					else {
						return false;
					}
				default:
					System.err.println("Error: Incorrect state in isNullVarEqualsFuncCallAssn");
					throw new Exception();
					//System.exit(-1);
					//break;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the list of tokens represents a return expression
	 * 
	 * @param tokenList The list of tokens
	 * @return true if the line represented by the passed tokens is a return statement; false otherwise
	 */
	private boolean isReturnExpr(List<String> tokenList) throws Exception {
		final int
			St_START = 1,
			St_LP_START = 2;
		
		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;
		
		while (tokenIt.hasNext()) {
			String nextToken = (String)tokenIt.next();
			switch(currentState) {
				case St_START:
					if (nextToken.equals("LP")) {
						currentState = St_LP_START;
					}
					else if (nextToken.equals("RETURN")) {
						return true;
					}
					else {
						return false;
					}
					break;
				case St_LP_START:
					if (nextToken.equals("LP")) {
						//state stays the same
					}
					else if (nextToken.equals("RETURN")) {
						return true;
					}
					break;
				default:
					System.err.println("Error: Incorrect state in isReturnExpr");
					throw new Exception();
					//System.exit(-1);
					//break;
			}
		}
		
		return false;
	}
	
	/**
	 * Determines if the current line represents a return expression.
	 * If so, returns a list of tokens of the returned expression.
	 * Otherwise, returns an empty list of tokens.
	 * 
	 * @param tokenList The list of tokens
	 * @return A list of tokens of the returned expression if the current line is of the form "return expression";
	 * otherwise, returns an empty list of strings.
	 */
	private List<String> returnExpr(List<String> tokenList) throws Exception {
		final int
			St_START = 1,
			St_LP_START = 2;
		
		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;
		
		while (tokenIt.hasNext()) {
			String nextToken = (String)tokenIt.next();
			switch(currentState) {
				case St_START:
					if (nextToken.equals("LP")) {
						currentState = St_LP_START;
					}
					else if (nextToken.equals("RETURN")) {
						List<String> exprList = new ArrayList<String>();
						while (tokenIt.hasNext()) {
							exprList.add((String)tokenIt.next());
						}
						return exprList;
					}
					else {
						return new ArrayList<String>();
					}
					break;
				case St_LP_START:
					if (nextToken.equals("LP")) {
						//state stays the same
					}
					else if (nextToken.equals("RETURN")) {
						List<String> exprList = new ArrayList<String>();
						while (tokenIt.hasNext()) {
							exprList.add((String)tokenIt.next());
						}
						return exprList;
					}
					break;
				default:
					System.err.println("Error: Incorrect state in isReturnExpr");
					throw new Exception();
					//System.exit(-1);
					//break;
			}
		}

		return new ArrayList<String>();
	}
	
	/**
	 * Checks if the expression represented by the tokens is a function call
	 * 
	 * @param tokenList The list of tokens
	 * @return true if the expression is a function call; false otherwise
	 */
	private boolean isFuncCall(List<String> tokenList) throws Exception {
		final int
			St_START = 1,
			St_LP_START = 2,
			St_NAME_FOUND = 3,
			St_NAME_STR = 4;
		
		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;
		
		while (tokenIt.hasNext()) {
			String nextToken = (String)tokenIt.next();
			switch(currentState) {
				case St_START:
					if (nextToken.equals("LP")) {
						currentState = St_LP_START;
					}
					else if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND;
					}
					else {
						return false;
					}
					break;
				case St_LP_START:
					if (nextToken.equals("LP")) {
						//state stays the same
					}
					else if (nextToken.equals("NAME")) {
						currentState = St_NAME_FOUND;
					}
					else {
						return false;
					}
					break;
				case St_NAME_FOUND:
					currentState = St_NAME_STR;
					break;
				case St_NAME_STR:
					if (nextToken.equals("LP")) {
						return true;
					}
					else {
						return false;
					}
				default:
					System.err.println("Error: Incorrect state in isFuncCall");
					throw new Exception();
					//System.exit(-1);
					//break;
			}
		}
		
		return false;
	}
	
	/**
	 * Set null_var to a new value
	 * 
	 * @param newNullVar The value the new null_var will be set to
	 * @param newNullVarFt The FunctionTrace corresponding to the new null_var
	 */
	private void setNullVar(FunctionTrace newNullVarFt, String newNullVar) {
		this.null_var_ft = newNullVarFt;
		this.null_var = newNullVar;
	}
	
	String getCallbackFunctionCall(FunctionTrace ft) throws Exception {
		//Ensure ft is of type ASYNC_CALL
		String type = getLineType(ft);
		if (!type.equals("ASYNC_CALL")) {
			System.err.println("Error: Cannot get callback function call from non-ASYNC_CALL trace");
			throw new Exception();
			//System.exit(-1);
		}
		
		//Find which index contains the variable FuncCall
		List<VariableDesc> varDescList = ft.f_decl.var_descs;
		Iterator varDescIt = varDescList.iterator();
		int indexOfFuncCall = -1;
		boolean foundIndex = false;
		int counter = 0;
		while (varDescIt.hasNext() && !foundIndex) {
			VariableDesc varDesc = (VariableDesc)varDescIt.next();
			if (varDesc.getVarName().equals("FuncCall")) {
				indexOfFuncCall = counter;
				foundIndex = true;
			}
			counter++;
		}
		
		if (!foundIndex) {
			System.err.println("Error: Callback function call not stored in ASYNC_CALL");
			throw new Exception();
			//System.exit(-1);
		}
		
		String theCall = ft.var_values.get(indexOfFuncCall);
		
		//Strip the leading and trailing quotation marks (note that the dtrace always uses double quotes)
		if (theCall.startsWith("\"") && theCall.endsWith("\"")) {
			theCall = theCall.substring(1,theCall.length()-1);
		}
		
		return theCall;
	}
	
	public static List<List<FunctionTrace>> extractSequences(List<FunctionTrace> ft) {
		List<List<FunctionTrace>> sequences = new ArrayList<List<FunctionTrace>>();
		List<FunctionTrace> sequence = new ArrayList<FunctionTrace>();
		Iterator it = ft.iterator();
		
		boolean first_function = true;
		String first_function_ppt = null;
		String first_function_trace_type = null;
		boolean finishedSequence = true;
		//TODO: Handle recursive functions
		while (it.hasNext()) {
			FunctionTrace next_ft = (FunctionTrace)it.next();
			if (first_function) {
				sequence = new ArrayList<FunctionTrace>();
				first_function = false;
				sequence.add(next_ft);
				
				int index_from_colons = next_ft.f_decl.ppt_decl.indexOf(":::");
				first_function_ppt = next_ft.f_decl.ppt_decl.substring(0,index_from_colons);
				first_function_trace_type = next_ft.f_decl.ppt_decl.substring(index_from_colons);
				
				finishedSequence = false;
			}
			else {
				sequence.add(next_ft);
				
				//Determine if this is the last trace in the sequence
				int index_from_colons = next_ft.f_decl.ppt_decl.indexOf(":::");
				String function_ppt = next_ft.f_decl.ppt_decl.substring(0,index_from_colons);
				String function_trace_type = next_ft.f_decl.ppt_decl.substring(index_from_colons);
				
				boolean function_ppt_match = function_ppt.equals(first_function_ppt);
				boolean function_trace_type_exit = function_trace_type.startsWith(":::EXIT");
				boolean function_trace_type_error = function_trace_type.startsWith(":::ERROR");
				
				if (function_ppt_match && (function_trace_type_exit || function_trace_type_error)) {
					sequences.add(sequence);
					first_function = true;
					finishedSequence = true;
				}
			}
		}
		
		if (!finishedSequence) {
			sequences.add(sequence);
		}
		
		return sequences;
	}
	
	public static List<FunctionTrace> extractRelevantSequence(List<List<FunctionTrace>> llft) throws Exception {
		List<FunctionTrace> relevantSequence = null;
		
		Iterator it = llft.iterator();
		List<FunctionTrace> lft = null;
		List<FunctionTrace> finalFunctionTrace = new ArrayList<FunctionTrace>();
		boolean foundErrorTrace = false;
		while (it.hasNext()) {
			lft = (List<FunctionTrace>)it.next();
			Iterator it_trace = lft.iterator();
			while (it_trace.hasNext()) {
				FunctionTrace ft = (FunctionTrace)it_trace.next();
				
				int index_from_colons = ft.f_decl.ppt_decl.indexOf(":::");
				if (ft.f_decl.ppt_decl.substring(index_from_colons).equals(":::ERROR")) {
					foundErrorTrace = true;
					break;
				}
			}
			if (foundErrorTrace) {
				break;
			}
		}
		
		//Return last sequence if no error trace is found
		relevantSequence = lft;
		if (lft == null) {
			System.err.println("Error: Empty list of sequences");
			throw new Exception();
			//System.exit(-1);
		}
		else if (foundErrorTrace) {
			//Remove the "ERROR" trace(s)
			Iterator relevantSequence_it = lft.iterator();
			while (relevantSequence_it.hasNext()) {
				FunctionTrace ft = (FunctionTrace)relevantSequence_it.next();
				
				int index_from_colons = ft.f_decl.ppt_decl.indexOf(":::");
				if (ft.f_decl.ppt_decl.substring(index_from_colons).equals(":::ERROR")) {
					break;
				}
				else {
					finalFunctionTrace.add(ft);
				}
			}
		}
		else {
			finalFunctionTrace = lft;
		}
		
		if (finalFunctionTrace.isEmpty()) {
			System.err.println("Error: Empty list of sequences");
			throw new Exception();
			//System.exit(-1);
		}

		return finalFunctionTrace;
	}
	
	public static boolean sequenceIsAsync(List<FunctionTrace> ft) {
		if (ft == null) {
			return false;
		}
		
		Iterator it = ft.iterator();
		
		if (!it.hasNext()) {
			return false;
		}
		else {
			FunctionTrace firstTrace = (FunctionTrace)it.next();
			int index_from_colons = firstTrace.f_decl.ppt_decl.indexOf(":::");
			if (firstTrace.f_decl.ppt_decl.substring(index_from_colons).equals(":::ASYNC")) {
				return true;
			}
		}
		
		return false;
	}
	
	public static List<FunctionTrace> stitchSequences(List<List<FunctionTrace>> sequences, List<FunctionTrace> relevantSequence) throws Exception {
		if (sequences.isEmpty() || relevantSequence.isEmpty()) {
			System.err.println("Error: Empty list of sequences");
			throw new Exception();
			//System.exit(-1);
		}
		
		List<FunctionTrace> stitchedSeq = new ArrayList<FunctionTrace>();
		
		List<FunctionTrace> curr_sequence = relevantSequence;
		ListIterator rs_it = curr_sequence.listIterator(curr_sequence.size());
		//Iterator seq_it = sequences.iterator();
		
		while (rs_it.hasPrevious()) {
			FunctionTrace ft = (FunctionTrace)rs_it.previous();
			int index_from_colons = ft.f_decl.ppt_decl.indexOf(":::");
			String postfix = ft.f_decl.ppt_decl.substring(index_from_colons);
			
			if (!postfix.equals(":::ASYNC")) {
				stitchedSeq.add(ft);
			}
			else {
				stitchedSeq.add(ft);
				
				//Record ID
				//First, determine where RCA_timerID is found
				int rca_timerID_index = -1;
				Iterator vars = ft.f_decl.var_descs.iterator();
				int var_counter = 0;
				while (vars.hasNext()) {
					VariableDesc varDesc = (VariableDesc)vars.next();
					if (varDesc.getVarName().equals("RCA_timerID")) {
						rca_timerID_index = var_counter;
						break;
					}
					var_counter++;
				}
				
				if (rca_timerID_index == -1) {
					System.err.println("Error: Async ID missing");
					throw new Exception();
					//System.exit(-1);
				}
				int asyncID = Integer.parseInt(ft.var_values.get(rca_timerID_index));
				
				//Look through all the sequences and find corresponding async call
				List<FunctionTrace> callingSeq = null;
				Iterator seq_it = sequences.iterator();
				List<FunctionTrace> trace_seq = null;
				int async_call_index = -1;
				
				boolean async_call_found = false;
				
				while (seq_it.hasNext() && !async_call_found) {
					trace_seq = (List<FunctionTrace>)seq_it.next();
					Iterator trace_seq_it = trace_seq.iterator();
					
					int counter = 0;
					while (trace_seq_it.hasNext() && !async_call_found) {
						FunctionTrace trace = (FunctionTrace)trace_seq_it.next();
						index_from_colons = trace.f_decl.ppt_decl.indexOf(":::");
						if (trace.f_decl.ppt_decl.substring(index_from_colons).equals(":::ASYNC_CALL")) {
							//Get value of RCA_timerID, which is always at index 0 for ASYNC_CALL-type traces
							int asyncCall_ID = Integer.parseInt(trace.var_values.get(0));
							if (asyncID == asyncCall_ID) {
								async_call_found = true;
								async_call_index = counter;
								callingSeq = trace_seq;
							}
						}
						counter++;
					}
				}
				
				if (async_call_found) {
					rs_it = callingSeq.listIterator(async_call_index+1);
				}
			}
		}
		
		Collections.reverse(stitchedSeq);
		
		return stitchedSeq;
	}
	
	/**
	 * Fetches the function name corresponding to the function trace
	 * 
	 * @param ft The function trace
	 * @return The function name corresponding to ft
	 */
	private String getFunctionName(FunctionTrace ft) {
		FunctionDecl fd = ft.f_decl;
		String name = fd.f_name.substring(0, fd.f_name.indexOf(":::"));
		return name;
	}
	
	/**
	 * Fetches the line number of the current function trace relative to the corresponding function
	 * 
	 * @param ft The function trace
	 * @return The line number of the current function
	 */
	private int getFunctionLineno(FunctionTrace ft) {
		String name = ft.f_decl.f_name;
		String suffix = name.substring(name.indexOf(":::"));
		String lineno_str = "";
		if (suffix.startsWith(":::EXIT")) {
			lineno_str = suffix.substring(7);
		}
		else if (suffix.startsWith(":::INTERMEDIATE")) {
			lineno_str = suffix.substring(15);
		}
		else if (suffix.startsWith(":::ENTER")) {
			lineno_str = "1";
		}
		else if (suffix.startsWith(":::ASYNC") || suffix.startsWith(":::ASYNC_CALL")) {
			lineno_str = "0"; //we won't need the line numbers for these anyway
		}
		else {
			//System.err.println("Error: FunctionTrace suffix undefined");
			//System.exit(-1);
			return -1;
		}
		int lineno = Integer.parseInt(lineno_str);
		return lineno;
	}
	
	/**
	 * Gets the string representation of the function trace's line type
	 * 
	 * @param ft The function trace
	 * @return The line type (either ENTER, EXIT, INTERMEDIATE, ASYNC_CALL, or ASYNC)
	 */
	private String getLineType(FunctionTrace ft) throws Exception {
		String name = ft.f_decl.f_name;
		String suffix = name.substring(name.indexOf(":::"));
		String linetype = "";
		if (suffix.startsWith(":::EXIT")) {
			linetype = "EXIT";
		}
		else if (suffix.startsWith(":::INTERMEDIATE")) {
			linetype = "INTERMEDIATE";
		}
		else if (suffix.startsWith(":::ENTER")) {
			linetype = "ENTER";
		}
		else if (suffix.startsWith(":::ASYNC_CALL")) { //must be checked before :::ASYNC !!
			linetype = "ASYNC_CALL";
		}
		else if (suffix.startsWith(":::ASYNC")) {
			linetype = "ASYNC";
		}
		else {
			System.err.println("Error: Incorrect Function Line Type");
			throw new Exception();
			//System.exit(-1);
		}
		
		return linetype;
	}
	
	/**
	 * Fetches the line corresponding to the function trace
	 * <p>
	 * The JS code for a function must be saved in a file called func.js where
	 * func is the name of the JS function
	 * 
	 * @param ft The function trace
	 * @return The string containing the JS line
	 */
	private String getLine(FunctionTrace ft) throws Exception {
		String current_function = getFunctionName(ft);
		int lineno = getFunctionLineno(ft);
		String strLine = null;
		try {
			FileInputStream fstream = new FileInputStream(JS_SOURCE_FOLDER + "/" + current_function + ".js");
			DataInputStream din = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(din));
			
			String functionDeclaration = null;
			List<String> functionDeclarationTokens = null;
			int linesToReadAhead = lineno-1; //subtract 1 because this next check counts as one read
			if ((strLine = br.readLine()) != null) {
				List<String> token_stream = getTokenStream(strLine);
				Iterator token_stream_it = token_stream.iterator();
				if (token_stream_it.hasNext()) {
					String firstToken = (String)token_stream_it.next();
					if (firstToken.equals("FUNCTION")) {
						//read next line - considered line no. 1
						//if ((strLine = br.readLine()) == null) {
						//	System.err.println("Error: Empty function");
						//	System.exit(-1);
						//}
						linesToReadAhead++;
						functionDeclaration = strLine;
						functionDeclarationTokens = token_stream;
					}
				}
			}
			else {
				System.err.println("Error: Empty function");
				throw new Exception();
				//System.exit(-1);
			}
			
			for (int i = 0; i < linesToReadAhead; i++) {
				if ((strLine = br.readLine()) == null) {
					System.err.println("Error: Function does not match with FunctionTrace line number");
					throw new Exception();
					//System.exit(-1);
				}
			}
		}
		catch (Exception e) {
			System.err.println("Error reading function file");
			throw new Exception();
			//System.exit(-1);
		}
		
		/*START INJECT MODE*/
		if (injectMode && (injectType == 0)) {
			//Check if this is the injected line
			boolean sameSrc = strLine.trim().equals(injectOrigSrc.trim()) || parse(strLine).toSource().trim().equals(injectOrigSrc.trim());
			boolean sameLineNo = (lineno == injectLineNo);
			boolean sameFunction = current_function.equals(injectFuncName);
			
			if (sameSrc && sameLineNo && sameFunction) {
				strLine = injectModSrc;
			}
		}
		/*END INJECT MODE*/
		
		return strLine;
	}
	
	/**
	 * Converts a string of JS code into tokens
	 * 
	 * @param str_to_parse The JS code to convert into tokens
	 * @return A list of tokens
	 */
	private List<String> getTokenStream(String str_to_parse) {
		Parser ps = new Parser(new CompilerEnvirons());
		TokenStream ts = ps.initForUnitTest(new StringReader(str_to_parse), "", 1, false);
		List<String> tokens_list = new ArrayList<String>();
		try {
			String t_name = null;
			int token;
			
			token = ts.getToken();
			while (Token.typeToName(token) != "EOF") {
				t_name = Token.typeToName(token);
				tokens_list.add(t_name);
				if (t_name.equals("NAME") || t_name.equals("STRING")) {
					tokens_list.add(ts.getString());
				}
				token = ts.getToken();
			}
		} catch (IOException ie) {
			System.err.println("Error");
		}
		return tokens_list;
	}
	
	private boolean strIsInteger(String s) {
		try {
			Integer.parseInt(s);
			return true;
		}
		catch (NumberFormatException nfe) {
			return false;
		}
	}
	
	public List<StringSetLine> getStrSet() {
		return strSet;
	}
	
	public String getErroneousID() {
		return erroneousID;
	}
	
	public int getCurrentState() {
		return currentState;
	}
	
	//Methods for inject mode
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