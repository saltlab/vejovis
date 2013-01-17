package com.crawljax.plugins.aji.executiontracer;

import com.fixmsg.*;

public class CandidateFix {
	private FixMessage message;
	private int rankValue;
	private FixClasses fixClass;
	
	public CandidateFix(FixMessage _message, FixClasses _fixClass) {
		this.message = _message;
		this.fixClass = _fixClass;
		this.rankValue = 0;
	}
	
	public FixMessage getMessage() {
		return message;
	}
	
	public String getMessageStr() {
		return message.getMsg();
	}
	
	public int getRankValue() {
		return rankValue;
	}
	
	public FixClasses getFixClass() {
		return fixClass;
	}
	
	public void setRankValue(int _rankValue) {
		this.rankValue = _rankValue;
	}
}