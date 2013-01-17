package com.fixmsg;

public class ModifyStrLiteralMessage extends FixMessage {
	private String oldStrLiteral;
	private String newStrLiteral;
	
	public ModifyStrLiteralMessage(int _lineNo, String _functionName, String _oldStrLiteral, String _newStrLiteral) {
		super(_lineNo, _functionName);
		this.oldStrLiteral = _oldStrLiteral;
		this.newStrLiteral = _newStrLiteral;
	}
	
	@Override
	public void createMessage() {
		message = "MODIFY THE STRING LITERAL IN LINE " + 
			lineNo + " OF FUNCTION " + functionName + "() FROM \"" 
			+ oldStrLiteral + "\" TO \"" + newStrLiteral + "\"";
	}
}