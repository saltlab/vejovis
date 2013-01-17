package com.crawljax.plugins.aji.executiontracer;

import java.util.*;

public class IDTreeNode {
	private DomIdInfo id;
	private boolean erroneousID;
	private List<IDTreeNode> children;
	private IDTreeNode parent;
	
	public IDTreeNode(DomIdInfo _id, boolean _erroneousID, IDTreeNode _parent) {
		this.id = _id;
		this.erroneousID = _erroneousID;
		this.children = new ArrayList<IDTreeNode>();
		this.parent = _parent;
	}
	
	public DomIdInfo getID() {
		return id;
	}
	
	public boolean isErroneousID() {
		return erroneousID;
	}
	
	public List<IDTreeNode> getChildren() {
		return children;
	}
	
	public IDTreeNode getParent() {
		return parent;
	}
	
	public List<IDTreeNode> getSiblings() {
		List<IDTreeNode> siblings = new ArrayList<IDTreeNode>();
		if (parent == null) {
			return siblings; //empty list
		}
		
		//Get all the children of parent (apart from this node)
		List<IDTreeNode> parentsChildren = parent.getChildren();
		for (int i = 0; i < parentsChildren.size(); i++) {
			IDTreeNode parentsChild = parentsChildren.get(i);
			if (!parentsChild.equals(this)) {
				siblings.add(parentsChild);
			}
		}
		
		return siblings;
	}
	
	public List<IDTreeNode> getClosestSiblings() {
		List<IDTreeNode> siblings = new ArrayList<IDTreeNode>();
		if (parent == null) {
			return siblings; //empty list
		}
		
		//Get the children to the left and right of this node
		//If this node is the only child, then return empty list
		//If this node is at the very left, return the two closest siblings to its right (one if there's only one other)
		//If this node is at the very right, return the two closest siblings to its left (one if there's only one other)
		List<IDTreeNode> parentsChildren = parent.getChildren();
		if (parentsChildren.size() == 0 || parentsChildren.size() == 1) {
			return siblings;
		}
		
		//Determine the position of this node
		int nodePosition = -1;
		for (int i = 0; i < parentsChildren.size(); i++) {
			IDTreeNode parentsChild = parentsChildren.get(i);
			if (parentsChild.equals(this)) {
				nodePosition = i;
				break;
			}
		}
		
		if (nodePosition == -1) {
			//Something wrong, so just return empty list
			return siblings;
		}
		
		if (nodePosition == 0) { //Very left
			siblings.add(parentsChildren.get(nodePosition+1));
			if (parentsChildren.size() > 2) {
				siblings.add(parentsChildren.get(nodePosition+2));
			}
			return siblings;
		}
		
		if (nodePosition == parentsChildren.size()-1) { //Very right
			siblings.add(parentsChildren.get(nodePosition-1));
			if (parentsChildren.size() > 2) {
				siblings.add(parentsChildren.get(nodePosition-2));
			}
			return siblings;
		}
		
		//If this point is reached, then nodePosition must be somewhere in between
		siblings.add(parentsChildren.get(nodePosition-1));
		siblings.add(parentsChildren.get(nodePosition+1));
		
		return siblings;
	}
}