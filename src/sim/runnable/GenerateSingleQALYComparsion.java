package sim.runnable;

import java.util.Arrays;
import java.util.HashMap;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import person.D2EFT_HEA_Person;

public class GenerateSingleQALYComparsion implements Runnable {

	public static String KEY_SEED = "KEY_SEED";
	public static String KEY_DAY_0_QALY = "KEY_DAY_0_QALY";
	public static String KEY_QALY = "KEY_QALY";

	private final long seed;
	private final float[] day_0_qalys;
	private final float[][] qaly_mapping;
	private final int[] mapping_study_arm_offset;

	private HashMap<String, Object> result;

	public GenerateSingleQALYComparsion(final long seed, final float[][] qaly_mapping, // []{study_arm, wk_0, wk_48,
																						// wk_96}
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
			
			// Locate row that has similiar starting results 
			
		
			
			

			cmpPerson[s].setInterpol_study_day(new int[] { 0, 48 * 7, 96 * 7 });
			cmpPerson[s].setInterpol_QALY(interpol_QALY);
		}

	}

	public HashMap<String, Object> getResult() {
		return result;
	}

}
