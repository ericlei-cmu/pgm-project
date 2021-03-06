package cmu.ml.pgm.project;

import static cmu.ml.pgm.project.MatrixMethods.*;

import java.io.*;

import no.uib.cipr.matrix.*;

public final class CollectiveMatrixFactorization {

	private CollectiveMatrixFactorization() {
	}

	public static CollectiveMatrixFactorizationResult factorizeMatricesWithFeatures(
			CollectiveMatrixFactorizationDataset data, int latentDim,
			int maxIterOuter, int maxIterInner, double stepFeatureTransforms,
			double stepLatentFeatures) {
		return factorizeMatricesWithFeatures(data, latentDim, maxIterOuter,
				maxIterInner, stepFeatureTransforms, stepLatentFeatures,
				stepLatentFeatures, true, false);
	}

	public static CollectiveMatrixFactorizationResult factorizeMatricesWithFeatures(
			CollectiveMatrixFactorizationDataset data, int latentDim,
			int maxIterOuter, int maxIterInner, double stepFeatureTransforms,
			double stepLatentFromFeatures, double stepLatentFromRelations,
			boolean saveIntermediate, boolean writeIntermediate) {

		int nEntities = data.getNumEntities();
		CollectiveMatrixFactorizationResult result = new CollectiveMatrixFactorizationResult(
				nEntities);

		// initialize estimates
		for (int s = 0; s < nEntities; s++) {
			int n = data.getNumItems(s);
			int dNorm = data.getNumNormalFeatures(s);
			result.setLatentFeatures(s, new DenseMatrix(n, latentDim));
			randomlyInitialize(result.getLatentFeatures(s));
			result.setBernoulliFeatureMap(s,
					new DenseMatrix(latentDim, data.getNumBernoulliFeatures(s)));
			randomlyInitialize(result.getBernoulliFeatureMap(s));
			result.setNormalFeatureMap(s,
					new DenseMatrix(latentDim, data.getNumNormalFeatures(s)));
			randomlyInitialize(result.getNormalFeatureMap(s));

			Matrix fNorm = data.getNormalFeatures(s);
			Matrix u = result.getLatentFeatures(s);
			Matrix aNorm = result.getNormalFeatureMap(s);
			result.setFeatureVariance(s,
					squaredFrobeniusNorm(minus(fNorm, times(u, aNorm)))
							/ (n * dNorm));
		}
		for (int s = 0; s < nEntities; s++) {
			for (int t = s; t < nEntities; t++) {
				Matrix r = data.getRelations(s, t);
				if (r == null)
					continue;
				Matrix rHat = result.getRelations(s, t);
				result.setRelationVariance(
						s,
						t,
						sparseSquaredFrobeniusNormOfDiff(r, rHat)
								/ data.getNumObserved(s, t));
			}
		}

		{
			double obj = calcObj(data, result);
			System.out.println("initial neg llh=" + obj);
			System.out.println();
		}

		// coordinate descent
		for (int t = 0; t < maxIterOuter; t++) {
			System.out.println("t = " + t);
			// update feature maps for each entity
			for (int s = 0; s < nEntities; s++) {
				// System.out.println("updating feature maps for " + s);
				Matrix u = result.getLatentFeatures(s);
				Matrix fBern = data.getBernoulliFeatures(s);
				Matrix fNorm = data.getNormalFeatures(s);
				int dBern = data.getNumBernoulliFeatures(s);
				int dNorm = data.getNumNormalFeatures(s);
				int n = data.getNumItems(s);
				double sigma2 = result.getFeatureVariance(s);

				// update Bernoulli maps
				for (int tt = 0; tt < maxIterInner; tt++) {
					// System.out.println("\ttt = " + tt);
					// update each column
					for (int k = 0; k < dBern; k++) {
						Vector grad = new DenseVector(latentDim);
						Matrix a = result.getBernoulliFeatureMap(s);
						Vector a_k = getColumn(a, k);
						for (int i = 0; i < n; i++) {
							Vector u_i = getRow(u, i);
							Vector newVal = times(u_i, fBern.get(i, k));
							newVal.add(
									-1,
									times(u_i,
											1 - 1 / (1 + Math.exp(u_i.dot(a_k)))));
							grad.add(newVal);
						}
						Vector scaledGrad = times(grad, stepFeatureTransforms);
						addToColumn(a, k, scaledGrad);
						result.setBernoulliFeatureMap(s, a);
					}
				}

				// update normal maps
				for (int tt = 0; tt < maxIterInner; tt++) {
					Matrix an = result.getNormalFeatureMap(s);
					Matrix grad = times(transpose(u),
							minus(fNorm, times(u, an)));
					grad.scale(1 / sigma2);
					Matrix scaledGrad = times(grad, stepFeatureTransforms);
					an.add(scaledGrad);
					result.setNormalFeatureMap(s, an);
				}

				// update variance
				Matrix an = result.getNormalFeatureMap(s);
				Matrix mu = times(u, an);
				sigma2 = squaredFrobeniusNorm(minus(fNorm, mu)) / (n * dNorm);
				result.setFeatureVariance(s, sigma2);

			}
			// System.out.println("finished feature maps");

			{
				double obj = calcObj(data, result);

				System.out.println("after feature map neg llh=" + obj);
				System.out.println();
			}

			// update latent features for each entity
			for (int s = 0; s < nEntities; s++) {
				// System.out.println("updating latent features for " + s);
				Matrix fBern = data.getBernoulliFeatures(s);
				Matrix fNorm = data.getNormalFeatures(s);
				Matrix aBern = result.getBernoulliFeatureMap(s);
				Matrix aNorm = result.getNormalFeatureMap(s);
				int dBern = data.getNumBernoulliFeatures(s);
				int n = data.getNumItems(s);
				double sigma2_s = result.getFeatureVariance(s);

				for (int tt = 0; tt < maxIterInner; tt++) {
					// System.out.println("\ttt = " + tt);
					Matrix u = result.getLatentFeatures(s);

					// gradient from Bernoulli feature term
					if (data.getNumBernoulliFeatures(s) != 0) {
						for (int i = 0; i < n; i++) {
							// update row i of U
							Vector grad = new DenseVector(latentDim);
							for (int k = 0; k < dBern; k++) {
								Vector u_i = getRow(u, i);
								Vector a_k = getColumn(aBern, k);
								Vector newVal = times(a_k, fBern.get(i, k));
								newVal.add(
										-1,
										times(a_k, 1 - 1 / (1 + Math.exp(u_i
												.dot(a_k)))));
								grad.add(newVal);
							}
							Vector scaledGrad = times(grad,
									stepLatentFromFeatures);
							addToRow(result.getLatentFeatures(s), i, scaledGrad);
						}
						// System.out.println("\t1");
					}

					// System.out.println("getting gradient from normal features");
					// gradient from normal feature term
					if (data.getNumNormalFeatures(s) != 0) {
						Matrix gradNorm = times(minus(fNorm, times(u, aNorm)),
								transpose(aNorm)).scale(1 / sigma2_s);
						Matrix scaledGradNorm = times(gradNorm,
								stepLatentFromFeatures);
						result.getLatentFeatures(s).add(scaledGradNorm);
						// System.out.println("\t2");
						// System.out.println(scaledGradNorm.get(0, 0));
					}

					// System.out.println("getting gradient from relations");
					// gradient from relation term with each entity
					for (int w = 0; w < nEntities; w++) {
						// if (w == s)
						// continue;
						Matrix r = data.getRelations(s, w);
						if (r == null)
							continue;
						Matrix v = w == s ? u : result.getLatentFeatures(w);
						Matrix rHat = times(u, transpose(v));
						int n_w = data.getNumItems(w);
						double sigma2_sw = result.getRelationVariance(s, w);
						for (int i = 0; i < n; i++) {
							// update row i of U
							Vector grad = new DenseVector(latentDim);
							for (int j = 0; j < n_w; j++) {
								double r_ij = r.get(i, j);
								double rHat_ij = rHat.get(i, j);
								double selfRelationCorrection = (w == s && i == j) ? 2
										: 1;
								if (r_ij != 0)
									grad.add(times(getRow(v, j),
											selfRelationCorrection
													* (r_ij - rHat_ij)));
							}
							grad.scale(1 / sigma2_sw);
							Vector scaledGrad = times(grad,
									stepLatentFromRelations);
							addToRow(result.getLatentFeatures(s), i, scaledGrad);
							// if (i == 0)
							// System.out.println(scaledGrad.get(0));
						}
					}
					// System.out.println("\t3");
				}
			}
			// System.out.println("finished latent features");

			{
				double obj = calcObj(data, result);

				System.out.println("after latent features neg llh=" + obj);
				System.out.println();
			}

			// update variances
			for (int s = 0; s < nEntities; s++) {
				Matrix aNorm = result.getNormalFeatureMap(s);
				Matrix fNorm = data.getNormalFeatures(s);
				Matrix u = result.getLatentFeatures(s);
				int n = data.getNumItems(s);
				int dNorm = data.getNumNormalFeatures(s);
				double sigma2 = squaredFrobeniusNorm(minus(fNorm,
						times(u, aNorm)))
						/ (n * dNorm);
				result.setFeatureVariance(s, sigma2);

				for (int w = s; w < nEntities; w++) {
					Matrix r = data.getRelations(s, w);
					if (r == null)
						continue;
					Matrix rHat = result.getRelations(s, w);
					result.setRelationVariance(
							s,
							w,
							sparseSquaredFrobeniusNormOfDiff(r, rHat)
									/ data.getNumObserved(s, w));
				}
			}

			{
				double obj = calcObj(data, result);
				System.out.println("after all neg llh=" + obj);
				System.out.println();
			}

			if (saveIntermediate)
				result.addIntermediateRelations();

			if (writeIntermediate)
				writeLatentFeatures(result, t);
		}

		return result;
	}

