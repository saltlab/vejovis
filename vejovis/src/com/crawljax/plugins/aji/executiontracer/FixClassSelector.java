package com.crawljax.plugins.aji.executiontracer;

import java.util.*;

import com.berico.similarity.*;
import com.fixmsg.*;

public class FixClassSelector {
	private List<StringSetLine> strSet;
	private String erroneousID;
	private DirectDOMAccess dda;
	private List<List<DomIdInfo>> domIdStates;
	private int currentState;
	
	private List<StringSetLine> compressedStrSet;
	private boolean compressionDone = false;
	
	List<CandidateFix> candidateFixes;
	List<String> replacementsTried;
	
	public FixClassSelector(List<StringSetLine> _strSet, String _erroneousID, DirectDOMAccess _dda, List<List<DomIdInfo>> _domIdStates, int _currentState) throws Exception {
		this.strSet = _strSet;
		this.erroneousID = _erroneousID;
		this.dda = _dda;
		this.domIdStates = _domIdStates;
		this.currentState = _currentState;
		compressedStrSet = new ArrayList<StringSetLine>();
		candidateFixes = new ArrayList<CandidateFix>();
		replacementsTried = new ArrayList<String>();
		
		//Create compressed version of string set
		strSetCompress(_strSet);
		compressionDone = true;
		fillGaps();
		
		return;
	}
	
	public void chooseFixClasses() throws Exception {
		//Go through all fix classes and try to extract candidate fixes from each one
		
		findFixesNumberSuffixMod();
		findFixesEarlyNodeInsertion();
		findFixesNodeReinsertion();
		findFixesNearMatchDL();
		findFixesNearMatchTree();
		findFixesSamePrefixMod();
	}
	
	private void findFixesNumberSuffixMod() throws Exception {
		//Prerequisite 1: Erroneous ID suffixed by number
		boolean numberFound = false;
		int indexOfBiggestSuffix = -1;
		for (int i = 0; i < erroneousID.length(); i++) {
			String str = erroneousID.substring(i);
			if (strIsInteger(str)) {
				numberFound = true;
				indexOfBiggestSuffix = i;
				break;
			}
		}
		
		if (!numberFound) {
			return;
		}
		
		//Prerequisite #2: Prefix of erroneous ID used by other IDs in the DOM
		//that also have numerical suffixes
		
		//Check all possible prefixes
		List<List<DomIdInfo>> possibleIdsPerPrefix = new ArrayList<List<DomIdInfo>>(); //list of possible replacement IDs per prefix
		boolean possibleMatchFound = false;
		for (int i = indexOfBiggestSuffix; i < erroneousID.length(); i++) {
			List<DomIdInfo> possibleIds = new ArrayList<DomIdInfo>();
			String prefix = erroneousID.substring(0, i);
			
			//See if the prefix appears anywhere in the current state
			List<DomIdInfo> currentStateIds = domIdStates.get(currentState);
			for (int j = 0; j < currentStateIds.size(); j++) {
				DomIdInfo nextIdToCheck = currentStateIds.get(j);
				String idToCheck = nextIdToCheck.getIdStr();
				if (idToCheck.startsWith(prefix) && !idToCheck.equals(prefix)) {
					//Check if idToCheck is suffixed with a number
					String idToCheckSuffix = idToCheck.substring(prefix.length());
					if (strIsInteger(idToCheckSuffix)) {
						possibleIds.add(nextIdToCheck);
						possibleMatchFound = true;
					}
				}
			}
			
			possibleIdsPerPrefix.add(possibleIds);
		}
		
		if (!possibleMatchFound) {
			return;
		}
		
		//PREREQUISITES PASSED AT THIS POINT
		
		//Create candidate fix message(s) for each possible replacement ID found
		
		//Possible Modify, "Ensure Correct", Add, or Remove messages
		for (int i = 0; i < possibleIdsPerPrefix.size(); i++) {
			List<DomIdInfo> nextIdSet = possibleIdsPerPrefix.get(i);
			for (int j = 0; j < nextIdSet.size(); j++) {
				String possibleId = nextIdSet.get(j).getIdStr();
				if (replacementsTried.contains(possibleId)) { //To prevent duplicates
					continue;
				}
				else {
					replacementsTried.add(possibleId);
					craftModifyStrLiteralMessage(possibleId, FixClasses.NUMBER_SUFFIX_MOD);
					craftEnsureCorrectStrLiteralMessage(possibleId, FixClasses.NUMBER_SUFFIX_MOD);
					craftAddStrLiteralMessage(possibleId, FixClasses.NUMBER_SUFFIX_MOD);
					craftRemoveStrLiteralMessage(possibleId, FixClasses.NUMBER_SUFFIX_MOD);
				}
			}
		}
	}
	
	private void findFixesSamePrefixMod() throws Exception {
		//Prerequisite #1: Prefix of erroneous ID is used by other
		//IDs in the DOM
		
		//You should only add IDs with the biggest overall prefix
		List<DomIdInfo> currentStateIds = domIdStates.get(currentState);
		List<DomIdInfo> samePrefixedIds = new ArrayList<DomIdInfo>();
		int biggestOverallPrefix = 0;
		for (int i = 0; i < currentStateIds.size(); i++) {
			DomIdInfo nextDomId = currentStateIds.get(i);
			String possibleId = nextDomId.getIdStr();
			//Find the biggest shared prefix with the ID
			int biggestPrefix = 0;
			for (int j = 1; j <= possibleId.length() && j <= erroneousID.length(); j++) {
				String prefixErroneous = erroneousID.substring(0, j);
				String prefixPossible = possibleId.substring(0, j);
				if (prefixErroneous.equals(prefixPossible)) {
					biggestPrefix++;
				}
				else {
					break;
				}
			}
			
			if (biggestPrefix == 0) {
				continue;
			}
			
			if (biggestPrefix > biggestOverallPrefix) {
				if (!samePrefixedIds.isEmpty()) {
					samePrefixedIds.clear();
				}
				biggestOverallPrefix = biggestPrefix;
				samePrefixedIds.add(nextDomId);
			}
			else if (biggestPrefix == biggestOverallPrefix) {
				samePrefixedIds.add(nextDomId);
			}
			else {
				continue;
			}
		}
		
		if (biggestOverallPrefix == 0) {
			return;
		}
		
		//PREREQUISITES PASSED AT THIS POINT
		
		//Create candidate fix message(s) for each possible replacement ID found
		
		//Possible Modify, "Ensure Correct", Add, or Remove messages
		for (int j = 0; j < samePrefixedIds.size(); j++) {
			String possibleId = samePrefixedIds.get(j).getIdStr();
			if (replacementsTried.contains(possibleId)) { //To prevent duplicates
				continue;
			}
			else {
				replacementsTried.add(possibleId);
				craftModifyStrLiteralMessage(possibleId, FixClasses.SAME_PREFIX_MOD);
				craftEnsureCorrectStrLiteralMessage(possibleId, FixClasses.SAME_PREFIX_MOD);
				craftAddStrLiteralMessage(possibleId, FixClasses.SAME_PREFIX_MOD);
				craftRemoveStrLiteralMessage(possibleId, FixClasses.SAME_PREFIX_MOD);
			}
		}
	}
	
