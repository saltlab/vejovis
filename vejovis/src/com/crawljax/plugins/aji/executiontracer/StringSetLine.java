package com.crawljax.plugins.aji.executiontracer;

import com.rca.*;
import java.util.*;

public class StringSetLine {
	private int lineNo;
	private String varIdentifier;
	private String strLiteral; //null if not of type string literal
	private int type; //0 - StringSetLine list, 1 - gap, 2 - string literal
	private List<StringSetLine> ssll; //initially null if type 
	private int seqIndex; //index in the FunctionTrace sequence
	private String funcName;
	
	public List<String> possibleStrs; //only used when gap filling
	
	//Use the following members to indicate that a gap
	//may be due to function call with missing parameters
	private boolean hasPossiblyBadFuncCall;
	private int funcCallLineNo;
	private String funcCallfname;
	private int funcCallparamNum;
	
	public enum Type {
		STRING_SET_LINE_LIST,
		GAP,
		STRING_LITERAL
	};
	
	public StringSetLine(int _lineNo, String _varId, String _strLiteral, int _type, List<StringSetLine> _ssll, int _seqIndex, String _funcName) {
		this.lineNo = _lineNo;
		this.varIdentifier = _varId;
		this.strLiteral = _strLiteral;
		this.type = _type;
		this.ssll = _ssll;
		this.seqIndex = _seqIndex;
		this.funcName = _funcName;
		
		this.possibleStrs = new ArrayList<String>();
		
		hasPossiblyBadFuncCall = false;
		funcCallLineNo = 0;
		funcCallfname = "";
		funcCallparamNum = 0;
	}
	
	public int getLineNo() {
		return lineNo;
	}
	
	public String getVarIdent() {
		return varIdentifier;
	}
	
	public String getStrLiteral() {
		return strLiteral;
	}
	
	public int getType() {
		return type;
	}
	
	public List<StringSetLine> getSSLL() {
		return ssll;
	}
	
	public int getSeqIndex() {
		return seqIndex;
	}
	
	public String getFuncName() {
		return funcName;
	}
	
	public void setType(int _type) {
		this.type = _type;
	}
	
	public void setStrLiteral(String _strLiteral) {
		this.strLiteral = _strLiteral;
	}
	
	public void setFuncName(String _funcName) {
		this.funcName = _funcName;
	}
	
	public void setHasPossiblyBadFuncCall(int lineNo, String fname, int paramNum) {
		this.hasPossiblyBadFuncCall = true;
		this.funcCallLineNo = lineNo;
		this.funcCallfname = fname;
		this.funcCallparamNum = paramNum;
	}
	
	public boolean gapHasPossiblyBadFuncCall() {
		return hasPossiblyBadFuncCall;
	}
	
	public int getFuncCallLineNo() {
		return funcCallLineNo;
	}
	
	public String getFuncCallfname() {
		return funcCallfname;
	}
	
	public int getFuncCallparamNum() {
		return funcCallparamNum;
	}
}