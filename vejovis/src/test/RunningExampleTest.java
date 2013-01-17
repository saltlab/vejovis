package test;

import com.crawljax.core.configuration.*;
import com.crawljax.core.CrawljaxController;
import com.crawljax.plugins.aji.JSModifyProxyPlugin;
import com.crawljax.plugins.aji.executiontracer.*;
import com.crawljax.plugins.crawloverview.CrawlOverview;
import com.crawljax.plugins.webscarabwrapper.WebScarabWrapper;

import java.util.*;

public class RunningExampleTest {
	private static final int testNumber = 8;
	
	private static String URL = "";
	
	static CrawljaxConfiguration config;
	static CrawlSpecification crawler;
	static List<String> excludeList;
	static String outputFolder;
	
	static final String JSSOURCEOUTPUTFOLDER = "js_files_sample";
	static final String EXECUTIONTRACEDIRECTORY = "executiontrace/";
	static final String DOMIDLISTSDIRECTORY = "domIdLists/";
	static final String OUTPUTDIRECTORY = "/sample_assertions/";
	
	public static void main(String[] args) {
		String srcLine = "";
		String funcName = "";
		int lineNo = -1;
		boolean injectMode = false;
		int injectLineNo = 0;
		String injectFuncName = "";
		String injectOrigSrc = "";
		String injectModSrc = "";
		int injectType = 0; //0 - regular, 1 - DOM node removal
		switch(testNumber) {
			case 1:
				URL = "http://frolinsfilms.comuv.com/vejovis_example.html";
				srcLine = "var msgDiv = document.getElementById(\"mainResults\" + msg + index);";
				funcName = "checkIfRight";
				lineNo = 11;
				break;
			case 2:
				URL = "http://frolinsfilms.comuv.com/vejovis_example_v2.html";
				srcLine = "var msgDiv = document.getElementById(prefix+ idx + \"\");";
				funcName = "showResult";
				lineNo = 1;
				break;
			case 3:
				URL = "http://frolinsfilms.comuv.com/vejovis_example_v3.html";
				srcLine = "var msgDiv = document.getElementById(prefix+ idx + idx + \"\" + extra + \"hello\" + extra);";
				funcName = "showResult";
				lineNo = 2;
				break;
			case 4:
				URL = "http://frolinsfilms.comuv.com/vejovis_example_v4.html";
				srcLine = "var msgDiv = document.getElementById(\"mainResults\" + msg + index);";
				funcName = "checkIfRight";
				lineNo = 11;
				break;
			case 5:
				URL = "http://frolinsfilms.comuv.com/vejovis_example_v5.html";
				srcLine = "var msgDiv = document.getElementById(\"mainResults\" + msg + index);";
				funcName = "checkIfRight";
				lineNo = 11;
				break;
			case 6:
				URL = "http://frolinsfilms.comuv.com/vejovis_example_v6.html";
				srcLine = "var msgDiv = document.getElementById(\"smainResults\" + msg + index);";
				funcName = "checkIfRight";
				lineNo = 11;
				break;
			case 7:
				URL = "http://frolinsfilms.comuv.com/vejovis_example_injection.html";
				srcLine = "var msgDiv = document.getElementById(\"mainResults\" + msg + index);";
				funcName = "checkIfRight";
				lineNo = 11;
				injectMode = true;
				injectLineNo = 5;
				injectFuncName = "checkIfRight";
				injectOrigSrc = "msg = \"Correct\";";
				injectModSrc = "msg = \"Correct2345\";";
				break;
			case 8:
				URL = "http://frolinsfilms.comuv.com/vejovis_example_injection.html";
				srcLine = "var msgDiv = document.getElementById(\"mainResults\" + msg + index);";
				funcName = "checkIfRight";
				lineNo = 11;
				injectMode = true;
				injectLineNo = 5;
				injectFuncName = "checkIfRight";
				injectOrigSrc = "msg = \"Correct\";";
				injectModSrc = "var m = document.getElementById(\"mainResultsCorrect0\"); var n = document.getElementById(\"mainResultsCorrect1\"); m.parentNode.removeChild(m); n.parentNode.removeChild(n); msg = \"Correct\";";
				injectType = 1;
				break;
			default:
				System.out.println("Error: Invalid test number");
				System.exit(-1);
				break;
		}
		
		config = new CrawljaxConfiguration();
		crawler = new CrawlSpecification(URL);
		
		//The test(s)
		test1();
		
		config.setCrawlSpecification(crawler);
		config.addPlugin(new CrawlOverview());
		
		ProxyConfiguration prox = new ProxyConfiguration();
		
		WebScarabWrapper web = new WebScarabWrapper();
		JSModifyProxyPlugin modifier = new JSModifyProxyPlugin(new AstInstrumenter(), excludeList);
		/*START INJECT MODE*/
		if (injectMode) {
			modifier.setInject();
			modifier.setInjectLineNo(injectLineNo);
			modifier.setInjectFuncName(injectFuncName);
			modifier.setInjectOrigSrc(injectOrigSrc);
			modifier.setInjectModSrc(injectModSrc);
			modifier.setInjectType(injectType);
		}
		/*END INJECT MODE*/
		modifier.excludeDefaults();
		modifier.setInstrumentAsyncs(false);
		web.addPlugin(modifier);
		JSExecutionTracer tracer = new JSExecutionTracer("daikon.assertions");
		tracer.setOutputFolder(OUTPUTDIRECTORY + outputFolder);
		
		modifier.setJsSourceOutputFolder(JSSOURCEOUTPUTFOLDER);

		config.addPlugin(tracer);
		config.addPlugin(web);
		config.setProxyConfiguration(prox);
		
		try {
			CrawljaxController crawljax = new CrawljaxController(config);
			crawljax.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//Check DOM IDs in tracer
		List<List<DomIdInfo>> states = tracer.getStates();
		
		//Run StringSetExtractor
		//Set up direct DOM access
		
		try {
			DirectDOMAccess dda = new DirectDOMAccess(lineNo, srcLine, funcName);
			StringSetExtractor extractor = new StringSetExtractor(JSSOURCEOUTPUTFOLDER, OUTPUTDIRECTORY+outputFolder+EXECUTIONTRACEDIRECTORY,dda);
			/*START INJECT MODE*/
			if (injectMode) {
				extractor.setInject();
				extractor.setInjectLineNo(injectLineNo);
				extractor.setInjectFuncName(injectFuncName);
				extractor.setInjectOrigSrc(injectOrigSrc);
				extractor.setInjectModSrc(injectModSrc);
				extractor.setInjectType(injectType);
			}
			/*END INJECT MODE*/
			int extractionResult = extractor.extractStringSet();
			List<StringSetLine> strSet = extractor.getStrSet();
			
			//Get current state
			int currentState = extractor.getCurrentState();
			
			//Run FixClassSelector
			FixClassSelector fcs = new FixClassSelector(strSet, extractor.getErroneousID(), dda, states, currentState); //create module that gets erroneous ID after
			fcs.chooseFixClasses();
			
			return;
		}
		catch (Exception e) {
			System.out.println("Error: exception caught");
		}
		
		return; //Put this here for debugging help
	}
	
	private static void test1() {
		crawler.setMaximumStates(4);
		crawler.setDepth(3);

		crawler.click("button").withAttribute("id", "mainOKButton");
		crawler.click("button").withAttribute("id", "mainAnswerSubmit");
		
		InputSpecification choices = new InputSpecification();
		choices.field("mainChoices").setValue("basel");
		crawler.setInputSpecification(choices);
		
		//Exclude list
		List<String> sampleExclude = new ArrayList<String>();
		
		excludeList = sampleExclude;
		
		outputFolder = "test1/";
	}
}