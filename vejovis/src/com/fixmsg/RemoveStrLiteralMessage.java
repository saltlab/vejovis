package com.fixmsg;

public class RemoveStrLiteralMessage extends FixMessage {
	private String strLiteral;
	
	public RemoveStrLiteralMessage(int _lineNo, String _functionName, String _strLiteral) {
		super(_lineNo, _functionName);
		this.strLiteral = _strLiteral;
	}
	
	@Override
	public void createMessage() {
		String ident;
		if (strLiteral.startsWith("\"") || strLiteral.startsWith("\'")) {
			ident = "STRING LITERAL";
		}
		else {
			ident = "EXPRESSION";
		}
		message = "REMOVE THE " + ident + " " + strLiteral +
			" IN LINE " + lineNo + " OF FUNCTION " + functionName
			+ "()";
	}
}