	private void findFixesNearMatchDL() throws Exception {
		//No prerequisites here
		
		//Go through all current IDs and collect all IDs with a
		//Damerau-Levenshtein distance of 2 or smaller
		List<DomIdInfo> currentStateIds = domIdStates.get(currentState);
		List<DomIdInfo> shortEditDistanceIds = new ArrayList<DomIdInfo>();
		for (int i = 0; i < currentStateIds.size(); i++) {
			DomIdInfo nextId = currentStateIds.get(i);
			IDistanceCalculator distanceCalc = new DamerauLevenshteinDistance();
			String idToCompare = nextId.getIdStr();
			int editDistance = distanceCalc.calculate(idToCompare, erroneousID);
			if (editDistance <= 2) {
				shortEditDistanceIds.add(nextId);
			}
		}
		
		//Create candidate fix message(s) for each possible replacement ID found
		
		//Possible Modify, "Ensure Correct", Add, or Remove messages
		for (int j = 0; j < shortEditDistanceIds.size(); j++) {
			String possibleId = shortEditDistanceIds.get(j).getIdStr();
			if (replacementsTried.contains(possibleId)) { //To prevent duplicates
				continue;
			}
			else {
				replacementsTried.add(possibleId);
				craftModifyStrLiteralMessage(possibleId, FixClasses.NEAR_MATCH_DL);
				craftEnsureCorrectStrLiteralMessage(possibleId, FixClasses.NEAR_MATCH_DL);
				craftAddStrLiteralMessage(possibleId, FixClasses.NEAR_MATCH_DL);
				craftRemoveStrLiteralMessage(possibleId, FixClasses.NEAR_MATCH_DL);
			}
		}
	}
	
	private void findFixesNearMatchTree() throws Exception {
		//No prerequisites here
		
		//Go through all current IDs, put them in a list, sort the
		//IDs, then insert them one by one into the IDTree
		List<DomIdInfo> currentStateIds = domIdStates.get(currentState);
		List<DomIdInfo> closelyRelatedIds = new ArrayList<DomIdInfo>();
		
		//Place IDs in separate list
		List<DomIdInfo> stateIdsSorted = new ArrayList<DomIdInfo>();
		for (int i = 0; i < currentStateIds.size(); i++) {
			stateIdsSorted.add(currentStateIds.get(i));
		}
		DomIdInfo dummyErroneousIdInfo = new DomIdInfo(erroneousID, 0, null, null, null, currentState);
		stateIdsSorted.add(dummyErroneousIdInfo);
		Collections.sort(stateIdsSorted);
		
		//Now that the list of IDs (including the erroneous ID) is
		//sorted, start creating the ID tree, rooted at the string ""
		IDTreeNode root = new IDTreeNode(new DomIdInfo("", 0, null, null, null, currentState), false, null);
		for (int i = 0; i < stateIdsSorted.size(); i++) {
			DomIdInfo stateId = stateIdsSorted.get(i);
			insertInIdTree(stateId, root);
		}
		
		//Find the IDTreeNode of the erroneous ID
		IDTreeNode erroneousIDTreeNode = searchIDTreeNode(root);
		if (erroneousIDTreeNode == null) {
			//Something wrong if this point is reached. Just return
			//and let other fix classes be checked
			return;
		}
		
		//Get the parent (if any non-root), children (if any), and one or two closest siblings (if any) of the erroneous ID
		DomIdInfo parentId = erroneousIDTreeNode.getParent().getID(); //parents
		if (!(parentId.getIdStr().equals(""))) {
			closelyRelatedIds.add(parentId);
		}
		List<IDTreeNode> childrenIds = erroneousIDTreeNode.getChildren(); //children
		for (int i = 0; i < childrenIds.size(); i++) {
			DomIdInfo childId = childrenIds.get(i).getID();
			closelyRelatedIds.add(childId);
		}
		List<IDTreeNode> siblingIds = erroneousIDTreeNode.getClosestSiblings(); //closest siblings
		for (int i = 0; i < siblingIds.size(); i++) {
			DomIdInfo siblingId = siblingIds.get(i).getID();
			closelyRelatedIds.add(siblingId);
		}
		
		//Create candidate fix message(s) for each possible replacement ID found
		
		//Possible Modify, "Ensure Correct", Add, or Remove messages
		for (int j = 0; j < closelyRelatedIds.size(); j++) {
			String possibleId = closelyRelatedIds.get(j).getIdStr();
			if (replacementsTried.contains(possibleId)) { //To prevent duplicates
				continue;
			}
			else {
				replacementsTried.add(possibleId);
				craftModifyStrLiteralMessage(possibleId, FixClasses.NEAR_MATCH_TREE);
				craftEnsureCorrectStrLiteralMessage(possibleId, FixClasses.NEAR_MATCH_TREE);
				craftAddStrLiteralMessage(possibleId, FixClasses.NEAR_MATCH_TREE);
				craftRemoveStrLiteralMessage(possibleId, FixClasses.NEAR_MATCH_TREE);
			}
		}
	}
	
