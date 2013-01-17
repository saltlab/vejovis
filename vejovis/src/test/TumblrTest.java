package test;

import com.crawljax.core.configuration.*;
import com.crawljax.core.CrawljaxController;
import com.crawljax.plugins.aji.JSModifyProxyPlugin;
import com.crawljax.plugins.aji.executiontracer.*;
import com.crawljax.plugins.crawloverview.CrawlOverview;
import com.crawljax.plugins.webscarabwrapper.WebScarabWrapper;

import java.util.*;

public class TumblrTest {
	private static final int testNumber = 1;
	
	private static String URL = "";
	
	static CrawljaxConfiguration config;
	static CrawlSpecification crawler;
	static List<String> excludeList;
	static String outputFolder;
	
	static final String JSSOURCEOUTPUTFOLDER = "js_files_tumblr";
	static final String EXECUTIONTRACEDIRECTORY = "executiontrace/";
	static final String DOMIDLISTSDIRECTORY = "domIdLists/";
	static final String OUTPUTDIRECTORY = "/tumblr_assertions/";
	
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
				URL = "http://www.tumblr.com/why-tumblr";
				srcLine = "$('dot_' + (promo_offset % promo_count || promo_count)).addClassName('active');";
				funcName = "change_promo";
				lineNo = 14;
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
		modifier.setInstrumentAsyncs(true);
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
			
			//Print suggestions
			List<CandidateFix> fixSuggestions = fcs.getCandidateFixes();
			System.out.println("TOTAL FIX SUGGESTIONS: " + fixSuggestions.size());
			for (int i = 0; i < fixSuggestions.size(); i++) {
				CandidateFix nextFix = fixSuggestions.get(i);
				System.out.println("FIX CLASS: " + nextFix.getFixClass().name());
				System.out.println(nextFix.getMessageStr());
			}
			
			return;
		}
		catch (Exception e) {
			System.out.println("Error: exception caught");
		}
		
		return; //Put this here for debugging help
	}
	
	private static void test1() {
		crawler.setMaximumStates(3);
		crawler.setDepth(3);

		crawler.clickDefaultElements();
		
		//Exclude list
		List<String> tumblrExclude = new ArrayList<String>();
		tumblrExclude.add(".*why-tumblrscript[012345678]");
		tumblrExclude.add(".*quant.*");
		tumblrExclude.add(".*assets.*");
		
		excludeList = tumblrExclude;
		
		outputFolder = "test1/";
	}
}