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

/**
 * Represents a Javascript expression and its evaluated value. Should only be used by ErrorReport
 * 
 * @author dannyroest@gmail.com (Danny Roest)
 * @version $id$
 */
public class JavascriptExpression {
	private String expression;
	private String value;

	/**
	 * @param expression
	 *            the Javascript expression
	 * @param value
	 *            the evaluated value
	 */
	protected JavascriptExpression(String expression, String value) {
		this.expression = expression;
		this.value = value;
	}

	/**
	 * @return the Javascript expression
	 */
	public String getExpression() {
		return expression;
	}

	/**
	 * @return the evaluated value
	 */
	public String getValue() {
		return value;
	}

}