	private void findFixesNodeReinsertion() throws Exception {
		//Prerequisite 1: Previous states exist
		if (currentState == 0) {
			return;
		}
		
		//Prerequisite 2: Erroneous ID exists in a previous state
		List<DomIdInfo> matchingIds = new ArrayList<DomIdInfo>();
		for (int i = 0; i < currentState; i++) {
			//Check if erroneous ID appears anywhere in state i
			List<DomIdInfo> state = domIdStates.get(i);
			for (int j = 0; j < state.size(); j++) {
				DomIdInfo idInfo = state.get(j);
				if (idInfo.getIdStr().equals(erroneousID)) {
					matchingIds.add(idInfo);
				}
			}
		}
		
		if (matchingIds.isEmpty()) {
			return;
		}
		
		//PREREQUISITES PASSED AT THIS POINT
		
		//Create candidate fix message(s) for each possible replacement ID found
		
		//Possible Create messages
		List<DomIdInfo> triedSoFar = new ArrayList<DomIdInfo>();
		for (int j = 0; j < matchingIds.size(); j++) {
			DomIdInfo nextIdMatch = matchingIds.get(j);
			//Check if the ID has been tried before
			boolean triedBefore = false;
			for (int k = 0; k < triedSoFar.size(); k++) {
				DomIdInfo prevId = triedSoFar.get(k);
				boolean sameIdStr = prevId.getIdStr().equals(nextIdMatch.getIdStr());
				boolean sameParentXpath = prevId.getParentXpath().equals(nextIdMatch.getParentXpath());
				boolean sameParentId = prevId.getParentIdStr().equals(nextIdMatch.getParentIdStr());
				boolean sameTagName = prevId.getTagName().equals(nextIdMatch.getTagName());
				
				if (sameIdStr && sameParentXpath && sameParentId && sameTagName) {
					triedBefore = true;
					break;
				}
			}
			if (triedBefore) {
				continue;
			}
			else {
				triedSoFar.add(nextIdMatch);
				craftCreateNodeMessage(nextIdMatch, FixClasses.NODE_REINSERTION);
			}
		}
	}
	
	private void findFixesEarlyNodeInsertion() throws Exception {
		//Prerequisite 1: Future states exist
		if (currentState == domIdStates.size()-1) {
			return;
		}
		
		//Prerequisite 2: Erroneous ID exists in a future state
		List<DomIdInfo> matchingIds = new ArrayList<DomIdInfo>();
		for (int i = currentState + 1; i < domIdStates.size(); i++) {
			//Check if erroneous ID appears anywhere in state i
			List<DomIdInfo> state = domIdStates.get(i);
			for (int j = 0; j < state.size(); j++) {
				DomIdInfo idInfo = state.get(j);
				if (idInfo.getIdStr().equals(erroneousID)) {
					matchingIds.add(idInfo);
				}
			}
		}
		
		if (matchingIds.isEmpty()) {
			return;
		}
		
		//PREREQUISITES PASSED AT THIS POINT
		
		//Create candidate fix message(s) for each possible replacement ID found
		
		//Possible Create messages
		List<DomIdInfo> triedSoFar = new ArrayList<DomIdInfo>();
		for (int j = 0; j < matchingIds.size(); j++) {
			DomIdInfo nextIdMatch = matchingIds.get(j);
			//Check if the ID has been tried before
			boolean triedBefore = false;
			for (int k = 0; k < triedSoFar.size(); k++) {
				DomIdInfo prevId = triedSoFar.get(k);
				boolean sameIdStr = prevId.getIdStr().equals(nextIdMatch.getIdStr());
				boolean sameParentXpath = prevId.getParentXpath().equals(nextIdMatch.getParentXpath());
				boolean sameParentId = prevId.getParentIdStr().equals(nextIdMatch.getParentIdStr());
				boolean sameTagName = prevId.getTagName().equals(nextIdMatch.getTagName());
				
				if (sameIdStr && sameParentXpath && sameParentId && sameTagName) {
					triedBefore = true;
					break;
				}
			}
			if (triedBefore) {
				continue;
			}
			else {
				triedSoFar.add(nextIdMatch);
				craftCreateNodeMessage(nextIdMatch, FixClasses.EARLY_NODE_INSERTION);
			}
		}
	}
	
	private void craftModifyStrLiteralMessage(String possibleReplacementID, FixClasses fc) throws Exception {
		//Go through each entry in the compressed string set until
		//a point is reached where the ID concatenated so far does
		//not match the possible replacement ID. If the point reached
		//is a string literal, create a modify message.
		
		//ASSUMPTION: Only one string set entry has a mistake!
		
		String idSoFar = "";
		boolean lastIsGap = false;
		int lastMatchingIndex = 0;
		for (int i = 0; i < compressedStrSet.size(); i++) {
			StringSetLine nextSSL = compressedStrSet.get(i);
			if (nextSSL.getType() == StringSetLine.Type.STRING_LITERAL.ordinal()) {
				idSoFar += nextSSL.getStrLiteral();
				if (!possibleReplacementID.startsWith(idSoFar)) {
					//Construct the end of the erroneous ID string
					String endStr = "";
					boolean lastIsGapEnd = false;
					for (int j = i + 1; j < compressedStrSet.size(); j++) {
						StringSetLine nextSSLEnd = compressedStrSet.get(j);
						if (nextSSLEnd.getType() == StringSetLine.Type.STRING_LITERAL.ordinal()) {
							endStr += nextSSLEnd.getStrLiteral();
							lastIsGapEnd = false;
						}
						else if (nextSSLEnd.getType() == StringSetLine.Type.GAP.ordinal() && lastIsGapEnd) {
							continue;
						}
						else if (nextSSLEnd.getType() == StringSetLine.Type.GAP.ordinal()) {
							endStr += nextSSLEnd.getStrLiteral();
							lastIsGapEnd = true;
						}
						else {
							System.err.println("Error: Invalid StringSetLine type");
							throw new Exception();
							//System.exit(-1);
						}
					}
					
					int matchingEndIndex = possibleReplacementID.lastIndexOf(endStr);
					
					if (matchingEndIndex != -1) {
						String newStrSuggestion = possibleReplacementID.substring(lastMatchingIndex, matchingEndIndex);
						
						ModifyStrLiteralMessage msg = new ModifyStrLiteralMessage(nextSSL.getLineNo(), nextSSL.getFuncName(), nextSSL.getStrLiteral(), newStrSuggestion);
						msg.createMessage();
						CandidateFix fix = new CandidateFix(msg, fc);
						candidateFixes.add(fix);
					}
					
					return;
				}
				else if (idSoFar.equals("")) {
					lastMatchingIndex = 0;
				}
				else {
					lastMatchingIndex = idSoFar.length();
				}
				lastIsGap = false;
			}
			else if (nextSSL.getType() == StringSetLine.Type.GAP.ordinal() && lastIsGap) {
				continue;
			}
			else if (nextSSL.getType() == StringSetLine.Type.GAP.ordinal()) {
				idSoFar += nextSSL.getStrLiteral();
				if (!possibleReplacementID.startsWith(idSoFar)) {
					return;
				}
				else if (idSoFar.equals("")) {
					lastMatchingIndex = 0;
				}
				else {
					lastMatchingIndex = idSoFar.length();
				}
				lastIsGap = true;
			}
			else {
				System.err.println("Error: Invalid StringSetLine type");
				throw new Exception();
				//System.exit(-1);
			}
		}
	}
	
