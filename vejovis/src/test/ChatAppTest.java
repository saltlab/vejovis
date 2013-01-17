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

public class ChatAppTest {
	private static String URL = "";
	
	static CrawljaxConfiguration config;
	static CrawlSpecification crawler;
	static List<String> excludeList;
	static String outputFolder;
	
	static final String JSSOURCEOUTPUTFOLDER = "js_files_chatapp";
	static final String EXECUTIONTRACEDIRECTORY = "executiontrace/";
	static final String DOMIDLISTSDIRECTORY = "domIdLists/";
	static final String OUTPUTDIRECTORY = "/chatapp_assertions/";
	static final String INJECTIONSFOLDER = "chatappInjections/";
	
	public static void main(String[] args) {
		String srcLine = "";
		String funcName = "";
		int lineNo = -1;
		boolean injectMode = true;
		int injectLineNo = 0;
		String injectFuncName = "";
		String injectOrigSrc = "";
		String injectModSrc = "";
		URL = "http://gamma.nic.fi/~jmp/chat/app.html";
		//URL = "http://192.168.1.84/wp/wp-admin/edit-comments.php";
		int numActivations = 150;
		int injStart = 0;
		
		//Redirect System.out to file
		File file = new File("/chatAppInjections/results_150.txt");
		PrintStream printStream = null;
		try {
			printStream = new PrintStream(new FileOutputStream(file));
		}
		catch (FileNotFoundException fnfe) {
			//Do nothing
		}
		//if (printStream != null) {
		//	System.setOut(printStream);
		//}
		
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
					boolean gotOutput8 = false;
					
					int output1 = 0; //line no. of GEBID
					String output2 = ""; //original source of GEBID
					int output3 = 0; //line no. of mutated line
					String output4 = ""; //original source of mutated line
					String output5 = ""; //modified source of mutated line
					String output6 = ""; //name of enclosing function of mutated line
					List<String> output7 = null; //keywords
					int output8 = 0; //injection type
					
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
						else if (nextLine.startsWith("OUTPUT 8")) {
							String typeStr = nextLine.substring(10);
							output8 = Integer.parseInt(typeStr);
							gotOutput8 = true;
						}
					}
					
					if (gotOutput1 && gotOutput2 && gotOutput3 && gotOutput4 && gotOutput5 && gotOutput6 && gotOutput7) {
						//Place InjectionInput in list
						InjectionInput inj = new InjectionInput(output1, output2, output3, output4, output5, output6, output7);
						if (output8 == 1) {
							inj.setType();
						}
						injList.add(inj);
					}
				}
			}
			catch (Exception e) {
				System.err.println("Error: Malformed injection file");
				System.err.println("File name: " + child.getName());
			}
		}
		
		int totalActivated = 0;
		int totalSuccesses = 0;
		int totalFailures = 0;
		int totalInjected = injList.size();
		
		JSModifyProxyPlugin modifier = new JSModifyProxyPlugin(new AstInstrumenter());
		
		for (int i = injStart; i < injList.size(); i++) {
			if (totalActivated == numActivations) break;
			printStream.println("\n");
			printStream.println("Iteration number: " + i);
			printStream.println("Successes So Far: " + totalSuccesses);
			printStream.println("Failures So Far: " + totalFailures);
			printStream.println("Activated So Far: " + totalActivated);
			printStream.println("Injected So Far: " + i);
			
			try {
				config = new CrawljaxConfiguration();
				crawler = new CrawlSpecification(URL);
				
				//The test(s)
				test1();
				
				config.setCrawlSpecification(crawler);
				//config.addPlugin(new CrawlOverview());
				
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
					modifier.setInjectType(injList.get(i).getType());
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
				
				DirectDOMAccess dda = null;
				if (injList.get(i).getType() == 0) {
					//assume GEBID line is mutated here
					dda = new DirectDOMAccess(injList.get(i).getGebidLineNo(), injList.get(i).getModSrcCodeMutated(), injList.get(i).getFuncNameMutated());
				}
				else {
					dda = new DirectDOMAccess(injList.get(i).getGebidLineNo(), injList.get(i).getOrigSrcCodeGebid(), injList.get(i).getFuncNameMutated());
				}
				StringSetExtractor extractor = new StringSetExtractor(JSSOURCEOUTPUTFOLDER, OUTPUTDIRECTORY+outputFolder+EXECUTIONTRACEDIRECTORY,dda);
				/*START INJECT MODE*/
				if (injectMode) {
					extractor.setInject();
					extractor.setInjectLineNo(injList.get(i).getMutatedLineNo());
					extractor.setInjectFuncName(injList.get(i).getFuncNameMutated());
					extractor.setInjectOrigSrc(injList.get(i).getOrigSrcCodeMutated());
					extractor.setInjectModSrc(injList.get(i).getModSrcCodeMutated());
					extractor.setInjectType(injList.get(i).getType());
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
				int foundIndex = -1;
				for (int j = 0; j < fixSuggestions.size(); j++) {
					String suggestion = fixSuggestions.get(j).getMessageStr();
					if (containsKeywords(suggestion, injList.get(i).getKeywordList())) {
						foundMatch = true;
						foundIndex = j;
						break;
					}
				}
				
				if (foundMatch) {
					totalSuccesses++;
					printStream.println("Fix Class: " + fixSuggestions.get(foundIndex).getFixClass().name());
				}
				else {
					totalFailures++;
				}
				
				printStream.println("Number of Fix Suggestions: " + fixSuggestions.size());
				
				totalActivated++;
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
				System.err.println("Error: exception caught");
				//Don't exit
			}
		}
		
		printStream.println("\n");
		printStream.println("Total Successes: " + totalSuccesses);
		printStream.println("Total Failures: " + totalFailures);
		printStream.println("Total Activated: " + totalActivated);
		printStream.println("Total Injected: " + totalInjected);
		
		printStream.close();
		
		return; //Put this here for debugging help
	}
	
	private static void test1() {
		crawler.setMaximumStates(4);
		crawler.setDepth(4);

		crawler.click("INPUT").withAttribute("id", "broadcasting");
		crawler.click("INPUT").withAttribute("id", "clearing");
		
		InputSpecification text = new InputSpecification();
		text.field("myvoice").setValue("rand");
		crawler.setInputSpecification(text);
		
		//Exclude list
		List<String> chatAppExclude = new ArrayList<String>();
		chatAppExclude.add(".*html.*");
		chatAppExclude.add(".*elemental.*");
		chatAppExclude.add(".*qs.*");
		
		excludeList = chatAppExclude;
		
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