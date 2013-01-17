package com.fixmsg;

public class CreateNodeMessage extends FixMessage {
	private String tagName;
	private String nodeId;
	private String parentId;
	private String parentXpath;
	
	public CreateNodeMessage(int _lineNo, String _functionName, String _tagName, String _nodeId, String _parentId, String _parentXpath) {
		super(_lineNo, _functionName);
		this.tagName = _tagName;
		this.nodeId = _nodeId;
		this.parentId = _parentId;
		this.parentXpath = _parentXpath;
	}
	
	@Override
	public void createMessage() {
		String parentIdent;
		if (parentId != null) {
			parentIdent = "PARENT " + parentId;
		}
		else {
			parentIdent = "PARENT " + parentXpath;
		}
		message = "CREATE A NODE WITH TAG NAME " + tagName +
			" AND ID " + nodeId + " AND INSERT THE NODE AS A CHILD OF " +
			parentIdent + " BEFORE LINE " + lineNo + " OF FUNCTION " +
			functionName + "()";
	}
}