	private void craftEnsureCorrectStrLiteralMessage(String possibleReplacementID, FixClasses fc) throws Exception {
		//Go through each entry in the compressed string set until
		//a point is reached where the ID concatenated so far does
		//not match the possible replacement ID. If the point reached
		//is a gap, create an "ensure correct" message.
		
		//ASSUMPTION: Only one string set entry has a mistake!
		
		String idSoFar = "";
		boolean lastIsGap = false;
		int lastMatchingIndex = 0;
		for (int i = 0; i < compressedStrSet.size(); i++) {
			StringSetLine nextSSL = compressedStrSet.get(i);
			if (nextSSL.getType() == StringSetLine.Type.STRING_LITERAL.ordinal()) {
				idSoFar += nextSSL.getStrLiteral();
				if (!possibleReplacementID.startsWith(idSoFar)) {
					return;
				}
				else if (idSoFar.equals("")) {
					lastMatchingIndex = 0;
				}
				else {
					lastMatchingIndex = idSoFar.length();
				}
				lastIsGap = false;
			}
			else if (nextSSL.getType() == StringSetLine.Type.GAP.ordinal() && lastIsGap) {
				continue;
			}
			else if (nextSSL.getType() == StringSetLine.Type.GAP.ordinal()) {
				idSoFar += nextSSL.getStrLiteral();
				if (!possibleReplacementID.startsWith(idSoFar)) {
					//Construct the end of the erroneous ID string
					String endStr = "";
					boolean lastIsGapEnd = true;
					int countNextImmediateGaps = 0; //number of consecutive gaps immediately following this current one
					boolean strLiteralFound = false;
					for (int j = i + 1; j < compressedStrSet.size(); j++) {
						StringSetLine nextSSLEnd = compressedStrSet.get(j);
						if (nextSSLEnd.getType() == StringSetLine.Type.STRING_LITERAL.ordinal()) {
							endStr += nextSSLEnd.getStrLiteral();
							lastIsGapEnd = false;
							if (!strLiteralFound) {
								strLiteralFound = true;
							}
						}
						else if (nextSSLEnd.getType() == StringSetLine.Type.GAP.ordinal() && lastIsGapEnd) {
							if (!strLiteralFound) {
								countNextImmediateGaps++;
							}
							continue;
						}
						else if (nextSSLEnd.getType() == StringSetLine.Type.GAP.ordinal()) {
							endStr += nextSSLEnd.getStrLiteral();
							lastIsGapEnd = true;
						}
						else {
							System.err.println("Error: Invalid StringSetLine type");
							throw new Exception();
							//System.exit(-1);
						}
					}
					
					int matchingEndIndex = possibleReplacementID.lastIndexOf(endStr);
					
					if (matchingEndIndex != -1) {
						//Create a candidate fix for each consecutive gap
						String newStrSuggestion = possibleReplacementID.substring(lastMatchingIndex, matchingEndIndex);
						
						for (int j = 0; j <= countNextImmediateGaps; j++) {
							StringSetLine nextSSLToFix = compressedStrSet.get(i + j);
							EnsureCorrectStrLiteralMessage msg = new EnsureCorrectStrLiteralMessage(nextSSLToFix.getLineNo(), nextSSLToFix.getFuncName(), nextSSLToFix.getVarIdent(), newStrSuggestion);
							msg.createMessage();
							CandidateFix fix = new CandidateFix(msg, fc);
							candidateFixes.add(fix);
							
							//Create a Pass Parameter message if the SSL has a corresponding
							//(possibly) bad function call
							if (nextSSLToFix.gapHasPossiblyBadFuncCall()) {
								PassParamMessage ppMsg = new PassParamMessage(nextSSLToFix.getFuncCallLineNo(), nextSSLToFix.getFuncCallfname(), nextSSLToFix.getFuncCallparamNum(), nextSSLToFix.getFuncName(), newStrSuggestion);
								ppMsg.createMessage();
								CandidateFix ppFix = new CandidateFix(ppMsg, fc);
								candidateFixes.add(ppFix);
							}
						}
					}
					
					return;
				}
				else if (idSoFar.equals("")) {
					lastMatchingIndex = 0;
				}
				else {
					lastMatchingIndex = idSoFar.length();
				}
				lastIsGap = true;
			}
			else {
				System.err.println("Error: Invalid StringSetLine type");
				throw new Exception();
				//System.exit(-1);
			}
		}
	}
	
