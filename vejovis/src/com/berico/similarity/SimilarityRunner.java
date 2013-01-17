package com.berico.similarity;

import com.berico.similarity.DamerauLevenshteinDistance.DameauLevenshteinDistanceResult;

/**
 * @author Richard Clayton (Berico Technologies)
 *
 */
public class SimilarityRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {		
		ISimilarityCalculator similarity = new CosineSimilarity();
		String one = "apple";
		String two = "applet";
		
		double percentageSimilar = similarity.calculate(one, two) * 100;
	    System.out.println(String.format("%s and %s are %s%% similar", one, two, percentageSimilar));
	    
	    IDistanceCalculator distanceCalc = new DamerauLevenshteinDistance();
	    
	    String distOne = "snapple";
	    String distTwo = "apple";
	    
	    int editDistance = distanceCalc.calculate(distOne, distTwo);
	    
	    System.out.println(
	      String.format("The distance between %s and %s is %s",
	    		  distOne, distTwo, editDistance));
	    
	    
	    DamerauLevenshteinDistance distanceCalc2 = new DamerauLevenshteinDistance();
	    
	    DameauLevenshteinDistanceResult result = distanceCalc2.calculateAndReturnFullResult(distOne, distTwo);
	    
	    System.out.println(result);
	}

}
