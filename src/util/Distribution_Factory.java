package util;

import java.util.Arrays;

import org.apache.commons.math3.random.EmpiricalDistribution;

public class Distribution_Factory {

	public static EmpiricalDistribution generateEmpiricalDistribution(double[] val, int[] freq) {

		int count = Arrays.stream(freq).sum();
		double[] data = new double[count];
		int pt = 0;

		for (int i = 0; i < val.length; i++) {
			double v = val[i];
			for (int c = 0; c < freq[i]; c++) {
				data[pt] = v;
				pt++;
			}
		}		

		EmpiricalDistribution dist = new EmpiricalDistribution();
		dist.load(data);
		return dist;

	}

}