	private void craftAddStrLiteralMessage(String possibleReplacementID, FixClasses fc) throws Exception {
		//At each entry of the compressed string set, check if 
		//possibleReplacements is prefixed by the id up to that
		//entry, and is suffixed by the string formed by the rest
		//of the entries. If so, an add message must be included
		
		//ASSUMPTION: Only one string set entry has a mistake!
		
		String idSoFar = "";
		boolean lastIsGap = false;
		int lastMatchingIndex = 0;
		for (int i = 0; i < compressedStrSet.size(); i++) {
			StringSetLine nextSSL = compressedStrSet.get(i);
			if (nextSSL.getType() == StringSetLine.Type.STRING_LITERAL.ordinal()) {
				idSoFar += nextSSL.getStrLiteral();
				if (!possibleReplacementID.startsWith(idSoFar)) {
					return;
				}
				else if (idSoFar.equals("")) {
					lastMatchingIndex = 0;
				}
				else {
					lastMatchingIndex = idSoFar.length();
				}
				lastIsGap = false;
			}
			else if (nextSSL.getType() == StringSetLine.Type.GAP.ordinal() && lastIsGap) {
				continue;
			}
			else if (nextSSL.getType() == StringSetLine.Type.GAP.ordinal()) {
				idSoFar += nextSSL.getStrLiteral();
				if (!possibleReplacementID.startsWith(idSoFar)) {
					return;
				}
				else if (idSoFar.equals("")) {
					lastMatchingIndex = 0;
				}
				else {
					lastMatchingIndex = idSoFar.length();
				}
				lastIsGap = true;
			}
			else {
				System.err.println("Error: Invalid StringSetLine type");
				throw new Exception();
				//System.exit(-1);
			}
			
			String endStr = "";
			boolean lastIsGapEnd = lastIsGap;
			int countNextImmediateGaps = 0; //number of consecutive gaps immediately following this current one
			boolean strLiteralFound = false;
			for (int j = i + 1; j < compressedStrSet.size(); j++) {
				StringSetLine nextSSLEnd = compressedStrSet.get(j);
				if (nextSSLEnd.getType() == StringSetLine.Type.STRING_LITERAL.ordinal()) {
					endStr += nextSSLEnd.getStrLiteral();
					lastIsGapEnd = false;
					if (!strLiteralFound) {
						strLiteralFound = true;
					}
				}
				else if (nextSSLEnd.getType() == StringSetLine.Type.GAP.ordinal() && lastIsGapEnd) {
					if (!strLiteralFound) {
						countNextImmediateGaps++;
					}
					continue;
				}
				else if (nextSSLEnd.getType() == StringSetLine.Type.GAP.ordinal()) {
					endStr += nextSSLEnd.getStrLiteral();
					lastIsGapEnd = true;
				}
				else {
					System.err.println("Error: Invalid StringSetLine type");
					throw new Exception();
					//System.exit(-1);
				}
			}
			
			int matchingEndIndex = possibleReplacementID.lastIndexOf(endStr);
			
			if (matchingEndIndex != -1) {
				if (matchingEndIndex > lastMatchingIndex) {
					//Get the string in between
					String inBetweenStr = possibleReplacementID.substring(lastMatchingIndex, matchingEndIndex);
					
					if (lastIsGap) {
						for (int j = 0; j <= countNextImmediateGaps; j++) {
							StringSetLine nextSSLToFix = compressedStrSet.get(i + j);
							AddStrLiteralMessage msg = new AddStrLiteralMessage(nextSSLToFix.getLineNo(), nextSSLToFix.getFuncName(), inBetweenStr, dda.getLineNo(), dda.funcName(), nextSSLToFix.getVarIdent());
							msg.createMessage();
							CandidateFix fix = new CandidateFix(msg, fc);
							candidateFixes.add(fix);
						}
					}
					else {
						String expr = "\"" + nextSSL.getStrLiteral() + "\"";
						AddStrLiteralMessage msg = new AddStrLiteralMessage(nextSSL.getLineNo(), nextSSL.getFuncName(), inBetweenStr, dda.getLineNo(), dda.funcName(), expr);
						msg.createMessage();
						CandidateFix fix = new CandidateFix(msg, fc);
						candidateFixes.add(fix);
					}
				}
			}
		}
	}
	
	private void craftRemoveStrLiteralMessage(String possibleReplacementID, FixClasses fc) throws Exception {
		//At each entry of the compressed string set, check if 
		//possibleReplacements is composed of the id up to the
		//previous entry and the string formed by the rest
		//of the entries. If so, a remove message must be included,
		//suggesting that the current entry must be removed
		
		//ASSUMPTION: Only one string set entry has a mistake!
		
		String idSoFar = "";
		String prevIdSoFar = "";
		boolean lastIsGap = false;
		int lastMatchingIndex = 0;
		for (int i = 0; i < compressedStrSet.size(); i++) {
			prevIdSoFar = idSoFar;
			StringSetLine nextSSL = compressedStrSet.get(i);
			if (nextSSL.getType() == StringSetLine.Type.STRING_LITERAL.ordinal()) {
				idSoFar += nextSSL.getStrLiteral();
				if (!possibleReplacementID.startsWith(idSoFar)) {
					return;
				}
				else if (idSoFar.equals("")) {
					lastMatchingIndex = 0;
				}
				else {
					lastMatchingIndex = idSoFar.length();
				}
				lastIsGap = false;
			}
			else if (nextSSL.getType() == StringSetLine.Type.GAP.ordinal() && lastIsGap) {
				continue;
			}
			else if (nextSSL.getType() == StringSetLine.Type.GAP.ordinal()) {
				idSoFar += nextSSL.getStrLiteral();
				if (!possibleReplacementID.startsWith(idSoFar)) {
					return;
				}
				else if (idSoFar.equals("")) {
					lastMatchingIndex = 0;
				}
				else {
					lastMatchingIndex = idSoFar.length();
				}
				lastIsGap = true;
			}
			else {
				System.err.println("Error: Invalid StringSetLine type");
				throw new Exception();
				//System.exit(-1);
			}
			
			String endStr = "";
			boolean lastIsGapEnd = lastIsGap;
			int countNextImmediateGaps = 0; //number of consecutive gaps immediately following this current one
			boolean strLiteralFound = false;
			for (int j = i + 1; j < compressedStrSet.size(); j++) {
				StringSetLine nextSSLEnd = compressedStrSet.get(j);
				if (nextSSLEnd.getType() == StringSetLine.Type.STRING_LITERAL.ordinal()) {
					endStr += nextSSLEnd.getStrLiteral();
					lastIsGapEnd = false;
					if (!strLiteralFound) {
						strLiteralFound = true;
					}
				}
				else if (nextSSLEnd.getType() == StringSetLine.Type.GAP.ordinal() && lastIsGapEnd) {
					if (!strLiteralFound) {
						countNextImmediateGaps++;
					}
					continue;
				}
				else if (nextSSLEnd.getType() == StringSetLine.Type.GAP.ordinal()) {
					endStr += nextSSLEnd.getStrLiteral();
					lastIsGapEnd = true;
				}
				else {
					System.err.println("Error: Invalid StringSetLine type");
					throw new Exception();
					//System.exit(-1);
				}
			}

			if (possibleReplacementID.equals(prevIdSoFar + endStr)) {
				if (lastIsGap) {
					for (int j = 0; j <= countNextImmediateGaps; j++) {
						StringSetLine nextSSLToFix = compressedStrSet.get(i + j);
						RemoveStrLiteralMessage msg = new RemoveStrLiteralMessage(nextSSLToFix.getLineNo(), nextSSLToFix.getFuncName(), nextSSLToFix.getVarIdent());
						msg.createMessage();
						CandidateFix fix = new CandidateFix(msg, fc);
						candidateFixes.add(fix);
					}
				}
				else {
					String expr = "\"" + nextSSL.getStrLiteral() + "\"";
					RemoveStrLiteralMessage msg = new RemoveStrLiteralMessage(nextSSL.getLineNo(), nextSSL.getFuncName(), expr);
					msg.createMessage();
					CandidateFix fix = new CandidateFix(msg, fc);
					candidateFixes.add(fix);
				}
			}
		}
	}
	
