package com.exceptions;

import com.rca.*;

public class UnsuccessfulEnterException extends Exception {
	private FunctionTrace possibleBadFuncCall;
	private int paramNum;
	private String funcToCall;
	
	public UnsuccessfulEnterException(FunctionTrace _ft, int _paramNum, String _funcToCall) {
		this.possibleBadFuncCall = _ft;
		this.paramNum = _paramNum;
		this.funcToCall = _funcToCall;
	}
	
	public FunctionTrace getPossibleBadFuncCall() {
		return possibleBadFuncCall;
	}
	
	public int getParamNum() {
		return paramNum;
	}
	
	public String getFuncToCall() {
		return funcToCall;
	}
}