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

import com.google.common.collect.Lists;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.condition.invariant.Invariant;
import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawljaxException;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.plugin.PostCrawlingPlugin;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.StateVertix;
import com.crawljax.util.Helper;
import com.crawljax.util.PrettyHTML;
import com.crawljax.util.XPathHelper;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.custommonkey.xmlunit.Difference;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates a html report which can be used for error reporting. This report contains descriptions of
 * the failures, and can be used to inspect the failures via screenshots, dom trees, and Javascript
 * expressions. It can be used as an {@link PostCrawlingPlugin} or a {@link OnNewStatePlugin}. In
 * the {@link OnNewStatePlugin} phase it collects screenshots of every state it sees. In the
 * {@link PostCrawlingPlugin} phase it genereates the report.
 *
 * @author dannyroest@gmail.com (Danny Roest)
 * @author a.mesbah
 * @version $id$
 */
public class ErrorReport implements PostCrawlingPlugin, OnNewStatePlugin {
	private static final Logger LOGGER = Logger.getLogger(ErrorReport.class);

	private static final String MAIN_HTML = "index.html";
	private static final String MAIN_FOLDER = "errorreport/";
	private static final String DATA_FOLDER = "data/";
	private static final String SCREENSHOTS_FOLDER = "img/";
	private static final String NEW_SCREENSHOTS_FOLDER = "new/";
	private static final String ORIGIONAL_SCREENSHOTS_FOLDER = "orig/";
	private static final String GENERAL_JS = "general.js";
	private static final String JQUERY_JS = "jquery.js";
	private static final String STYLE_CSS = "style.css";

	public static final String[] HIGHLIGHT_COLORS =
	        { "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#00FFFF", "#FF00FF", "#C0C0C0",
	                "#FF6600", "#99FF00", "#663300", "#336600", "#6633CC", "#FF99FF",
	                "#FF0066", };

	private final File report;
	private final String outputFolder;
	private final String newScreenshotsFolder;
	private final String originalScreenshotsFolder;
	private final String statesFolder;

	private final String title;

	private final Map<String, ReportErrorList> reportErrors =
	        new HashMap<String, ReportErrorList>();

	private final List<String> javascriptExpressions = new ArrayList<String>();

	// counter for unique id error
	private final AtomicInteger indexError = new AtomicInteger(1);

	private final List<String> filterAttributes;
	
	private final List<String> originalScreenShotsTaken = Lists.newArrayList();

	private final boolean includeScreenShots;
	
	/**
	 * Creates a new ErrorReport objects and created the needed folders.
	 *
	 * @param title
	 *            the title of the report and is also used as folder name
	 */
	public ErrorReport(String title) {
		this(title, MAIN_FOLDER);
	}

	/**
	 * Creates a new ErrorReport objects and created the needed folders.
	 *
	 * @param title
	 *            the title of the report and is also used as folder name
	 * @param outputFolderName
	 *            folder to use for output
	 */
	public ErrorReport(String title, String outputFolderName) {
		this(title, outputFolderName, Lists.<String> newArrayList());
	}
	
	/**
	 * Creates a new ErrorReport objects and created the needed folders.
	 *
	 * @param title
	 *            the title of the report and is also used as folder name
	 * @param outputFolderName
	 *            folder to use for output
	 * @param filterAttributes
	 *            the dom element attributes to exclude during generation of the Report.
	 */
	public ErrorReport(String title, String outputFolderName, List<String> filterAttributes) {
		this(title, outputFolderName, filterAttributes, true);
	}

	/**
	 * Creates a new ErrorReport objects and created the needed folders.
	 *
	 * @param title
	 *            the title of the report and is also used as folder name
	 * @param outputFolderName
	 *            folder to use for output
	 * @param filterAttributes
	 *            the dom element attributes to exclude during generation of the Report.
	 * @param includeScreenShots
	 *            are screenshots included in this ErrorReport?
	 */
	public ErrorReport(String title, String outputFolderName, List<String> filterAttributes,
	        boolean includeScreenShots) {
		this.title = title;
		this.filterAttributes = filterAttributes;
		this.includeScreenShots = includeScreenShots;
		if (outputFolderName == null) {
			this.outputFolder = title;
		} else {
			this.outputFolder = Helper.addFolderSlashIfNeeded(outputFolderName) + title;
		}

		this.newScreenshotsFolder =
		        outputFolder + "/" + SCREENSHOTS_FOLDER + NEW_SCREENSHOTS_FOLDER;
		this.originalScreenshotsFolder =
		        outputFolder + "/" + SCREENSHOTS_FOLDER + ORIGIONAL_SCREENSHOTS_FOLDER;
		this.statesFolder = outputFolder + "/" + DATA_FOLDER;

		this.report = new File(outputFolder + "/" + MAIN_HTML);
		generateNeededFilesAndFolders();
	}