	private void craftCreateNodeMessage(DomIdInfo domId, FixClasses fc) {
		//The message would consist of the element's tag name and ID, the
		//parent's xpath/ID, and the direct DOM access line number and
		//enclosing function
		String tagName = domId.getTagName();
		String idStr = domId.getIdStr();
		String parentId = domId.getParentIdStr();
		String parentXpath = domId.getParentXpath();
		
		if (parentId.equals("")) {
			parentId = null;
		}
		
		CreateNodeMessage msg = new CreateNodeMessage(dda.getLineNo(), dda.funcName(), tagName, idStr, parentId, parentXpath);
		msg.createMessage();
		CandidateFix fix = new CandidateFix(msg, fc);
		candidateFixes.add(fix);
	}
	
	private void strSetCompress(List<StringSetLine> _strSet) throws Exception {
		if (compressionDone) {
			System.err.println("Error: String set compression done twice");
			throw new Exception();
			//System.exit(-1);
		}
		
		//Compress
		for (int i = 0; i < _strSet.size(); i++) {
			StringSetLine next = _strSet.get(i);
			if (next.getType() == StringSetLine.Type.STRING_SET_LINE_LIST.ordinal() && !(next.getSSLL().isEmpty())) {
				strSetCompress(next.getSSLL());
			}
			else if (next.getType() == StringSetLine.Type.STRING_SET_LINE_LIST.ordinal() && next.getSSLL().isEmpty()) {
				next.setType(StringSetLine.Type.GAP.ordinal());
				compressedStrSet.add(next);
			}
			else if (next.getType() == StringSetLine.Type.STRING_LITERAL.ordinal() || next.getType() == StringSetLine.Type.GAP.ordinal()) {
				compressedStrSet.add(next);
			}
		}
	}
	
