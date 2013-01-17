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

import java.util.ArrayList;
import java.util.List;

/**
 * Class that contains a type of error which is used by ErrorReport for the Velocity template.
 * 
 * @author dannyroest@gmail.com (Danny Roest)
 * @version $id$
 */
public class ReportErrorList {

	private String type;
	private List<ReportErrorReader> reportErrors = new ArrayList<ReportErrorReader>();

	/**
	 * @param type
	 *            the type of the errors
	 */
	public ReportErrorList(String type) {
		super();
		this.type = type;
	}

	/**
	 * @return the type of the errors
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the reportErrors
	 */
	public List<ReportErrorReader> getReportErrors() {
		return reportErrors;
	}

	/**
	 * @param reportError
	 *            the reportError to add
	 */
	public void addReportError(ReportError reportError) {
		this.reportErrors.add(new ReportErrorReader(reportError));
	}

}