	/**
	 * Generates the report.
	 *
	 * @throws IOException
	 *             when cannot generates the report
	 */
	public void generate() throws IOException {
		String template = Helper.getTemplateAsString("errorreport.vm");
		VelocityContext context = new VelocityContext();

		context.put("title", this.title);
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		context.put("date", sdf.format(cal.getTime()));

		List<ReportErrorList> reportErrorLists =
		        new ArrayList<ReportErrorList>(reportErrors.values());

		context.put("reportErrorLists", reportErrorLists);

		FileWriter writer = new FileWriter(report);
		VelocityEngine ve = new VelocityEngine();
		// disable logging
		ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS,
		        "org.apache.velocity.runtime.log.NullLogChute");
		ve.evaluate(context, writer, this.title, template);
		writer.flush();
		writer.close();
		LOGGER.info("Report written to: " + report.getAbsolutePath());
	}

	/**
	 * Adds the violated invariant to the report.
	 *
	 * @param invariant
	 *            the violated invariant
	 * @param browser
	 *            the browser used
	 */
	public void addInvariantViolation(Invariant invariant, EmbeddedBrowser browser) {
		addInvariantViolation(invariant, new ArrayList<Eventable>(), browser);
	}

	/**
	 * Adds the violated invariant to the report.
	 *
	 * @param invariant
	 *            the violated invariant
	 * @param pathToFailure
	 *            the fired Eventables before the violation
	 * @param browser
	 *            the browser used
	 */
	public void addInvariantViolation(
	        Invariant invariant, List<Eventable> pathToFailure, EmbeddedBrowser browser) {
			addReportError(new ReportError("Invariant Violations", invariant.getDescription())
			        .withPathToFailure(pathToFailure), browser);
	}

	/**
	 * Adds the Eventable that could not be fired to the report.
	 *
	 * @param eventable
	 *            the Eventable that could not be fired
	 * @param browser
	 *            the browser used
	 */
	public void addEventFailure(Eventable eventable, EmbeddedBrowser browser) {
		addEventFailure(eventable, null, new ArrayList<Eventable>(), browser);
	}

	/**
	 * Adds the Eventable that could not be fired to the report.
	 *
	 * @param eventable
	 *            the Eventable that could not be fired
	 * @param pathToFailure
	 *            the fired Eventables before the failure
	 * @param browser
	 *            the browser used
	 */
	public void addEventFailure(
	        Eventable eventable, List<Eventable> pathToFailure, EmbeddedBrowser browser) {
		addEventFailure(eventable, null, pathToFailure, browser);
	}

	/**
	 * Adds the Eventable that could not be fired to the report.
	 *
	 * @param eventable
	 *            the Eventable that could not be fired
	 * @param originalState
	 *            the state in which the Eventable was originally fired
	 * @param browser
	 *            the browser used
	 */
	public void addEventFailure(
	        Eventable eventable, StateVertix originalState, EmbeddedBrowser browser) {
		addEventFailure(eventable, originalState, new ArrayList<Eventable>(), browser);
	}

	/**
	 * Adds the Eventable that could not be fired to the report.
	 *
	 * @param eventable
	 *            the Eventable that could not be fired
	 * @param originalState
	 *            the state in which the Eventable was originally fired
	 * @param pathToFailure
	 *            the fired Eventables before the failure
	 * @param browser
	 *            the browser used
	 */
	public void addEventFailure(Eventable eventable, StateVertix originalState,
	        List<Eventable> pathToFailure, EmbeddedBrowser browser) {
		List<Highlight> highlights = new ArrayList<Highlight>();
		highlights.add(
		        new Highlight("Could not fire event: " + eventable.toString(),
		                eventable.getIdentification().getValue()));
		addReportError(
		        new ReportError("Event Failures", eventable.getElement().getText() + " "
		                + eventable.getElement().getAttributes()).withPathToFailure(pathToFailure)
		                .withHighlights(highlights).includeOriginalState(originalState), browser);
	}

	/**
	 * Adds a state failure which is a difference between two states.
	 *
	 * @param originalState
	 *            the state to compare with
	 * @param browser
	 *            the browser used
	 */
	public void addStateFailure(StateVertix originalState, EmbeddedBrowser browser) {
		addStateFailure(browser.getDom(), originalState, browser);
	}

	/**
	 * Adds a state failure which is a difference between two states.
	 *
	 * @param currentDom
	 *            the current DOM which is used instead Browser.getDom()
	 * @param originalState
	 *            the state to compare with
	 * @param browser
	 *            the browser used
	 */
	public void addStateFailure(
	        String currentDom, StateVertix originalState, EmbeddedBrowser browser) {
		addStateFailure(currentDom, originalState, new ArrayList<Eventable>(), browser);
	}
	
	/**
	 * Adds a state failure which is a difference between two states.
	 *
	 * @param currentDom
	 *            the current DOM which is used instead Browser.getDom()
	 * @param originalState
	 *            the state to compare with
	 * @param pathToFailure
	 *            the fired Eventables before the failure
	 * @param browser
	 *            the browser used
	 */
	public void addStateFailure(String currentDom, StateVertix originalState,
	        List<Eventable> pathToFailure, EmbeddedBrowser browser) {

		List<Difference> differences =
		        Helper.getDifferences(currentDom, originalState.getDom(), filterAttributes);
		List<Highlight> highlights = new ArrayList<Highlight>();
		for (Difference difference : differences) {
			highlights.add(new Highlight(StringEscapeUtils.escapeHtml(difference.toString()),
			        difference.getTestNodeDetail().getXpathLocation(),
			        difference.getControlNodeDetail().getXpathLocation()));
		}
		addReportError(new ReportError("State Differences",
		        originalState.getName() + " (" + differences.size() + ")")
		        .withPathToFailure(pathToFailure).withHighlights(highlights)
		        .includeOriginalState(originalState).useDomInSteadOfBrowserDom(currentDom),
		        browser);

	}

	/**
	 * Adds a reportError to the ErrorReport.
	 *
	 * @param reportError
	 *            the reportError containing the information about the failure
	 * @param browser
	 *            the browser used
	 */
	public void addFailure(ReportError reportError, EmbeddedBrowser browser) {
			addReportError(reportError, browser);
	}

	/**
	 * Adds Javascript expressions and its evaluated values to the report. Example: document.tittle
	 *
	 * @param javascriptExpressions
	 *            the Javascript expressions to evaluate when there is a failure added to the
	 *            ErrorReport. A toString representation of the result is shown in the report.
	 */
	public void setJavascriptExpressions(List<String> javascriptExpressions) {
		this.javascriptExpressions.addAll(javascriptExpressions);
	}

	/**
	 * @param javascriptExpressions
	 *            the Javascript expressions to evaluate when there is a failure added to the
	 *            ErrorReport
	 */
	public void setJavascriptExpressions(String... javascriptExpressions) {
		this.javascriptExpressions.addAll(Arrays.asList(javascriptExpressions));
	}

	private void addReportError(ReportError error, EmbeddedBrowser browser) {
		error.setId(indexError.getAndIncrement());
		if (!includeScreenShots) {
			error.dontIncludeScreenshots();
		}
		if (error.getCurrentDom() == null) {
			error = error.useDomInSteadOfBrowserDom(browser.getDom());
		}
		// TODO Stefan; REFACTOR!! This looks so ugly it must contain bugs ;)
		if (!reportErrors.containsKey(error.getTypeDescription())) {
			reportErrors.put(
			        error.getTypeDescription(), new ReportErrorList(error.getTypeDescription()));
		}
		reportErrors.get(error.getTypeDescription()).addReportError(error);
		processReportError(error, browser);
	}

	private void setHighlights(ReportError error) {
		int i = 0;
		for (Highlight highlight : error.getHighlights()) {
			highlight.setId(error.getId() + "_" + i);
			highlight.setColor(HIGHLIGHT_COLORS[i % HIGHLIGHT_COLORS.length]);
			i++;
		}
	}

	private void processReportError(ReportError error, EmbeddedBrowser browser) {
		setHighlights(error);
		saveDOMs(error);
		try {
	        saveJavascriptExpressions(error, browser);
	        if (error.includeScreenshots()) {
	        	// The "New" screenshot
	        	List<HighlightedElement> highlightedElements =
	        	        addHighlightsInBrowser(error.getHighlights(), browser);
	        	makeScreenShot(browser, String.valueOf(error.getId()), this.newScreenshotsFolder);
	        	removeHighlightedElement(highlightedElements, browser);
	        	
	        	if (error.hasOriginalState()) {
					if (originalScreenShotsTaken.contains(error.getOriginalState().getName())) {
						error.setOriginalScreenShotId(error.getOriginalState().getName());
					}
				}
	        }
        } catch (CrawljaxException e) {
	        LOGGER.error("Catched Exception when setting highlights", e);
        }
	}

	private void saveJavascriptExpressions(ReportError error, EmbeddedBrowser browser)
	        throws CrawljaxException {
		List<JavascriptExpression> evaluatedJavascriptExpressions =
		        new ArrayList<JavascriptExpression>();
		for (String expression : this.javascriptExpressions) {
			String js = "try{ return " + expression + "; }catch(e){}";
			Object result = browser.executeJavaScript(js);
			String value;
			if (result != null) {
				value = result.toString();
			} else {
				value = "null";
			}
			evaluatedJavascriptExpressions.add(new JavascriptExpression(expression, value));
		}
		error.setJavascriptExpressions(evaluatedJavascriptExpressions);
	}

	/**
	 * Take a screenshot.
	 *
	 * @param browser
	 *            the browser at which in the current state the screenshot must be taken.
	 * @param id
	 *            the id of the file.
	 * @param dir
	 *            the directory where the screenshot must be stored.
	 * @throws CrawljaxException
	 *             when screenshoting fails.
	 */
	private void makeScreenShot(EmbeddedBrowser browser, String id, String dir)
	        throws CrawljaxException {
		String filename = "screenshot_" + id + ".png";
		File screenShot = new File(dir, filename);
		try {
			browser.saveScreenShot(screenShot);
		} catch (Exception e) {
			throw new CrawljaxException(e);
		}
	}

	private void saveDOMs(ReportError error) {
		try {
			saveDOM(error, "current", error.getCurrentDom(), error.getHighlights());
			if (error.getOriginalState() != null) {
				saveDOM(error, "original", error.getOriginalState().getDom(),
				        error.getHighlights());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void saveDOM(ReportError error, String suffix, String dom, List<Highlight> highlights)
	        throws SAXException, IOException {
		Document doc = Helper.getDocument(dom);
		for (Highlight highlight : highlights) {
			doc = addMarker(highlight.getId(), doc, highlight.getXpathCurrentDom());
		}
		String formattedDom = Helper.getDocumentToString(doc);
		formattedDom = PrettyHTML.prettyHTML(formattedDom, "  ");
		formattedDom = StringEscapeUtils.escapeHtml(formattedDom);
		formattedDom = replaceMarkers(formattedDom, highlights, suffix);
		String filename = "dom_" + error.getId() + "_" + suffix + ".txt";
		File stateFile = new File(this.statesFolder + filename);
		try {
			FileWriter writer = new FileWriter(stateFile, false);
			writer.write(formattedDom);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void generateNeededFilesAndFolders() {
		try {
			Helper.directoryCheck(this.newScreenshotsFolder);
			Helper.directoryCheck(this.originalScreenshotsFolder);
			Helper.directoryCheck(this.statesFolder);

			FileWriter out = new FileWriter(new File(outputFolder + "/" + GENERAL_JS));
			out.write(Helper.getTemplateAsString(GENERAL_JS));
			out.close();

			out = new FileWriter(new File(outputFolder + "/" + JQUERY_JS));
			out.write(Helper.getTemplateAsString(JQUERY_JS));
			out.close();

			out = new FileWriter(new File(outputFolder + "/" + STYLE_CSS));
			out.write(Helper.getTemplateAsString(STYLE_CSS));
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Document addMarker(String id, Document doc, String xpath) {
		try {

			String prefixMarker = "###BEGINMARKER" + id + "###";
			String suffixMarker = "###ENDMARKER###";

			NodeList nodeList = XPathHelper.evaluateXpathExpression(doc, xpath);

			if (nodeList.getLength() == 0 || nodeList.item(0) == null) {
				return doc;
			}
			Node element = nodeList.item(0);

			if (element.getNodeType() == Node.ELEMENT_NODE) {
				Node beginNode = doc.createTextNode(prefixMarker);
				Node endNode = doc.createTextNode(suffixMarker);

				element.getParentNode().insertBefore(beginNode, element);
				if (element.getNextSibling() == null) {
					element.getParentNode().appendChild(endNode);
				} else {
					element.getParentNode().insertBefore(endNode, element.getNextSibling());
				}
			} else if (element.getNodeType() == Node.TEXT_NODE
			        && element.getTextContent() != null) {
				element.setTextContent(prefixMarker + element.getTextContent() + suffixMarker);
			} else if (element.getNodeType() == Node.ATTRIBUTE_NODE) {
				element.setNodeValue(prefixMarker + element.getTextContent() + suffixMarker);
			}

			return doc;
		} catch (Exception e) {
			return doc;
		}
	}

	private String replaceMarkers(String dom, List<Highlight> highlights, String suffix) {
		for (Highlight highlight : highlights) {
			String regexSearch = "\\s*" + "###BEGINMARKER" + highlight.getId() + "###";
			String replace =
			        "<div id='" + highlight.getId() + "_" + suffix
			                + "' style='display: inline; background-color: "
			                + highlight.getColor() + ";'>";
			dom = dom.replaceAll(regexSearch, replace);
		}
		dom = dom.replaceAll("(\\s)*###ENDMARKER###", "</div>");
		return dom;
	}

	private List<HighlightedElement> addHighlightsInBrowser(
	        List<Highlight> highlights, EmbeddedBrowser browser) throws CrawljaxException {
		List<HighlightedElement> highlightedElements = new ArrayList<HighlightedElement>();
		for (Highlight highlight : highlights) {
			String xpath = XPathHelper.stripXPathToElement(highlight.getXpathCurrentDom());

			if (xpath != null && !xpath.equals("")) {
				String jsGetMarkElement = Helper.getJSGetElement(xpath) + "try{var oldStyle;"
				        + "if(ATUSA_element!=null){\n"
				        + "if(ATUSA_element.nodeName.toLowerCase()  == 'tr'){\n"
				        + "oldStyle = ATUSA_element.style.background;"
				        + "ATUSA_element.style.background='" + highlight.getColor() + "';"
				        + "}else{" + "oldStyle = ATUSA_element.style.border;"
				        + "ATUSA_element.style.border='2px solid " + highlight.getColor()
				        + "';" + "}"
				        + "if(ATUSA_element.getAttribute('style') == null){return null}"
				        + "return oldStyle;" + "}" + "}catch(e){}";

				Object obj = browser.executeJavaScript(jsGetMarkElement);
				String style = null;
				if (obj != null) {
					style = obj.toString();
				} else {
					style = null;
				}
				highlightedElements.add(new HighlightedElement(highlight, style));
			}
		}
		return highlightedElements;
	}

	private void removeHighlightedElement(
	        List<HighlightedElement> highlightedElements, EmbeddedBrowser browser)
	        throws CrawljaxException {
		// walk backwards to undo in reverse order as highlights are added because
		// the multiple highlights could be added
		for (int i = highlightedElements.size() - 1; i >= 0; i--) {
			HighlightedElement highlightedElement = highlightedElements.get(i);
			String xpath = XPathHelper.stripXPathToElement(
			        highlightedElement.getHighlight().getXpathCurrentDom());
			String style = highlightedElement.getOldStyle();
			String jsRevert = Helper.getJSGetElement(xpath);
			// remove style attribute if did not have a style attribute before
			if (style == null || style.equals("")) {
				jsRevert += "try{" + "if(ATUSA_element!=null){"
				        + "ATUSA_element.removeAttribute('style');" + "}" + "}catch(e){}";
			} else {
				// else restore style property
				jsRevert += "try{" + "if(ATUSA_element!=null){\n"
				        + "if(ATUSA_element.nodeName.toLowerCase()  == 'tr'){\n"
				        + "ATUSA_element.style.background='" + style + "';" + "}else{"
				        + "ATUSA_element.style.border='" + style + "';" + "}" + "}"
				        + "}catch(e){}";
			}
			browser.executeJavaScript(jsRevert);
		}
	}

	/**
	 * Class for saving the styles for highlighting in order to restore the original style.
	 *
	 * @author dannyroest@gmail.com (Danny Roest)
	 * @version $id$
	 */
	private class HighlightedElement {

		private final Highlight highlight;
		private final String oldStyle;

		public HighlightedElement(Highlight highlight, String oldStyle) {
			super();
			this.highlight = highlight;
			this.oldStyle = oldStyle;
		}

		public Highlight getHighlight() {
			return highlight;
		}

		public String getOldStyle() {
			return oldStyle;
		}
	}

    @Override
	public void postCrawling(CrawlSession session) {
		try {
			this.generate();
		} catch (IOException e) {
			LOGGER.error("Could not generate ErrorReport because of IOException", e);
		}
	}

    /**
	 * store a screenshot of the original state which is currently in the browser specified.
	 *
	 * @param browser
	 *            the browser holding the state
	 * @param state
	 *            the state.
	 */
    public void storeOriginalScreenShot(EmbeddedBrowser browser, StateVertix state) {
		try {
			makeScreenShot(browser, state.getName(), this.originalScreenshotsFolder);
			// Store that a state is shot.
			originalScreenShotsTaken.add(state.getName());
		} catch (CrawljaxException e) {
			LOGGER.warn("Catched exception while creating ScreenShot,"
			        + " possibly the Browser did not support it", e);
		}
	}
    
	@Override
	public void onNewState(CrawlSession session) {
		this.storeOriginalScreenShot(session.getBrowser(), session.getCurrentState());
	}
}
