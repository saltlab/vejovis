package com.fixmsg;

public abstract class FixMessage {
	protected int lineNo;
	protected String functionName;
	
	protected String message;
	
	protected FixMessage(int _lineNo, String _functionName) {
		this.lineNo = _lineNo;
		this.functionName = _functionName;
		this.message = "";
	}
	
	/**
	 * Implement this function to print corresponding fix
	 * suggestion message
	 */
	protected abstract void createMessage();
	
	public String getMsg() {
		return message;
	}
}