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
package com.crawljax.plugins.aji.assertionchecker;

import java.util.List;

import org.apache.log4j.Logger;

import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlSession;
import com.crawljax.core.plugin.PreStateCrawlingPlugin;
import com.crawljax.plugins.errorreport.ErrorReport;
import com.crawljax.plugins.errorreport.ReportError;

/**
 * Plugin that gets the JavaScript assertion results from the browser.
 * 
 * @author Frank Groeneveld
 * @version $Id: JSAssertionResults.java 6162 2009-12-16 13:56:21Z frank $
 */
public class JSAssertionResults implements PreStateCrawlingPlugin {

	private ErrorReport errorReporter;
	private static final Logger LOGGER = Logger.getLogger(JSAssertionResults.class);

	/**
	 * Construct the assertion plugin.
	 * 
	 * @param reporter
	 *            The reporter to use if invariants fail.
	 */
	public JSAssertionResults(ErrorReport reporter) {
		errorReporter = reporter;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void preStateCrawling(CrawlSession session, List<CandidateElement> candidateElements) {

		/* get the results array from the browser */
		String script = "return " + AstAssertionInserter.JSASSERTIONLOGNAME + ";";
		try {

			LOGGER.info("Reading errors from the browser");
			/* get the object from the browser */
			Object o = session.getBrowser().executeJavaScript(script);

			LOGGER.info("Analyzing errors");
			/* make sure it is an iteratable list */
			if (o instanceof List<?>) {

				List<List<String>> result = (List<List<String>>) o;
				/* walk through all failures */
				for (List<String> list : result) {
					System.err.println(list.get(0));
					System.err.println(list.get(1));
					System.err.println(list.get(2));

					/*
					 * list now contains 3 items: the expressions that failed, the name of the file
					 * and the line number (in that order).
					 */

					/* add this failure to the crawl report */
					 errorReporter.addFailure((new ReportError("Failed JavaScript assertion(s)",
					 list.get(1) + " at line " + list.get(2) + ": " + list.get(0)))
					 .dontIncludeScreenshots(), session.getBrowser());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		LOGGER.info("All done");
	}
}
