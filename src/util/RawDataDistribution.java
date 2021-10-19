package util;

import java.util.Arrays;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public class RawDataDistribution extends AbstractRealDistribution {

	private static final long serialVersionUID = 4939844542094681367L;

	private double[][] dataMapCumulative; // dataMap[RAW_DATA_VAL][cumul_probability]
	
	private static final int RAW_DATA_VAL_X = 0;
	private static final int RAW_DATA_VAL_P = RAW_DATA_VAL_X + 1;

	public RawDataDistribution(RandomGenerator rng) {
		super(rng);
	}

	public double[][] getDataMapCumulative() {
		return dataMapCumulative;
	}

	public void setDataMapCumulative(double[][] dataMapCumulative) {
		this.dataMapCumulative = dataMapCumulative;
	}

	private double getCumulValueFromMap(double arg) {
		return getCumulValueFromMap(arg, 
				Arrays.binarySearch(dataMapCumulative[RAW_DATA_VAL_X], arg), true);
	}

	private double getCumulValueFromMap(double arg, int index, boolean argIsX) {				
		double val;
		int dep_index, indep_index;
		
		if (argIsX) {
			dep_index = RAW_DATA_VAL_P; 
			indep_index = RAW_DATA_VAL_X;
		}else {
			dep_index = RAW_DATA_VAL_X; 
			indep_index = RAW_DATA_VAL_P;
		}		
		if (index >= 0) {
			return dataMapCumulative[dep_index][index];
		} else {
			// Use linear approximation
			double start_indep, start_dep, next_indep, next_dep;
			next_dep = dataMapCumulative[dep_index][~index];
			next_indep = dataMapCumulative[indep_index][~index];		

			if (~index == 0) {
				start_dep = 0;
				start_indep = 0;
			} else {
				start_dep = dataMapCumulative[dep_index][~index - 1];
				start_indep = dataMapCumulative[indep_index][~index - 1];
			}
			val = start_dep + (next_dep - start_dep) * (arg - start_indep) / (next_indep - start_indep);
			return val;

		}

	}

	@Override
	public double cumulativeProbability(double arg0) {
		return getCumulValueFromMap(arg0);
	}

	@Override
	public double density(double arg0) {
		int index = Arrays.binarySearch(dataMapCumulative[RAW_DATA_VAL_X], arg0);

		final double DX_RATIO = 10E-8;
		double cumul_p_at = getCumulValueFromMap(arg0, index, true);
		double cumul_x_at = arg0;

		double cumul_x_pre;
		if (index > 0) {
			cumul_x_pre = dataMapCumulative[RAW_DATA_VAL_X][index - 1];

		} else if (index == 0 || ~index == 0) {
			cumul_x_pre = 0;

		} else {
			cumul_x_pre = dataMapCumulative[RAW_DATA_VAL_X][~index - 1];
		}

		return cumul_p_at - getCumulValueFromMap(arg0 - (cumul_x_at - cumul_x_pre) * DX_RATIO);
	}

	@Override
	public double getNumericalMean() {
		int index = Arrays.binarySearch(dataMapCumulative[RAW_DATA_VAL_P], 0.5);				
		return getCumulValueFromMap(0.5, index, false);
	}

	@Override
	public double getNumericalVariance() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();		
	}

	@Override
	public double getSupportLowerBound() {
		return 0;
	}

	@Override
	public double getSupportUpperBound() {
		return 1;
	}

	@Override
	public boolean isSupportConnected() {
		return true;
	}

	@Override
	public boolean isSupportLowerBoundInclusive() {
		return true;
	}

	@Override
	public boolean isSupportUpperBoundInclusive() {
		return true;
	}

}
