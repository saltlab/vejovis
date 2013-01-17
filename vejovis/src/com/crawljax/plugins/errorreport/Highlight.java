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
 * Class that defines elements that should be highlighted. Used for highlighting elements in the
 * ErrorReport
 * 
 * @author dannyroest@gmail.com (Danny Roest)
 * @version $id$
 */
public class Highlight {

	private String id;
	private final String description;
	private final String xpathCurrentDom;
	private String xpathOriginalDom;
	private String color;

	/**
	 * Creates a highlight for the current DOM.
	 * 
	 * @param description
	 *            a description why the element is highlighted
	 * @param xpathCurrentDom
	 *            the xpath expression of the corresponding element in the current DOM
	 */
	public Highlight(String description, String xpathCurrentDom) {
		this.description = description;
		this.xpathCurrentDom = xpathCurrentDom;
	}

	/**
	 * Creates a highlight for the current and the original DOM.
	 * 
	 * @param description
	 *            a description why the element is highlighted
	 * @param xpathCurrentDom
	 *            the xpath expression of the corresponding element in the current DOM
	 * @param xpathOriginalDom
	 *            the xpath expression of the corresponding element in the original DOM
	 */
	public Highlight(String description, String xpathCurrentDom, String xpathOriginalDom) {
		this(description, xpathCurrentDom);
		this.xpathOriginalDom = xpathCurrentDom;
	}

	/**
	 * @return the description why the element is highlighted
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the xpath expression of the corresponding element in the current DOM
	 */
	public String getXpathCurrentDom() {
		return xpathCurrentDom;
	}

	/**
	 * @return the xpath expression of the corresponding element in the original DOM
	 */
	public String getXpathOriginalDom() {
		return xpathOriginalDom;
	}

	/**
	 * @return the id of the highlight (set by the ErrorReport)
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            sets a unique id (should only be used by ErrorReport)
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the assigned HTML color of the highlight by the ErrorReport
	 */
	public String getColor() {
		return color;
	}

	/**
	 * @param color
	 *            sets a HTML color (should only be used by ErrorReport)
	 */
	public void setColor(String color) {
		this.color = color;
	}

}