	private void fillGaps() throws Exception {
		//TODO: Try to find more efficient way of doing this
		if (!compressionDone) {
			System.err.println("Error: String set gap filling done before compression");
			throw new Exception();
			//System.exit(-1);
		}
		
		int consecutiveGapsSoFar = 0;
		boolean previousIsGap = false;
		int mostRecentConsecutiveGaps = 0;
		
		String erroneousIDSoFar = "";
		
		List<Integer> possibleStrsCounter = new ArrayList<Integer>(); //used when traversing
		List<Integer> possibleStrsOrigIndex = new ArrayList<Integer>(); //index of list of possible strings in string set
		List<Integer> listNumIterationsToInc = new ArrayList<Integer>(); //how many iterations before corresponding possibleStrsCounter entry is incremented?
		
		for (int i = 0; i < compressedStrSet.size(); i++) {
			StringSetLine next = compressedStrSet.get(i);
			if (next.getType() == StringSetLine.Type.GAP.ordinal()) {
				consecutiveGapsSoFar++;
				if (consecutiveGapsSoFar == 1) {
					possibleStrsOrigIndex.add(i);
					possibleStrsCounter.add(0);
					
					listNumIterationsToInc.add(1);
				}
				previousIsGap = true;
			}
			else if (next.getType() == StringSetLine.Type.STRING_LITERAL.ordinal()) {
				String currStrLiteral = next.getStrLiteral();
				if (previousIsGap) {
					mostRecentConsecutiveGaps = consecutiveGapsSoFar;
					consecutiveGapsSoFar = 0;
					
					//Determine all possible strings for the gap
					int nextGapEntryToIncrement = possibleStrsCounter.size()-2;
					for (int j = 0; j < possibleStrsCounter.size()-1; j++) { //not including latest gap
						//Initialize possibleStrsCounter
						possibleStrsCounter.set(j, 0);
					}
					int multiplier = 1;
					int previousSize = 1;
					for (int j = possibleStrsCounter.size()-2; j >= 0; j--) {
						listNumIterationsToInc.set(j, multiplier*previousSize);
						multiplier = multiplier*previousSize;
						previousSize = compressedStrSet.get(possibleStrsOrigIndex.get(j)).possibleStrs.size();
					}
					multiplier = multiplier*previousSize;
					int nextSecondaryGapEntryToIncrement = possibleStrsCounter.size()-3;
					int prevGapOrigIndex = possibleStrsOrigIndex.get(possibleStrsOrigIndex.size()-1);
					int iterationNumber = 0;
					while (true) {
						iterationNumber++;
						if (iterationNumber > multiplier) { //multiplier holds the last multiplication indicating number of iterations
							break;
						}
						String idSoFar = "";
						int nextGapEntry = 0; //refers to possibleStrsCounter index
						boolean lastConcatenatedIsGap = false;
						for (int j = 0; j < prevGapOrigIndex; j++) {
							StringSetLine nextSS = compressedStrSet.get(j);
							if (nextSS.getType() == StringSetLine.Type.STRING_LITERAL.ordinal()) {
								idSoFar += nextSS.getStrLiteral();
								lastConcatenatedIsGap = false;
							}
							else if (nextSS.getType() == StringSetLine.Type.GAP.ordinal() && lastConcatenatedIsGap) {
								continue;
							}
							else if (nextSS.getType() == StringSetLine.Type.GAP.ordinal()) {
								idSoFar += nextSS.possibleStrs.get(possibleStrsCounter.get(nextGapEntry));
								nextGapEntry++;
								lastConcatenatedIsGap = true;
							}
							else {
								System.err.println("Error: Compressed string set entry neither gap nor string literal");
								throw new Exception();
								//System.exit(-1);
							}
						}
						
						//Find the appropriate string for the most recent gap
						if (erroneousID.startsWith(idSoFar)) {
							if (currStrLiteral.equals("")) {
								for (int k = prevGapOrigIndex; k < i; k++) {
									compressedStrSet.get(k).possibleStrs.add("");
								}
								for (int j = 0; j < erroneousID.length() - idSoFar.length(); j++) {
									for (int k = prevGapOrigIndex; k < i; k++) {
										String inBetweenString = erroneousID.substring(idSoFar.length(), idSoFar.length()+1+j);
										compressedStrSet.get(k).possibleStrs.add(inBetweenString);
									}
								}
								continue;
							}
							//Find first instance of current string literal,
							//starting from the end of idSoFar
							int idSoFarIdx = erroneousID.indexOf(currStrLiteral, idSoFar.length());
							if (idSoFarIdx != -1) {
								String inBetweenString = erroneousID.substring(idSoFar.length(),idSoFarIdx);
								//Add this string to each possibleStr entry of each list in the current gap
								for (int j = prevGapOrigIndex; j < i; j++) {
									compressedStrSet.get(j).possibleStrs.add(inBetweenString);
								}
								
								//Any other matches?
								while (true) {
									if (idSoFarIdx + currStrLiteral.length() >= erroneousID.length()) {
										break;
									}
									idSoFarIdx = erroneousID.indexOf(currStrLiteral, idSoFarIdx+currStrLiteral.length());
									if (idSoFarIdx != -1) {
										inBetweenString = erroneousID.substring(idSoFar.length(),idSoFarIdx);
										//Add this string to each possibleStr entry of each list in the current gap
										for (int j = prevGapOrigIndex; j < i; j++) {
											compressedStrSet.get(j).possibleStrs.add(inBetweenString);
										}
									}
									else {
										break;
									}
								}
							}
						}
						
						//Increment counters in possibleStrsCounter
						for (int j = 0; j < possibleStrsCounter.size() - 1; j++) {
							int numToInc = listNumIterationsToInc.get(j);
							int currCtrValue = possibleStrsCounter.get(j);
							if ((iterationNumber % numToInc) == 0) {
								int size = compressedStrSet.get(possibleStrsOrigIndex.get(j)).possibleStrs.size();
								int newCtrValue = (currCtrValue + 1) % size;
								possibleStrsCounter.set(j, newCtrValue);
							}
						}
						
					}
				}
				
				previousIsGap = false;
			}
			else {
				System.err.println("Error: Compressed string set entry neither gap nor string literal");
				throw new Exception();
				//System.exit(-1);
			}
		}
		
		//Account for the case where last entry/entries is/are gap(s)
		if (previousIsGap) {
			mostRecentConsecutiveGaps = consecutiveGapsSoFar;
			consecutiveGapsSoFar = 0;
			
			//Determine all possible strings for the gap
			int nextGapEntryToIncrement = possibleStrsCounter.size()-2;
			for (int j = 0; j < possibleStrsCounter.size()-1; j++) { //not including latest gap
				//Initialize possibleStrsCounter
				possibleStrsCounter.set(j, 0);
			}
			int multiplier = 1;
			int previousSize = 1;
			for (int j = possibleStrsCounter.size()-2; j >= 0; j--) {
				listNumIterationsToInc.set(j, multiplier*previousSize);
				multiplier = multiplier*previousSize;
				previousSize = compressedStrSet.get(possibleStrsOrigIndex.get(j)).possibleStrs.size();
			}
			multiplier = multiplier*previousSize;
			int nextSecondaryGapEntryToIncrement = possibleStrsCounter.size()-3;
			int prevGapOrigIndex = possibleStrsOrigIndex.get(possibleStrsOrigIndex.size()-1);
			int iterationNumber = 0;
			while (true) {
				iterationNumber++;
				if (iterationNumber > multiplier) { //multiplier holds the last multiplication indicating number of iterations
					break;
				}
				String idSoFar = "";
				int nextGapEntry = 0; //refers to possibleStrsCounter index
				boolean lastConcatenatedIsGap = false;
				for (int j = 0; j < prevGapOrigIndex; j++) {
					StringSetLine nextSS = compressedStrSet.get(j);
					if (nextSS.getType() == StringSetLine.Type.STRING_LITERAL.ordinal()) {
						idSoFar += nextSS.getStrLiteral();
						lastConcatenatedIsGap = false;
					}
					else if (nextSS.getType() == StringSetLine.Type.GAP.ordinal() && lastConcatenatedIsGap) {
						continue;
					}
					else if (nextSS.getType() == StringSetLine.Type.GAP.ordinal()) {
						idSoFar += nextSS.possibleStrs.get(possibleStrsCounter.get(nextGapEntry));
						nextGapEntry++;
						lastConcatenatedIsGap = true;;
					}
					else {
						System.err.println("Error: Compressed string set entry neither gap nor string literal");
						throw new Exception();
						//System.exit(-1);
					}
				}
				
				//Find the appropriate string for the most recent gap
				if (erroneousID.startsWith(idSoFar)) {
					//Find first instance of current string literal,
					//starting from the end of idSoFar
					String inBetweenString = erroneousID.substring(idSoFar.length());
					//Add this string to each possibleStr entry of each list in the current gap
					for (int j = prevGapOrigIndex; j < compressedStrSet.size(); j++) {
						compressedStrSet.get(j).possibleStrs.add(inBetweenString);
					}
				}
				
				//Increment counters in possibleStrsCounter
				for (int j = 0; j < possibleStrsCounter.size() - 1; j++) {
					int numToInc = listNumIterationsToInc.get(j);
					int currCtrValue = possibleStrsCounter.get(j);
					if ((iterationNumber % numToInc) == 0) {
						int size = compressedStrSet.get(possibleStrsOrigIndex.get(j)).possibleStrs.size();
						int newCtrValue = (currCtrValue + 1) % size;
						possibleStrsCounter.set(j, newCtrValue);
					}
				}
			}
		}
		
		//Check all possible IDs. First match to erroneous ID
		//is taken
		mostRecentConsecutiveGaps = consecutiveGapsSoFar;
		consecutiveGapsSoFar = 0;
		
		//Determine all possible strings for the gap
		int nextGapEntryToIncrement = possibleStrsCounter.size()-1;
		for (int j = 0; j < possibleStrsCounter.size(); j++) { //not including latest gap
			//Initialize possibleStrsCounter
			possibleStrsCounter.set(j, 0);
		}
		int multiplier = 1;
		int previousSize = 1;
		for (int j = possibleStrsCounter.size()-1; j >= 0; j--) {
			listNumIterationsToInc.set(j, multiplier*previousSize);
			multiplier = multiplier*previousSize;
			previousSize = compressedStrSet.get(possibleStrsOrigIndex.get(j)).possibleStrs.size();
		}
		multiplier = multiplier*previousSize;
		int nextSecondaryGapEntryToIncrement = possibleStrsCounter.size()-2;
		//int prevGapOrigIndex = possibleStrsOrigIndex.get(possibleStrsOrigIndex.size()-1);
		int iterationNumber = 0;
		while (true) {
			iterationNumber++;
			if (iterationNumber > multiplier) { //multiplier holds the last multiplication indicating number of iterations
				break;
			}
			String idSoFar = "";
			int nextGapEntry = 0; //refers to possibleStrsCounter index
			boolean lastConcatenatedIsGap = false;
			for (int j = 0; j < compressedStrSet.size(); j++) {
				StringSetLine nextSS = compressedStrSet.get(j);
				if (nextSS.getType() == StringSetLine.Type.STRING_LITERAL.ordinal()) {
					idSoFar += nextSS.getStrLiteral();
					lastConcatenatedIsGap = false;
				}
				else if (nextSS.getType() == StringSetLine.Type.GAP.ordinal() && lastConcatenatedIsGap) {
					continue;
				}
				else if (nextSS.getType() == StringSetLine.Type.GAP.ordinal()) {
					idSoFar += nextSS.possibleStrs.get(possibleStrsCounter.get(nextGapEntry));
					nextGapEntry++;
					lastConcatenatedIsGap = true;
				}
				else {
					System.err.println("Error: Compressed string set entry neither gap nor string literal");
					throw new Exception();
					//System.exit(-1);
				}
			}
			
			//Check if the corresponding ID matches the erroneous ID
			if (erroneousID.equals(idSoFar)) {				
				//Set the gaps with the corresponding string literals
				nextGapEntry = 0;
				boolean lastEntryIsGap = false;
				String latestSetStrLiteral = "";
				for (int j = 0; j < compressedStrSet.size(); j++) {
					StringSetLine nextSS = compressedStrSet.get(j);
					if (nextSS.getType() == StringSetLine.Type.GAP.ordinal() && lastEntryIsGap) {
						nextSS.setStrLiteral(latestSetStrLiteral);
					}
					else if (nextSS.getType() == StringSetLine.Type.GAP.ordinal()) {
						latestSetStrLiteral = nextSS.possibleStrs.get(possibleStrsCounter.get(nextGapEntry));
						nextSS.setStrLiteral(latestSetStrLiteral);
						nextGapEntry++;
						lastEntryIsGap = true;
					}
					else {
						lastEntryIsGap = false;
					}
				}
				
				break;
			}
			
			//Increment counters in possibleStrsCounter
			for (int j = 0; j < possibleStrsCounter.size(); j++) {
				int numToInc = listNumIterationsToInc.get(j);
				int currCtrValue = possibleStrsCounter.get(j);
				if ((iterationNumber % numToInc) == 0) {
					int size = compressedStrSet.get(possibleStrsOrigIndex.get(j)).possibleStrs.size();
					int newCtrValue = (currCtrValue + 1) % size;
					possibleStrsCounter.set(j, newCtrValue);
				}
			}
			
		}
	}
	