	// assumes no bernoulli features
	static double calcObj(CollectiveMatrixFactorizationDataset data,
			CollectiveMatrixFactorizationResult result) {
		int nEntities = data.getNumEntities();
		double obj = 0; // negative log likelihood
		for (int s = 0; s < nEntities; s++) {
			for (int w = s; w < nEntities; w++) {
				Matrix r = data.getRelations(s, w);
				if (r == null)
					continue;
				Matrix rHat = result.getRelations(s, w);
				double sse = sparseSquaredFrobeniusNormOfDiff(r, rHat);
				double temp = sse / result.getRelationVariance(s, w);
				obj += temp;
				System.out.println("raw rel sse " + s + w + " = " + sse);
				System.out.println("rel sse " + s + w + "=" + temp);

				double temp2 = Math.log(result.getRelationVariance(s, w))
						* data.getNumObserved(s, w);
				obj += temp2;
				System.out.println("var penalty " + s + w + "=" + temp2);
			}
			Matrix fHat = times(result.getLatentFeatures(s),
					result.getNormalFeatureMap(s));
			double temp1 = squaredFrobeniusNorm(minus(
					data.getNormalFeatures(s), fHat))
					/ result.getFeatureVariance(s);
			double temp2 = Math.log(result.getFeatureVariance(s))
					* fHat.numRows() * fHat.numColumns();
			if (!Double.isNaN(temp1) && !Double.isNaN(temp2)) {
				obj += temp1 + temp2;
				System.out.println("feat sse " + s + "=" + temp1);
				System.out.println("var penalty " + s + "=" + temp2);
			}
		}
		return obj;
	}

	public static void writeLatentFeatures(
			CollectiveMatrixFactorizationResult result, int t) {
		try {
			String dir = "output/";
			int nEntities = result.getNumEntities();
			for (int s = 0; s < nEntities; s++) {
				Matrix u = result.getLatentFeatures(s);
				String filename = dir + String.format("U.%d.(%d).dat", s, t);
				File file = new File(filename);
				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				int nRows = u.numRows();
				int nCols = u.numColumns();
				for (int i = 0; i < nRows; i++) {
					bw.write("" + u.get(i, 0));
					for (int j = 1; j < nCols; j++) {
						bw.write("," + u.get(i, j));
					}
					bw.newLine();
				}
				bw.close();
			}
		} catch (IOException e) {
			System.err.println(e);
		}
	}

}
