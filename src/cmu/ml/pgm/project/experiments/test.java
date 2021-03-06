package cmu.ml.pgm.project.experiments;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import cmu.ml.pgm.project.*;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrix;
//import jdistlib.Wishart;

/**
 * Created by dexter on 15. 3. 3..
 */
public class test {
	public static void main(String[] args) {
		MatrixFactorizationMovieLens mfTrain
		= new MatrixFactorizationMovieLens("Data/ml-100k/u.user", "Data/ml-100k/u.item",
				"Data/ml-100k/u.data.train", "Data/ml-100k/u.info.train");
		//		mfTrain.printMatrix();
		int latentDim = 3;
		double stepSize = 1e-4;
		double regCoef = .1;
		int maxIter = 10;
		double tol = 1e-6;
		MatrixFactorizationMovieLens mfTest
		= new MatrixFactorizationMovieLens("Data/ml-100k/u.user", "Data/ml-100k/u.item",
				"Data/ml-100k/u.data.test", "Data/ml-100k/u.info.test");
		Matrix testR = mfTest.getRelationMatrix();
		int nTest;
		PrintWriter writer;

		boolean doBaselineMethod = false;
		boolean doFeaturesMethod = false; // rmse=.19
		boolean doBayesian = true;

		if (doBaselineMethod) {
			for (int d = 1; d <= 10; d++) {
				System.out.println("Starting Baseline MF");
				MatrixFactorizationResult factorizationResult = BaselineMatrixFactorization.factorizeMatrix(
						mfTrain, latentDim, stepSize, regCoef, maxIter * mfTrain.getTrainingData().size(), tol);
				Matrix rBaseline = factorizationResult.getR();
				System.out.println("Done with Baseline MF");
				nTest = 0;
				double error = 0;
				for (int i = 0; i < mfTest.getNumUsers(); i++) {
					for (int j = 0; j < mfTest.getNumItems(); j++) {
						if (testR.get(i, j) != 0) {
							nTest++;
							error += Math.pow(testR.get(i, j) - rBaseline.get(i, j), 2);
						}
					}
				}
				error = Math.sqrt(error / nTest);
				System.out.printf("RMSE baseline = %f\n", error);

				try {
					writer = new PrintWriter("output/rNoFeatures.txt", "UTF-8");
					writer.println(rBaseline);
					writer.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}

		if (doFeaturesMethod) {
			//			for (int d = 7; d <= 9; d++) {
			System.out.println("starting with features");
			MatrixFactorizationResult featuresResult = MatrixFactorization.factorizeMatrixWithFeatures(
					mfTrain, latentDim, stepSize, maxIter, tol);
			System.out.println("done with features");
			for (int k = 0; k < featuresResult.getIntermediateR().size(); k++) {
				Matrix rFeatures = featuresResult.getIntermediateR().get(k);
				double featuresError = 0;
				nTest = 0;
				for (int i = 0; i < mfTest.getNumUsers(); i++)
					for (int j = 0; j < mfTest.getNumItems(); j++)
						if (testR.get(i, j) != 0) {
							nTest++;
							featuresError += Math.pow(testR.get(i, j) - rFeatures.get(i, j), 2);
						}
				featuresError = Math.sqrt(featuresError / nTest);
				System.out.printf("RMSE with features = %f\n", featuresError);

				//				System.out.println("ten entries of R with features:");
				//				for (int i = 0; i < 10; i++)
				//					System.out.print(rFeatures.get(0, i) + " ");
				//				System.out.println();
			}

			try {
				Matrix rFeatures = featuresResult.getR();
				writer = new PrintWriter("output/rFeatures.txt", "UTF-8");
				writer.println(rFeatures);
				writer.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			//			}
		}

		if (doBayesian) {
//			System.out.println("starting Bayesian");
////			for(latentDim = 2; latentDim <= 10; latentDim++) {
//			latentDim = 3;
//			MatrixFactorizationResult bayesianResult = BayesianMatrixFactorization.factorizeMatrixWithFeatures(mfTrain, latentDim, 100, 0.1);
//			Matrix rFeatures = bayesianResult.getR();
//			System.out.println("done with features");
//			double featuresError = 0;
//			nTest = 0;
//			Matrix average = new DenseMatrix(rFeatures.numRows(), rFeatures.numColumns());
//			for(int iter = 0; iter < bayesianResult.getU().length; iter++) {
//				featuresError = 0;
//				rFeatures = BayesianMatrixFactorization.matrixMult(bayesianResult.getU()[iter],
//						BayesianMatrixFactorization.transpose(bayesianResult.getV()[iter]));
//				average.add(rFeatures);
//				for (int i = 0; i < mfTest.getNumUsers(); i++) {
//					for (int j = 0; j < mfTest.getNumItems(); j++) {
//						if (testR.get(i, j) != 0) {
//							nTest++;
////							featuresError += Math.pow(testR.get(i, j) - rFeatures.get(i, j), 2);
//							featuresError += Math.pow(testR.get(i, j) - average.get(i, j) / (iter + 1), 2);
//						}
//					}
//				}
//
//				featuresError = Math.sqrt(featuresError / nTest);
//				System.out.printf("Dim: " + latentDim + ", RMSE Bayesian = %f\n", featuresError);
//			}
//			featuresError = 0;
//			rFeatures = bayesianResult.getR();
//			for (int i = 0; i < mfTest.getNumUsers(); i++) {
//				for (int j = 0; j < mfTest.getNumItems(); j++) {
//					if (testR.get(i, j) != 0) {
//						nTest++;
//						featuresError += Math.pow(testR.get(i, j) - rFeatures.get(i, j), 2);
//					}
//				}
//			}
//			featuresError = Math.sqrt(featuresError / nTest);
//			System.out.printf("AVERAGE Dim: " + latentDim + ", RMSE Bayesian = %f\n", featuresError);
////			}
//			try {
//				writer = new PrintWriter("output/rBayesian.txt", "UTF-8");
//				writer.println(rFeatures);
//				writer.close();
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			} catch (UnsupportedEncodingException e) {
//				e.printStackTrace();
//			}
		}
//		MatrixFactorizationMovieLens mfTrain
//				= new MatrixFactorizationMovieLens("Data/ml-1m/users.dat", "Data/ml-1m/movies.dat",
//				"Data/ml-1m/ratings.dat", "Data/ml-1m/info",1);
	}
}
