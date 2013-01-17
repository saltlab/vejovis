package com.fixmsg;

public class PassParamMessage extends FixMessage {
	private int paramNum;
	private String funcToCall;
	private String strVal;
	
	public PassParamMessage(int _lineNo, String _functionName, int _paramNum, String _funcToCall, String _strVal) {
		super(_lineNo, _functionName);
		this.paramNum = _paramNum;
		this.funcToCall = _funcToCall;
		this.strVal = _strVal;
	}
	
	@Override
	public void createMessage() {
		String funcName = "FUNCTION " + functionName + "()";
		if (functionName.startsWith("root")) {
			funcName = "THE JAVASCRIPT CODE";
		}
		
		if (paramNum > 0) {
			int lastDigit = (paramNum % 10);
			String numSuffix = "";
			switch (lastDigit) {
				case 1:
					numSuffix = "ST";
					break;
				case 2:
					numSuffix = "ND";
					break;
				case 3:
					numSuffix = "RD";
					break;
				default:
					numSuffix = "TH";
					break;
			}
			
			message = "MAKE SURE THE " + paramNum + numSuffix + 
				" PARAMETER OF THE CALL TO " + funcToCall + 
				"() IN LINE " + lineNo + " OF " +
				funcName + " HAS VALUE \"" + strVal + "\"";
		}
		else {
			message = "MAKE SURE THE PARAMETER OF THE CALL TO " + 
			funcToCall + "() IN LINE " + lineNo + " OF " +
			funcName + " HAS VALUE \"" + strVal + "\"";
		}
	}
}