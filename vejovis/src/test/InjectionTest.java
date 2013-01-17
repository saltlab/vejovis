package test;

import com.crawljax.core.configuration.*;
import com.crawljax.core.CrawljaxController;
import com.crawljax.plugins.aji.JSModifyProxyPlugin;
import com.crawljax.plugins.aji.executiontracer.*;
import com.crawljax.plugins.crawloverview.CrawlOverview;
import com.crawljax.plugins.webscarabwrapper.WebScarabWrapper;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class InjectionTest {
	private static String URL = "";
	
	static CrawljaxConfiguration config;
	static CrawlSpecification crawler;
	static List<String> excludeList;
	static String outputFolder;
	
	static final String JSSOURCEOUTPUTFOLDER = "js_files_sample";
	static final String EXECUTIONTRACEDIRECTORY = "executiontrace/";
	static final String DOMIDLISTSDIRECTORY = "domIdLists/";
	static final String OUTPUTDIRECTORY = "/sample_assertions/";
	static final String INJECTIONSFOLDER = "sampleInjections/";
	
	public static void main(String[] args) {
		String srcLine = "";
		String funcName = "";
		int lineNo = -1;
		boolean injectMode = true;
		int injectLineNo = 0;
		String injectFuncName = "";
		String injectOrigSrc = "";
		String injectModSrc = "";
		URL = "http://frolinsfilms.comuv.com/vejovis_example_injection.html";
		
		//Retrieve injections from injection file(s) and store them in 
		File dir = new File(INJECTIONSFOLDER);
		List<InjectionInput> injList = new ArrayList<InjectionInput>();
		for (File child : dir.listFiles()) {
			//Ignore the self and parent aliases, and files beginning with "."
			if (".".equals(child.getName()) || "..".equals(child.getName()) || child.getName().indexOf(".") == 0) {
			      continue; 
			}
			
			String fullpath = INJECTIONSFOLDER + "/" + child.getName();
			
			try {
				String contents = readFile(fullpath);
				
				String[] injectionStrs = contents.split("-----------------------------------------------------------------------------------------------------------");
				for (String injection : injectionStrs) {
					if (injection == "" || injection == null) {
						continue;
					}
					
					boolean gotOutput1 = false;
					boolean gotOutput2 = false;
					boolean gotOutput3 = false;
					boolean gotOutput4 = false;
					boolean gotOutput5 = false;
					boolean gotOutput6 = false;
					boolean gotOutput7 = false;
					
					int output1 = 0; //line no. of GEBID
					String output2 = ""; //original source of GEBID
					int output3 = 0; //line no. of mutated line
					String output4 = ""; //original source of mutated line
					String output5 = ""; //modified source of mutated line
					String output6 = ""; //name of enclosing function of mutated line
					List<String> output7 = null; //keywords
					
					//Split into different lines
					StringTokenizer st = new StringTokenizer(injection, "\n\r");
					while (st.hasMoreTokens()) {
						String nextLine = st.nextToken();
						if (nextLine.startsWith("OUTPUT 1")) {
							int lastColon = nextLine.indexOf(":", 9);
							String output1Str = nextLine.substring(lastColon + 2);
							output1 = Integer.parseInt(output1Str);
							gotOutput1 = true;
						}
						else if (nextLine.startsWith("OUTPUT 2")) {
							int lastColon = nextLine.indexOf(":", 9);
							output2 = nextLine.substring(lastColon + 2);
							gotOutput2 = true;
						}
						else if (nextLine.startsWith("OUTPUT 3")) {
							int lastColon = nextLine.indexOf(":", 9);
							String output3Str = nextLine.substring(lastColon + 2);
							output3 = Integer.parseInt(output3Str);
							gotOutput3 = true;
						}
						else if (nextLine.startsWith("OUTPUT 4")) {
							int lastColon = nextLine.indexOf(":", 9);
							output4 = nextLine.substring(lastColon + 2);
							gotOutput4 = true;
						}
						else if (nextLine.startsWith("OUTPUT 5")) {
							int lastColon = nextLine.indexOf(":", 9);
							output5 = nextLine.substring(lastColon + 2);
							gotOutput5 = true;
						}
						else if (nextLine.startsWith("OUTPUT 6")) {
							int lastColon = nextLine.indexOf(":", 9);
							output6 = nextLine.substring(lastColon + 2);
							gotOutput6 = true;
						}
						else if (nextLine.startsWith("OUTPUT 7")) {
							String theKeywords = nextLine.substring(10);
							StringTokenizer kwTokenize = new StringTokenizer(theKeywords, ",");
							output7 = new ArrayList<String>();
							while (kwTokenize.hasMoreTokens()) {
								//Add tokens to keyword list (output7)
								output7.add(kwTokenize.nextToken().trim());
							}
							
							gotOutput7 = true;
						}
					}
					
					if (gotOutput1 && gotOutput2 && gotOutput3 && gotOutput4 && gotOutput5 && gotOutput6 && gotOutput7) {
						//Place InjectionInput in list
						InjectionInput inj = new InjectionInput(output1, output2, output3, output4, output5, output6, output7);
						injList.add(inj);
					}
				}
			}
			catch (Exception e) {
				System.out.println("Error: Malformed injection file");
				System.out.println("File name: " + child.getName());
			}
		}
		
		int totalActivated = 0;
		int totalSuccesses = 0;
		int totalFailures = 0;
		int totalInjected = injList.size();
		
		JSModifyProxyPlugin modifier = new JSModifyProxyPlugin(new AstInstrumenter());
		
		for (int i = 0; i < injList.size(); i++) {
			System.out.println("\n");
			System.out.println("Successes So Far: " + totalSuccesses);
			System.out.println("Failures So Far: " + totalFailures);
			System.out.println("Activated So Far: " + totalActivated);
			System.out.println("Injected So Far: " + totalInjected);
			
			try {
				config = new CrawljaxConfiguration();
				crawler = new CrawlSpecification(URL);
				
				//The test(s)
				test1();
				
				config.setCrawlSpecification(crawler);
				config.addPlugin(new CrawlOverview());
				
				ProxyConfiguration prox = new ProxyConfiguration();
				
				WebScarabWrapper web = new WebScarabWrapper();
				modifier.setExcludeList(excludeList);
				/*START INJECT MODE*/
				if (injectMode) {
					modifier.setInject();
					modifier.setInjectLineNo(injList.get(i).getMutatedLineNo()); //absolute line number
					modifier.setInjectFuncName(injList.get(i).getFuncNameMutated());
					modifier.setInjectOrigSrc(injList.get(i).getOrigSrcCodeMutated());
					modifier.setInjectModSrc(injList.get(i).getModSrcCodeMutated());
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
				
				//assume GEBID line is mutated here
				DirectDOMAccess dda = new DirectDOMAccess(injList.get(i).getGebidLineNo(), injList.get(i).getModSrcCodeMutated(), injList.get(i).getFuncNameMutated());
				StringSetExtractor extractor = new StringSetExtractor(JSSOURCEOUTPUTFOLDER, OUTPUTDIRECTORY+outputFolder+EXECUTIONTRACEDIRECTORY,dda);
				/*START INJECT MODE*/
				if (injectMode) {
					extractor.setInject();
					extractor.setInjectLineNo(injList.get(i).getMutatedLineNo());
					extractor.setInjectFuncName(injList.get(i).getFuncNameMutated());
					extractor.setInjectOrigSrc(injList.get(i).getOrigSrcCodeMutated());
					extractor.setInjectModSrc(injList.get(i).getModSrcCodeMutated());
				}
				/*END INJECT MODE*/
				int extractionResult = extractor.extractStringSet();
				List<StringSetLine> strSet = extractor.getStrSet();
				
				//Get current state
				int currentState = extractor.getCurrentState();
				
				//Run FixClassSelector
				FixClassSelector fcs = new FixClassSelector(strSet, extractor.getErroneousID(), dda, states, currentState); //create module that gets erroneous ID after
				fcs.chooseFixClasses();
				
				//Check if any of the suggested fixes matches all keywords
				List<CandidateFix> fixSuggestions = fcs.getCandidateFixes();
				boolean foundMatch = false;
				for (int j = 0; j < fixSuggestions.size(); j++) {
					String suggestion = fixSuggestions.get(j).getMessageStr();
					if (containsKeywords(suggestion, injList.get(i).getKeywordList())) {
						foundMatch = true;
						break;
					}
				}
				
				if (foundMatch) {
					totalSuccesses++;
				}
				else {
					totalFailures++;
				}
				
				totalActivated++;
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
				System.out.println("Error: exception caught");
				//Don't exit
			}
		}
		
		System.out.println("\n");
		System.out.println("Total Successes: " + totalSuccesses);
		System.out.println("Total Failures: " + totalFailures);
		System.out.println("Total Activated: " + totalActivated);
		System.out.println("Total Injected: " + totalInjected);
		
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
	
	private static String readFile(String path) throws IOException {
		FileInputStream stream = new FileInputStream(new File(path));
		try {
		    FileChannel fc = stream.getChannel();
		    MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
		    /* Instead of using default, pass in a decoder. */
		    return Charset.defaultCharset().decode(bb).toString();
		}
		finally {
			stream.close();
		}
	}

	/**
	 * Check if msg contains all the keywords
	 * @param msg
	 * @return
	 */
	private static boolean containsKeywords(String msg, List<String> keywords) {
		boolean retVal = true;
		for (int i = 0; i < keywords.size(); i++) {
			retVal = retVal && msg.contains(keywords.get(i));
		}
		
		return retVal;
	}
}