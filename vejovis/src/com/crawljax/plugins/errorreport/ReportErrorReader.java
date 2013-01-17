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

import org.apache.commons.lang.StringEscapeUtils;

import java.util.List;

/**
 * Reader class for ReportError which is used for the Velocity template.
 * 
 * @author dannyroest@gmail.com (Danny Roest)
 * @version $id$
 */
public class ReportErrorReader {

	private final ReportError reportError;

	/**
	 * @param reportError
	 *            the ReportError to read from
	 */
	public ReportErrorReader(ReportError reportError) {
		this.reportError = reportError;
	}

	/**
	 * @return the Eventable path to the failure
	 */
	public List<Eventable> getPathToFailure() {
		return this.reportError.getPathToFailure();
	}

	/**
	 * @return whether screenshots are taken of the state at the moment of failure
	 */
	public boolean includeScreenshots() {
		return this.reportError.includeScreenshots();
	}
	
	/**
	 * @return the originalScreenShotId
	 */
	public String getOriginalScreenShotId() {
		return this.reportError.getOriginalScreenShotId();
	}

	/**
	 * @return whether there are screenshots taken of the original state
	 */
	public boolean includeOriginalScreenshots() {
		return this.reportError.getOriginalScreenShotId() != null
		        && !this.reportError.getOriginalScreenShotId().equals("");
	}

	/**
	 * @return the highlights
	 */
	public List<Highlight> getHighlights() {
		return this.reportError.getHighlights();
	}

	/**
	 * @return the original state related to the error
	 */
	public StateVertix getOriginalState() {
		return this.reportError.getOriginalState();
	}

	/**
	 * @return whether the error has a related original state
	 */
	public boolean hasOriginalState() {
		return getOriginalState() != null;
	}

	/**
	 * @return the description of the error
	 */
	public String getDescription() {
		return StringEscapeUtils.escapeHtml(this.reportError.getDescription());
	}

	/**
	 * @return the type of the error
	 */
	public String getTypeDescription() {
		return StringEscapeUtils.escapeHtml(this.reportError.getTypeDescription());
	}

	/**
	 * @return unique id of the error
	 */
	public int getId() {
		return this.reportError.getId();
	}

	/**
	 * @return the evaluated Javascript expressions
	 */
	public List<JavascriptExpression> getJavascriptExpressions() {
		return this.reportError.getJavascriptExpressions();
	}

}
