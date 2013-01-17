/*
 * ErrorReport is a plugin for Crawljax that generates a nice HTML report to visually report the
 * failures encountered during crawling. Copyright (C) 2010 crawljax.com This program is free
 * software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version. This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received
 * a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.crawljax.plugins.errorreport;

import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.StateVertix;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an Error for the ErrorReport. This class can be used to add custom Errors to the
 * ErrorReport. For reading the properties use ReportErrorReader
 * 
 * @author dannyroest@gmail.com (Danny Roest)
 * @version $id$
 */
public class ReportError {

	private final String typeDescription;
	private final String description;
	private int id;
	private List<JavascriptExpression> javascriptExpressions =
	        new ArrayList<JavascriptExpression>();

	private boolean includeScreenshots = true;
	private StateVertix originalState;
	private String currentDom;
	private List<Eventable> pathToFailure = new ArrayList<Eventable>();
	private List<Highlight> highlights = new ArrayList<Highlight>();
	private String originalScreenShotId;

	/**
	 * @param typeDescription
	 *            the type of the error. In the report the errors are categorized by this
	 *            typeDescription
	 * @param description
	 *            the description of the error
	 */
	public ReportError(String typeDescription, String description) {
		this.typeDescription = typeDescription;
		this.description = description;
	}

	/**
	 * @return a ReportError without screenshots
	 */
	public ReportError dontIncludeScreenshots() {
		this.includeScreenshots = false;
		return this;
	}

	/**
	 * @param pathToFailure
	 *            the Eventable path that lead to the failure
	 * @return a ReportError with the pathToFailure
	 */
	public ReportError withPathToFailure(List<Eventable> pathToFailure) {
		this.pathToFailure = pathToFailure;
		return this;
	}

	/**
	 * @param highlights
	 *            the highlighted elements
	 * @return a ReportError with the specified highlights
	 */
	public ReportError withHighlights(List<Highlight> highlights) {
		this.highlights = highlights;
		return this;
	}

	/**
	 * Use this current dom in stead of the browser's dom. This can be useful when the state should
	 * be modified. For example to the stripped DOM by the Oracle Comparators.
	 * 
	 * @param currentDom
	 *            the currentState which is used in state of the current browser's dom
	 * @return a ReportError with the specified current state
	 */
	public ReportError useDomInSteadOfBrowserDom(String currentDom) {
		this.currentDom = currentDom;
		return this;
	}

	/**
	 * @param originalState
	 *            the original state which is related to the error
	 * @return a ReportError with the specified original state
	 */
	public ReportError includeOriginalState(StateVertix originalState) {
		this.originalState = originalState;
		return this;
	}

	/**
	 * @return the Eventable path to the failure
	 */
	public List<Eventable> getPathToFailure() {
		return pathToFailure;
	}

	/**
	 * @return whether there should be screenshots taken of the current state
	 */
	public boolean includeScreenshots() {
		return includeScreenshots;
	}

	/**
	 * @return the highlights
	 */
	public List<Highlight> getHighlights() {
		return highlights;
	}

	/**
	 * @return the original state
	 */
	public StateVertix getOriginalState() {
		return originalState;
	}

	/**
	 * @return the original state related to this Error
	 */
	public boolean hasOriginalState() {
		return originalState != null;
	}

	/**
	 * @return a description of this error
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * @return the type of error
	 */
	public String getTypeDescription() {
		return this.typeDescription;
	}

	/**
	 * @return a unique id of the error (set by ErrorReport)
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            a unique id of the error (should only be used by ErrorReport)
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the evaluated Javascript expressions
	 */
	public List<JavascriptExpression> getJavascriptExpressions() {
		return javascriptExpressions;
	}

	/**
	 * @param javascriptExpressions
	 *            the evaluated Javascript expressions
	 */
	public void setJavascriptExpressions(List<JavascriptExpression> javascriptExpressions) {
		this.javascriptExpressions = javascriptExpressions;
	}

	/**
	 * @return the current DOM
	 */
	public String getCurrentDom() {
		return currentDom;
	}

	/**
	 * @param name
	 */
	public void setOriginalScreenShotId(String name) {
		this.originalScreenShotId = name;
	}

	/**
	 * @return the originalScreenShotId
	 */
	public String getOriginalScreenShotId() {
		return originalScreenShotId;
	}
	
	/**
	 * @return whether there are screenshots taken of the original state
	 */
	public boolean includeOriginalScreenshots() {
		return originalScreenShotId != null && !originalScreenShotId.equals("");
	}
}
