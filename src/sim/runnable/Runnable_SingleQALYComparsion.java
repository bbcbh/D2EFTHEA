package sim.runnable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import person.D2EFT_HEA_Person;

public class Runnable_SingleQALYComparsion implements Runnable {

	public static final String KEY_SEED = "KEY_SEED";
	public static final String KEY_DAY_0_QALY = "KEY_DAY_0_QALY";
	public static final String KEY_HEA_PERSON = "KEY_HEA_PERSON";
	public static final float DEFAULT_DELTA_QALY_RANGE = 0.05f;
	

	private final float delta_qaly; 
	private final long seed;
	private final float[] day_0_qalys;
	private final float[][] qaly_mapping; // []{study_arm, wk_0, wk_48, wk_96}
	private final int[] mapping_study_arm_offset;
	

	private HashMap<String, Object> result;

	
	
	public Runnable_SingleQALYComparsion(final long seed, final float[][] qaly_mapping,
			final int[] mapping_study_arm_offset, final float[] day_0_qalys, float delta_qaly) {		
		this.seed = seed;
		this.qaly_mapping = Arrays.copyOf(qaly_mapping, qaly_mapping.length);
		this.mapping_study_arm_offset = Arrays.copyOf(mapping_study_arm_offset, mapping_study_arm_offset.length);
		this.day_0_qalys = Arrays.copyOf(day_0_qalys,day_0_qalys.length);
		this.delta_qaly = delta_qaly;
		result = new HashMap<String, Object>();
	}
	
	public Runnable_SingleQALYComparsion(final long seed, final float[][] qaly_mapping,
			final int[] mapping_study_arm_offset, final float[] day_0_qalys) {
		this(seed, qaly_mapping, mapping_study_arm_offset, day_0_qalys, DEFAULT_DELTA_QALY_RANGE);
	}

	@Override
	public void run() {

		
		RandomGenerator rng = new MersenneTwister(seed);
		result.put(KEY_SEED, seed);

		float day_0_qaly = day_0_qalys[rng.nextInt(day_0_qalys.length)];
		result.put(KEY_DAY_0_QALY, day_0_qaly);

		D2EFT_HEA_Person[] cmpPerson = new D2EFT_HEA_Person[mapping_study_arm_offset.length];
		float[][] vaild_qaly_mapping;

		for (int s = 0; s < mapping_study_arm_offset.length; s++) {
			
			vaild_qaly_mapping = qaly_mapping;
			cmpPerson[s] = new D2EFT_HEA_Person(seed, s);
			float[] interpol_QALY = new float[] { day_0_qaly, Float.NaN, Float.NaN };

			int sampleRangeStartPt = mapping_study_arm_offset[s]; // Inclusive
			int sampleRangeEndPt = (s + 1) < mapping_study_arm_offset.length ? mapping_study_arm_offset[s + 1]
					: vaild_qaly_mapping.length; // Non-Inclusive
			float sampleVal = day_0_qaly;

			
			int[] visit_to_sample = new int[] { 1, 2 }; // 1 = Wk_00, 2 = Wk_48
			
		
			
			QALY_MAPPING_COMPARATOR cmp = new QALY_MAPPING_COMPARATOR(visit_to_sample[0]);

			for (int vPt =0; vPt < visit_to_sample.length; vPt++) {
				
				vaild_qaly_mapping = Arrays.copyOfRange(vaild_qaly_mapping, sampleRangeStartPt, sampleRangeEndPt);
				
				int visitResPt = visit_to_sample[vPt];				
				float[] searchKey = new float[vaild_qaly_mapping[0].length];
				searchKey[visitResPt] = sampleVal;

				int midPt = Arrays.binarySearch(vaild_qaly_mapping, searchKey, cmp);
				if (midPt < 0) {
					midPt = ~midPt;
				}

				int[] posToInclude = new int[] { midPt, midPt };

				while (posToInclude[0] > 0
						&& (vaild_qaly_mapping[posToInclude[0] - 1][visitResPt]) >= Math.max(0,
								sampleVal - delta_qaly)) {
					posToInclude[0]--;
				}
				while ((posToInclude[1]) < vaild_qaly_mapping.length
						&& vaild_qaly_mapping[posToInclude[1]][visitResPt] < sampleVal + delta_qaly) {
					posToInclude[1]++;
				}

				double[] sampleResult;

				if (posToInclude[1] > posToInclude[0]) {
					sampleResult = new double[posToInclude[1] - posToInclude[0]];
					for (int i = 0; i < sampleResult.length; i++) {
						sampleResult[i] = vaild_qaly_mapping[posToInclude[0] + i][visitResPt + 1];
					}

					Arrays.sort(sampleResult);
					int toInclude = sampleResult.length;
					while (Double.isNaN(sampleResult[toInclude - 1])) {
						toInclude--;
					}
					sampleResult = Arrays.copyOf(sampleResult, toInclude);
				} else {
					sampleResult = new double[] { vaild_qaly_mapping[posToInclude[0]][visitResPt + 1] };
				}

				if (sampleResult.length > 0) {
					EmpiricalDistribution dist = new EmpiricalDistribution(rng);
					dist.load(sampleResult);
					interpol_QALY[visitResPt] = (float) dist.sample();

				} else {
					interpol_QALY[visitResPt] = (float) sampleResult[0];
				}				
				
				// Set up next search
				if (vPt+1 < visit_to_sample.length) {
					sampleVal = interpol_QALY[visitResPt];					
					sampleRangeStartPt = posToInclude[0];
					sampleRangeEndPt = posToInclude[1];
					cmp = new QALY_MAPPING_COMPARATOR(visit_to_sample[vPt+ 1]);
					Arrays.sort(vaild_qaly_mapping, sampleRangeStartPt, sampleRangeEndPt,cmp);
				}

			}

			cmpPerson[s].setInterpol_study_day(new int[] { 0, 48 * 7, 96 * 7 });
			cmpPerson[s].setInterpol_QALY(interpol_QALY);
			cmpPerson[s].generatePolyFit(2);
		}
		
		result.put(KEY_HEA_PERSON, cmpPerson);				
		
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
