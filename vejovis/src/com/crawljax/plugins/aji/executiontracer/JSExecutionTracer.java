/*
    Automatic JavaScript Invariants is a plugin for Crawljax that can be
    used to derive JavaScript invariants automatically and use them for
    regressions testing.
    Copyright (C) 2010  crawljax.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
package com.crawljax.plugins.aji.executiontracer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawljaxException;
import com.crawljax.core.plugin.GeneratesOutput;
import com.crawljax.core.plugin.PostCrawlingPlugin;
import com.crawljax.core.plugin.PreCrawlingPlugin;
import com.crawljax.core.plugin.PreStateCrawlingPlugin;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.util.Helper;

import daikon.Daikon;

import org.w3c.dom.*;

/**
 * Crawljax Plugin that reads an instrumentation array from the webbrowser and saves the contents in
 * a Daikon trace file.
 * 
 * @author Frank Groeneveld
 * @version $Id: JSExecutionTracer.java 6162 2009-12-16 13:56:21Z frank $
 */
public class JSExecutionTracer
        implements PreStateCrawlingPlugin, PostCrawlingPlugin, PreCrawlingPlugin, OnNewStatePlugin, GeneratesOutput {

	private static final int ONE_SEC = 1000;

	private static String outputFolder;
	private static String assertionFilename;

	private static JSONArray points = new JSONArray();

	private static final Logger LOGGER = Logger.getLogger(JSExecutionTracer.class.getName());

	public static String EXECUTIONTRACEDIRECTORY = "executiontrace/";
	
	public static String DOMIDLISTSDIRECTORY = "domIdLists/";
	
	private static int stateCounterForDOMRetrieval = 0;
	
	//DOM ID info
	private static List<List<DomIdInfo>> states = new ArrayList<List<DomIdInfo>>();

	/**
	 * @param filename
	 *            How to name the file that will contain the assertions after execution.
	 */
	public JSExecutionTracer(String filename) {
		assertionFilename = filename;
	}

	/**
	 * Initialize the plugin and create folders if needed.
	 * 
	 * @param browser
	 *            The browser.
	 */
	@Override
	public void preCrawling(EmbeddedBrowser browser) {
		try {
			Helper.directoryCheck(getOutputFolder());
			Helper.directoryCheck(getOutputFolder() + EXECUTIONTRACEDIRECTORY);
			Helper.directoryCheck(getOutputFolder() + DOMIDLISTSDIRECTORY);
			
			//Delete all previous traces
			File dir = new File(getOutputFolder() + EXECUTIONTRACEDIRECTORY);
			for (File child : dir.listFiles()) {
				child.delete();
			}
			
			//Delete all previous DOM ID list files
			dir = new File(getOutputFolder() + DOMIDLISTSDIRECTORY);
			for (File child : dir.listFiles()) {
				child.delete();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		stateCounterForDOMRetrieval = 0;
		states = new ArrayList<List<DomIdInfo>>();
	}

	/**
	 * Retrieves the JavaScript instrumentation array from the webbrowser and writes its contents in
	 * Daikon format to a file.
	 * 
	 * @param session
	 *            The crawling session.
	 * @param candidateElements
	 *            The candidate clickable elements.
	 */
	@Override
	public void preStateCrawling(CrawlSession session, List<CandidateElement> candidateElements) {

		String filename = getOutputFolder() + EXECUTIONTRACEDIRECTORY + "jsexecutiontrace-";
		filename += session.getCurrentState().getName();

		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		Date date = new Date();
		filename += dateFormat.format(date) + "-" + stateCounterForDOMRetrieval +".dtrace";

		try {

			LOGGER.info("Reading execution trace");

			LOGGER.info("Parsing JavaScript execution trace");

			/* FIXME: Frank, hack to send last buffer items and wait for them to arrive */
			session.getBrowser().executeJavaScript("sendReally();");
			Thread.sleep(ONE_SEC);

			Trace trace = Trace.parse(points);

			PrintWriter file = new PrintWriter(filename);
			file.write(trace.getDeclaration());
			file.write('\n');
			file.write(trace.getData(points));
			file.close();

			LOGGER.info("Saved execution trace as " + filename);

			points = new JSONArray();
		} catch (CrawljaxException we) {
			we.printStackTrace();
			LOGGER.error("Unable to get instrumentation log from the browser");
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//FROLIN - GET DOM IDS AND INFO ON THEIR PARENTS
		try {
			getDOMElemsWithID(session);
		}
		catch (Exception e) {
			
		}
		stateCounterForDOMRetrieval++;
	}
	
	@Override
	public void onNewState(CrawlSession session) {
		preStateCrawling(session, null);
	}

	/**
	 * Get a list with all trace files in the executiontracedirectory.
	 * 
	 * @return The list.
	 */
	public List<String> allTraceFiles() {
		ArrayList<String> result = new ArrayList<String>();

		/* find all trace files in the trace directory */
		File dir = new File(getOutputFolder() + EXECUTIONTRACEDIRECTORY);

		String[] files = dir.list();
		if (files == null) {
			return result;
		}
		for (String file : files) {
			if (file.endsWith(".dtrace")) {
				result.add(getOutputFolder() + EXECUTIONTRACEDIRECTORY + file);
			}
		}

		return result;
	}

	@Override
	public void postCrawling(CrawlSession session) {
		try {
			PrintStream output = new PrintStream(getOutputFolder() + getAssertionFilename());

			/* save the current System.out for later usage */
			PrintStream oldOut = System.out;
			/* redirect it to the file */
			System.setOut(output);

			/* don't print all the useless stuff */
			Daikon.dkconfig_quiet = true;
			Daikon.noversion_output = true;

			List<String> arguments = allTraceFiles();

			/*
			 * TODO: Frank, fix this hack (it is done because of Daikon calling cleanup before init)
			 */
			arguments.add("-o");
			arguments.add(getOutputFolder() + "daikon.inv.gz");
			arguments.add("--format");
			arguments.add("javascript");
			arguments.add("--config_option");
			arguments.add("daikon.FileIO.unmatched_procedure_entries_quiet=true");
			arguments.add("--config_option");
			arguments.add("daikon.FileIO.ignore_missing_enter=true");

			/* Restore the old system.out */
			System.setOut(oldOut);

			/* close the output file */
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return Name of the assertion file.
	 */
	public String getAssertionFilename() {
		return assertionFilename;
	}

	@Override
	public String getOutputFolder() {
		return Helper.addFolderSlashIfNeeded(outputFolder);
	}

	@Override
	public void setOutputFolder(String absolutePath) {
		outputFolder = absolutePath;
	}

	/**
	 * Dirty way to save program points from the proxy request threads. TODO: Frank, find cleaner
	 * way.
	 * 
	 * @param string
	 *            The JSON-text to save.
	 */
	public static void addPoint(String string) {
		JSONArray buffer = null;
		try {
			buffer = new JSONArray(string);
			for (int i = 0; i < buffer.length(); i++) {
				points.put(buffer.get(i));
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Retrieve the DOM elements with IDs from the given state
	 * 
	 * Format:
	 * NEXT DOM ELEMENT
	 * ID: <id>
	 * PARENT HASH: <hash>
	 * PARENT ID: <parent id> or nothing (i.e., blank)
	 * PARENT XPATH: <xpath>
	 * 
	 * Each entry is separated by a blank space
	 */
	private void getDOMElemsWithID(CrawlSession session) throws Exception {
		String filename = getOutputFolder() + DOMIDLISTSDIRECTORY + "domidlist-" + stateCounterForDOMRetrieval;
		
		String theId = "";
		
		PrintWriter file = null;
		
		try {
			file = new PrintWriter(filename);
			List <DomIdInfo> domIds = new ArrayList<DomIdInfo>();
			
			NodeList nlist = session.getCurrentState().getDocument().getElementsByTagName("*");
			for (int i = 0; i < nlist.getLength(); i++) {
				Element e = (Element)nlist.item(i);
				if (e.hasAttribute("id")) {
					theId = e.getAttribute("id");
					
					//Get the hashcode of the parent
					Node parent = e.getParentNode();
					if (!(parent instanceof Element)) {
						continue;
					}
					Element parentElem = (Element)parent;
					int parentHash = hash(parentElem);
					
					//Get ID of parent (if it has one)
					String parentId = "";
					if (parentElem.hasAttribute("id")) {
						parentId = parentElem.getAttribute("id");
					}
					
					//Get XPATH
					String xpath = getXPath(parentElem);
					
					//Get tag name
					String tagName = e.getTagName();
					
					//Output to file
					file.write("NEXT DOM ELEMENT\n");
					file.write("ID: ");
					file.write(theId);
					file.write("\n");
					file.write("PARENT HASH: ");
					file.write(Integer.toString(parentHash));
					file.write("\n");
					file.write("PARENT ID: ");
					file.write(parentId);
					file.write("\n");
					file.write("PARENT XPATH: ");
					file.write(xpath);
					file.write("\n");
					file.write("ELEMENT TAG NAME: ");
					file.write(tagName);
					file.write("\n\n");
					
					//For simplicity, output to a structure as well
					//The index items in each list should correspond
					//to each other
					DomIdInfo newDomInfo = new DomIdInfo(theId, parentHash, parentId, xpath, tagName, stateCounterForDOMRetrieval);
					domIds.add(newDomInfo);
				}
			}
			System.err.println("\n");
			states.add(domIds);
		}
		catch (Exception ee) {
			System.err.println("Error: Exception when retrieving document");
			throw new Exception();
			//System.exit(-1);
		}
		finally {
			if (file != null) {
				file.close();
			}
		}
	}
	
	/**
	 * Custom hash function to identify DOM elements' parents
	 */
	private int hash(Element elem) {
		int hash = 7;
		
		//tag name
		hash = 31*hash + ((elem.getTagName() == null) ? 0:elem.getTagName().hashCode());
		
		//namespace URI - should we include this?
		hash = 31*hash + ((elem.getNamespaceURI() == null) ? 0:elem.getNamespaceURI().hashCode());
		
		//namespace prefix
		hash = 31*hash + ((elem.getPrefix() == null) ? 0:elem.getPrefix().hashCode());
		
		//concatenation of attribute names and values (ordered alphabetically)
		NamedNodeMap attr = elem.getAttributes();
		
		//attribute length
		hash = 31*hash + ((attr == null) ? 0:attr.getLength());
		
		//attribute names/values
		//Put the name/value pair in list and then sort alphabetically
		String concat = "";
		if (attr == null || attr.getLength() == 0) {
			hash = 31*hash + concat.hashCode();
			return hash;
		}
		List<String> attrContents = new ArrayList<String>();
		for (int i = 0; i < attr.getLength(); i++) {
			Attr attrItem = (Attr)attr.item(i);
			String name = attrItem.getName();
			String value = attrItem.getValue();
			
			String nameValue = name + value;
			
			attrContents.add(nameValue);
		}
		
		Collections.sort(attrContents);
		
		for (int i = 0; i < attrContents.size(); i++) {
			concat += "&" + attrContents.get(i);
		}
		
		hash = 31*hash + concat.hashCode();
		
		return hash;
	}
	
	public static List<List<DomIdInfo>> getStates() {
		return states;
	}
	
	public String getXPath(Element elem) {
		//See http://stackoverflow.com/questions/2631820/im-storing-click-coordinates-in-my-db-and-then-reloading-them-later-and-showing/2631931#2631931
		String xpath = "";
		
		if (elem == null) {
			return "";
		}
		
		int ix = 0;
		NodeList siblings = elem.getParentNode().getChildNodes();
		for (int i = 0; i < siblings.getLength(); i++) {
			Node sibling = siblings.item(i);
			if (sibling.isEqualNode(elem)) {
				int num = ix + 1;
				Node parentNode = elem.getParentNode();
				if (parentNode.getNodeType() == 1) {
					Element parentElem = (Element)parentNode;
					return getXPath(parentElem) + "/" + elem.getTagName() + "[" + Integer.toString(num) + "]";
				}
				else {
					return "/" + elem.getTagName() + "[" + Integer.toString(num) + "]";
				}
			}
			if (sibling.getNodeType() == 1) {
				Element siblingElem = (Element)sibling;
				if (siblingElem.getTagName().equals(elem.getTagName())) {
					ix++;
				}
			}
		}
		
		return xpath;
	}
	
	public void setTraceDirectory(String _traceDirectory) {
		this.EXECUTIONTRACEDIRECTORY = _traceDirectory;
	}
	
	public void setDomListsDirectory(String _domDirectory) {
		this.DOMIDLISTSDIRECTORY = _domDirectory;
	}
}