	private boolean strIsInteger(String s) {
		try {
			Integer.parseInt(s);
			return true;
		}
		catch (NumberFormatException nfe) {
			return false;
		}
	}
	
	public List<CandidateFix> getCandidateFixes() {
		return candidateFixes;
	}
	
	private boolean insertInIdTree(DomIdInfo stateId, IDTreeNode root) {
		//First, check if stateId is prefixed by root
		String stateIdStr = stateId.getIdStr();
		String rootStr = root.getID().getIdStr();
		if (stateIdStr.startsWith(rootStr)) {
			//Check children first
			boolean inserted = false;
			List<IDTreeNode> children = root.getChildren();
			for (int i = 0; i < children.size(); i++) {
				IDTreeNode child = children.get(i);
				inserted = insertInIdTree(stateId, child);
				
				if (inserted) {
					return true;
				}
			}
			
			if (!inserted) {
				//Insert as a child of this node
				boolean isErroneousId = false;
				if (stateIdStr.equals(erroneousID)) {
					isErroneousId = true;
				}
				IDTreeNode newNode = new IDTreeNode(stateId, isErroneousId, root);
				root.getChildren().add(newNode);
				return true;
			}
		}
		
		return false;
	}
	
	private IDTreeNode searchIDTreeNode(IDTreeNode root) {
		if (root.isErroneousID()) {
			return root;
		}
		else {
			//Find in children
			List<IDTreeNode> children = root.getChildren();
			for (int i = 0; i < children.size(); i++) {
				IDTreeNode child = children.get(i);
				IDTreeNode result = searchIDTreeNode(child);
				
				if (result != null) {
					return result;
				}
			}
			
			//If this point is reached, then it is not in any of the children
		}
		
		return null;
	}
}