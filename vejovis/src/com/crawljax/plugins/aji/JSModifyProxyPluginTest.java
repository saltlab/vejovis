package com.crawljax.plugins.aji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstNode;
import org.owasp.webscarab.httpclient.HTTPClient;
import org.owasp.webscarab.model.HttpUrl;
import org.owasp.webscarab.model.Request;
import org.owasp.webscarab.model.Response;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.crawljax.plugins.aji.executiontracer.AstInstrumenter;
import com.crawljax.util.Helper;

/**
 * Tester for js instrumentation plugin.
 * 
 * @author Frank Groeneveld
 */
public class JSModifyProxyPluginTest {

	private final String simpleJS =
	        "var simple = 1; function test() { "
	                + "var something = false; }; simple++; simple = 10;"
	                + "window.test = 0; window.test++;";

	private final CompilerEnvirons compilerEnvirons = new CompilerEnvirons();

	private static JSModifyProxyPlugin plugin;
	private static Class<? extends JSModifyProxyPlugin> accessiblePlugin;

	/**
	 * Create an AST from the passed js.
	 * 
	 * @param js
	 *            The javascript to be parsed.
	 * @return The AST.
	 */
	private AstNode parse(String js) {
		Parser p = new Parser(compilerEnvirons, null);

		return p.parse(js, null, 0);
	}

	/**
	 * One time setup done before the tests are ran.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		List<String> excludes = new ArrayList<String>();
		excludes.add(".*dontinstrument.*");
		plugin = new JSModifyProxyPlugin(new AstInstrumenter(), excludes);
		accessiblePlugin = plugin.getClass();
	}

	/**
	 * One time tear down after all tests have finished.
	 */
	@AfterClass
	public static void oneTimeTearDown() {

	}

	private Object invokePrivateMethod(String name, Object[] args) {
		try {
			// Print all the method names & execution result
			Method[] methods = accessiblePlugin.getDeclaredMethods();
			for (int i = 0; i < methods.length; i++) {
				if (methods[i].getName().equals(name)) {
					methods[i].setAccessible(true);
					return methods[i].invoke(plugin, args);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Calling a private method named " + name + " using reflection failed. "
			        + e.getMessage());
		}
		fail("Calling a private method named " + name + " using reflection failed because the "
		        + "method was not found.");
		return null;
	}

	/**
	 * Test the get proxy plugin method.
	 */
	@Test
	public void getProxyPlugin() {
		HTTPClient plug = plugin.getProxyPlugin(null);

		assertNotNull("Unable to get new proxy plugin", plug);
	}

	/**
	 * Test the get plugin name method.
	 */
	@Test
	public void getPluginName() {
		assertEquals("Wrong plugin name returned", "JSInstrumentPlugin", plugin.getPluginName());
	}

	/**
	 * Test the instrumentjs method.
	 */
	@Test
	public void modifyJS() {
		String result = (String) invokePrivateMethod("modifyJS", new Object[] { simpleJS, "" });
		assertFalse("No instrumentation code was added", equalSources(result, simpleJS));
	}

	/**
	 * Tests the createresponse method.
	 */
	@Test
	public void createResponseJavaScriptFile() {
		/* test javascript file */
		Response response = new Response();
		response.setHeader("Content-Type", "application/x-javascript");
		response.setContent(simpleJS.getBytes());

		response = invokeCreateResponse("http://createResponse-test.com", response);

		assertFalse("No instrumentation code was added", equalSources(new String(response
		        .getContent()), simpleJS));
	}

	/**
	 * Tests the createresponse method.
	 */
	@Test
	public void createResponseHTMLFile() {
		/* test html file */
		Response response = new Response();
		response.setHeader("Content-Type", "text/html");
		response.setContent(("<html><head></head><body><script type=\"text/javascript\">"
		        + simpleJS + "</script></body></html>").getBytes());

		response = invokeCreateResponse("http://createResponse-test.com", response);

		try {
			Document dom = Helper.getDocument(new String(response.getContent()));

			/* find script nodes in the html */
			NodeList nodes = dom.getElementsByTagName("script");
			String content = nodes.item(0).getTextContent();
			assertFalse("No instrumentation code was added", equalSources(new String(content),
			        simpleJS));
		} catch (Exception e) {
			fail("Unable to parse html");
			e.printStackTrace();
		}

	}

	/**
	 * Tests if the createresponse uses the excludefilename list.
	 */
	@Test
	public void createResponseDontInstrument() {
		/* test javascript file */
		Response response = new Response();
		response.setHeader("Content-Type", "application/x-javascript");
		response.setContent(simpleJS.getBytes());

		response = invokeCreateResponse("http://createresponse.com/dontinstrument.js", response);

		assertTrue("Instrumentation code was added", equalSources(new String(response
		        .getContent()), simpleJS));

	}

	/**
	 * Invoke the private method create response with a request object that contains filename.
	 */
	private Response invokeCreateResponse(String filename, Response response) {

		try {
			Request request = new Request();
			request.setURL(new HttpUrl(filename));

			Object[] args = { response, request };

			return (Response) invokePrivateMethod("createResponse", args);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		return null;
	}

	private boolean equalSources(String s1, String s2) {
		return parse(s1).toSource().equals(parse(s2).toSource());
	}
}