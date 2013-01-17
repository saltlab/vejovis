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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.FunctionNode;

import com.crawljax.plugins.aji.JSASTModifier;
import com.crawljax.plugins.aji.executiontracer.ProgramPoint;
import com.crawljax.util.Helper;

/**
 * This class implements the visitor interface of Mozilla Rhino and adds assertions on function
 * entry and exit for certain functions.
 * 
 * @author Frank Groeneveld
 * @version $Id: AstAssertionInserter.java 6165 2009-12-16 15:32:40Z frank $
 */
public class AstAssertionInserter extends JSASTModifier {

	private Map<String, List<String>> assertions = new TreeMap<String, List<String>>();

	private String assertionFilenameAndPath;

	public static final String JSASSERTIONLOGNAME = "window.assertionlog";

	/**
	 * @param filenameAndPath
	 *            Location of the assertion file, including its name.
	 */
	public AstAssertionInserter(String filenameAndPath) {
		assertionFilenameAndPath = filenameAndPath;
	}

	/**
	 * Create assertion code for a pre/post condition or invariant.
	 * 
	 * @param function
	 *            The function where the assertion is inserted.
	 * @param postfix
	 *            Whether this is a function enter or exit, based on Daikon naming.
	 * @param lineNo
	 *            The line number where this assertion will end up.
	 * @return The AstNode with assertion code.
	 */
	protected AstNode createNode(FunctionNode function, String postfix, int lineNo) {
		String code = "";
		List<String> expressions = null;

		if (postfix == ProgramPoint.EXITPOSTFIX) {
			/* get the assertion for this function exit */
			expressions =
			        assertions.get(getScopeName() + "." + getFunctionName(function) + postfix
			                + lineNo);
		}

		/* TODO: combine exit<num> and exit points? */

		/* get the assertion for this function entry/exit */
		if (expressions == null) {
			expressions =
			        assertions.get(getScopeName() + "." + getFunctionName(function) + postfix);
		}

		if (expressions == null) {
			return parse("/* empty */");
		}
		/* walk through all expressions */
		for (String expression : expressions) {

			/* escape the expression for quotes */
			String escapedExpression = expression.replaceAll("\\\'", "\\\\\'");
			code +=
			        "if (!(" + expression + ")) { " + JSASSERTIONLOGNAME + ".push(new Array('"
			                + escapedExpression + "', '" + getScopeName() + "', '" + lineNo
			                + "')); }; ";
		}

		return parse(code);
	}
	
	protected AstNode createNode(AstRoot root, String postfix, int lineNo, int rootCount) {
		return null;
	}

	private AstNode jsAssertDeclaration() {
		File js = new File(this.getClass().getResource("/arrayutils.js").getFile());

		String code;
		code =
		        Helper.getContent(js) + " if(typeof(" + JSASSERTIONLOGNAME
		                + ") == 'undefined') {" + JSASSERTIONLOGNAME + " = new Array();" + "}";

		return parse(code);
	}

	@Override
	public void finish(AstRoot node) {
		node.addChildToFront(jsAssertDeclaration());
	}

	@Override
	public void start() {
		try {
			String line = "", ppt = "";

			List<String> subset = new ArrayList<String>();

			BufferedReader input =
			        new BufferedReader(new FileReader(getAssertionFilenameAndPath()));

			while ((line = input.readLine()) != null) {
				if (line
				        .equals("================================================================"
				                + "===========")) {
					/* this is a new program point, so save the old one */
					assertions.put(ppt, subset);
					/* get the new program point name */
					ppt = input.readLine();
					/* new list of assertions for this new program point */
					subset = new ArrayList<String>();
				} else {
					/* skip unsupported or prestate stuff */
					if (line.startsWith("warning:") || line.contains("\\old")
					        || line.contains("has only one value")) {
						continue;
					}
					if (!"".equals(line)) {
						/* add the assertion to the subset for this program point */
						subset.add(line);
					}
				}
			}
			/* don't forget the last one */
			assertions.put(ppt, subset);

			input.close();

		} catch (FileNotFoundException e) {
			LOGGER.error("Assertion file not found: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getAssertionFilenameAndPath() {
		return assertionFilenameAndPath;
	}

	@Override
	protected AstNode createPointNode(String objectAndFunction, int lineNo) {
		/* TODO: fill this for dom change checking */
		return parse("/* */");
	}
}
