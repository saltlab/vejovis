package com.berico.similarity;

import java.util.Collection;

public class VectorMath {

	/**
	 * Calculate the Dot Product (inner product) of two vectors
	 * @param vectorOne Vector
	 * @param vectorTwo Vector
	 * @return Dot Product
	 * @throws VectorMathException Thrown if vectors are not of equal length
	 */
	public static int dotp(Collection<Integer> vectorOne, Collection<Integer> vectorTwo) throws VectorMathException {
		return dotp(vectorOne.toArray(new Integer[0]), vectorTwo.toArray(new Integer[0]));
	}

	/**
	 * Calculate the Dot Product (inner product) of two vectors
	 * @param vectorOne Vector
	 * @param vectorTwo Vector
	 * @return Dot Product
	 * @throws VectorMathException Thrown if vectors are not of equal length
	 */
	public static int dotp(Integer[] vectorOne, Integer[] vectorTwo) throws VectorMathException {
		if(vectorOne.length != vectorTwo.length){
			throw new VectorMathException(
					"Input Vectors do not have the same number of dimensions.");
		}
		int dotProduct = 0;
		for(int i = 0; i < vectorOne.length; i++){
			dotProduct += (vectorOne[i] * vectorTwo[i]);
		}
		return dotProduct;	
	}
	
	/**
	 * Calculate the Magnitude of a vector
	 * @param vector Vector
	 * @return Magnitude of the Vector
	 */
	public static double magnitude(Collection<Integer> vector){
		return magnitude(vector.toArray(new Integer[0]));
	}
	
	/**
	 * Calculate the Magnitude of a vector
	 * @param vector Vector
	 * @return Magnitude of the Vector
	 */
	public static double magnitude(Integer[] vector){
		double magnitude = 0;
		for(int i = 0; i < vector.length; i++){
			magnitude += Math.pow(vector[i], 2);
		}
		return Math.sqrt(magnitude);
	}
	
}
