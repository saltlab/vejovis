package com.fixmsg;

public class AddStrLiteralMessage extends FixMessage {
	private String strLiteral;
	private int ddaLineNo;
	private String ddaFuncName;
	private String expr;
	
	public AddStrLiteralMessage(int _lineNo, String _functionName, String _strLiteral, int _ddaLineNo, String _ddaFuncName, String _expr) {
		super(_lineNo, _functionName);
		this.strLiteral = _strLiteral;
		this.ddaLineNo = _ddaLineNo;
		this.ddaFuncName = _ddaFuncName;
		this.expr = _expr;
	}
	
	@Override
	public void createMessage() {
		message = "ADD AN ASSIGNMENT OF STRING LITERAL \"" +
			strLiteral + "\" TO A NEW VARIABLE BEFORE LINE " +
			ddaLineNo + " OF FUNCTION " + ddaFuncName + 
			"() AND INCLUDE THE VARIABLE IN THE CONCATENATION IN LINE " +
			lineNo + " OF FUNCTION " + functionName + "() AFTER EXPRESSION "
			+ expr;
	}
}