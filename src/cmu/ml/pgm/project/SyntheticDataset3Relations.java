package cmu.ml.pgm.project;

import java.io.BufferedReader;
import java.io.FileReader;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.sparse.LinkedSparseMatrix;
import static cmu.ml.pgm.project.MatrixMethods.*;

/**
 * When indexing entities of this dataset, use 0, 1, 2.
 * 
 * @author eric
 *
 */
public class SyntheticDataset3Relations implements CollectiveMatrixFactorizationDataset {

	private Matrix f1, f2, f3, r12, r13, r23;
	private final int N_ENTITIES = 3;
	private final int[] N = { 1000, 1000, 1000 };
	private final int[] D = { 100, 100, 100 };
	private final int N_OBS_R12 = 50000;
	private final int N_OBS_R13 = 50000;
	private final int N_OBS_R23 = 50000;

	public SyntheticDataset3Relations(String f1Path, String f2Path, String f3Path,
			String r12Path, String r13Path, String r23Path, boolean isSparse) {
		if (isSparse) {
			r12 = new LinkedSparseMatrix(N[0], N[1]);
			r13 = new LinkedSparseMatrix(N[0], N[2]);
			r23 = new LinkedSparseMatrix(N[1], N[2]);
			initializeSparseMatrix(r12Path, r12);
			initializeSparseMatrix(r13Path, r13);
			initializeSparseMatrix(r23Path, r23);
		} else {
			r12 = new DenseMatrix(N[0], N[1]);
			r13 = new DenseMatrix(N[0], N[2]);
			r23 = new DenseMatrix(N[1], N[2]);
			initializeDenseMatrix(r12Path, r12);
			initializeDenseMatrix(r13Path, r13);
			initializeDenseMatrix(r23Path, r23);
		}

		f1 = new DenseMatrix(N[0], D[0]);
		f2 = new DenseMatrix(N[1], D[1]);
		f3 = new DenseMatrix(N[2], D[2]);
		initializeDenseMatrix(f1Path, f1);
		initializeDenseMatrix(f2Path, f2);
		initializeDenseMatrix(f3Path, f3);
	}

	void initializeDenseMatrix(String filename, Matrix r) {
		try {
			BufferedReader fin = new BufferedReader(new FileReader(filename));
			int i = 0;
			while (fin.ready()) {
				String delim = ",";
				String[] tokens = fin.readLine().split(delim);
				for (int j = 0; j < tokens.length; j++) {
					float val = Float.parseFloat(tokens[j]);
					r.set(i, j, val);
				}
				i++;
			}
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	void initializeSparseMatrix(String filename, Matrix r) {
		try {
			BufferedReader fin = new BufferedReader(new FileReader(filename));
			while (fin.ready()) {
				String delim = ",";
				String[] tokens = fin.readLine().split(delim);
				int user_id = Integer.parseInt(tokens[0]) - 1;
				int item_id = Integer.parseInt(tokens[1]) - 1;
				float rating = Float.parseFloat(tokens[2]);
				r.set(user_id, item_id, rating);
			}
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	public int getNumEntities() {
		return N_ENTITIES;
	}

	@Override
	public Matrix getRelations(int s, int t) {
		if (s == t) return null;
		assert 0 <= s && s <= 2;
		assert 0 <= t && t <= 2;
		if (s == 0) {
			if (t == 1)
				return r12;
			else
				return r13;
		}

		if (s == 1) {
			if (t == 0)
				return transpose(r12);
			else
				return r23;
		}

		// s == 2
		if (t == 0)
			return transpose(r13);
		else
			return transpose(r23);
	}

	@Override
	public Matrix getBernoulliFeatures(int s) {
		assert 0 <= s && s <= 2;
		return new DenseMatrix(N[s], 0);
	}

	@Override
	public Matrix getNormalFeatures(int s) {
		assert 0 <= s && s <= 2;
		if (s == 0)
			return f1;
		if (s == 1)
			return f2;
		return f3;
	}

	@Override
	public int getNumItems(int s) {
		assert 0 <= s && s <= 2;
		return N[s];
	}

	@Override
	public int getNumBernoulliFeatures(int s) {
		assert 0 <= s && s <= 2;
		return 0;
	}

	@Override
	public int getNumNormalFeatures(int s) {
		assert 0 <= s && s <= 2;
		return D[s];
	}

	@Override
	public int getNumObserved(int s, int t) {
		if (s == t) return 0;
		assert 0 <= s && s <= 2;
		assert 0 <= t && t <= 2;
		if (s == 0) {
			if (t == 1)
				return N_OBS_R12;
			else
				return N_OBS_R13;
		}

		if (s == 1) {
			if (t == 0)
				return N_OBS_R12;
			else
				return N_OBS_R23;
		}

		// s == 2
		if (t == 0)
			return N_OBS_R13;
		else
			return N_OBS_R23;
	}

}