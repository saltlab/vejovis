package com.fixmsg;

public class EnsureCorrectStrLiteralMessage extends FixMessage {
	private String expression;
	private String strLiteral;
	
	public EnsureCorrectStrLiteralMessage(int _lineNo, String _functionName, String _expression, String _strLiteral) {
		super(_lineNo, _functionName);
		this.expression = _expression;
		this.strLiteral = _strLiteral;
	}
	
	@Override
	public void createMessage() {
		message = "ENSURE RETURN/PROPERTY VALUE OF EXPRESSION " + 
			expression + " IN LINE " + lineNo + " OF FUNCTION " + 
			functionName + "() IS \"" + strLiteral + "\"";
	}
}