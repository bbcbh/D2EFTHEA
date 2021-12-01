package sim.runnable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import person.D2EFT_HEA_Person;

public class GenerateSingleQALYComparsion implements Runnable {

	public static String KEY_SEED = "KEY_SEED";
	public static String KEY_DAY_0_QALY = "KEY_DAY_0_QALY";
	public static String KEY_QALY = "KEY_QALY";

	private final long seed;
	private final float[] day_0_qalys;
	private final float[][] qaly_mapping; // []{study_arm, wk_0, wk_48, wk_96}
	private final int[] mapping_study_arm_offset;
	private final float QALY_RANGE = 0.05f;

	private HashMap<String, Object> result;

	public GenerateSingleQALYComparsion(final long seed, final float[][] qaly_mapping,
			final int[] mapping_study_arm_offset, final float[] day_0_qalys) {

		this.seed = seed;
		this.qaly_mapping = qaly_mapping;
		this.mapping_study_arm_offset = mapping_study_arm_offset;
		this.day_0_qalys = day_0_qalys;
		result = new HashMap<String, Object>();
	}

	@Override
	public void run() {

		RandomGenerator rng = new MersenneTwister(seed);
		result.put(KEY_SEED, seed);

		float day_0_qaly = day_0_qalys[rng.nextInt(day_0_qalys.length)];
		result.put(KEY_DAY_0_QALY, day_0_qaly);

		D2EFT_HEA_Person[] cmpPerson = new D2EFT_HEA_Person[mapping_study_arm_offset.length];

		for (int s = 0; s < mapping_study_arm_offset.length; s++) {

			cmpPerson[s] = new D2EFT_HEA_Person(seed, s);
			float[] interpol_QALY = new float[] { day_0_qaly, Float.NaN, Float.NaN };

			int studyArmEndPt = s + 1 < mapping_study_arm_offset.length ? s + 1 : mapping_study_arm_offset.length;
			// Locate row that has similar to day 0 results

			for (int visitResPt : new int[] { 1, 2 }) { // 1 = Wk_00, 2 = Wk_48

				QALY_MAPPING_COMPARATOR cmp = new QALY_MAPPING_COMPARATOR(visitResPt);
				int midPt = Arrays.binarySearch(qaly_mapping, mapping_study_arm_offset[s], studyArmEndPt,
						new float[] { 0, day_0_qaly, 0, 0 }, cmp);
				if (midPt < 0) {
					midPt = ~midPt;
				}
				int[] posToInclude = new int[] { midPt, midPt };
				float[] valToInclude = new float[] { qaly_mapping[midPt][visitResPt], qaly_mapping[midPt][visitResPt] };
				while (posToInclude[0] > 0 && valToInclude[posToInclude[0]] >= Math.max(0, day_0_qaly - QALY_RANGE)) {
					posToInclude[0]--;
					valToInclude[0] = qaly_mapping[posToInclude[0]][visitResPt];
				}
				while (posToInclude[1] < qaly_mapping.length
						&& valToInclude[posToInclude[1]] >= day_0_qaly + QALY_RANGE) {
					posToInclude[1]++;
					valToInclude[1] = qaly_mapping[posToInclude[1]][visitResPt];
				}

				double[] sampleResult = new double[posToInclude[1] - posToInclude[0] + 1];
				for (int i = 0; i < sampleResult.length; i++) {
					sampleResult[i] = qaly_mapping[posToInclude[0] + 0][visitResPt + 1];
				}

				Arrays.sort(sampleResult);
				EmpiricalDistribution dist = new EmpiricalDistribution();
				dist.load(sampleResult);
				interpol_QALY[visitResPt + 1] = (float) dist.sample();
			}

			cmpPerson[s].setInterpol_study_day(new int[] { 0, 48 * 7, 96 * 7 });
			cmpPerson[s].setInterpol_QALY(interpol_QALY);
		}
	}

	public HashMap<String, Object> getResult() {
		return result;
	}

	private class QALY_MAPPING_COMPARATOR implements Comparator<float[]> {

		int cmpIndex;

		public QALY_MAPPING_COMPARATOR(int cmpIndex) {
			this.cmpIndex = cmpIndex;

		}

		@Override
		public int compare(float[] o1, float[] o2) {

			return Float.compare(o1[cmpIndex], o2[cmpIndex]);
		}

	}

}
