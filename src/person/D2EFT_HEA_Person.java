package person;

import java.util.Arrays;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

public class D2EFT_HEA_Person{
	private long id;
	private int study_arm;
	private int[] interpol_study_day;			

	public static final int STUDY_ARM_SOC = 0;
	public static final int STUDY_ARM_DOL = STUDY_ARM_SOC + 1;
	public static final int STUDY_ARM_D2N = STUDY_ARM_DOL + 1;
	
	public static final int INDEX_QALY = 0;
	public static final int INDEX_CD4 = INDEX_QALY + 1;
	public static final int INDEX_LENGTH = INDEX_CD4 + 1;
	
	private float[][] interpol = new float[INDEX_LENGTH][]; 
	private double[][] polyFit = new double[INDEX_LENGTH][];
	
	

	public D2EFT_HEA_Person(long id, int study_arm) {
		super();
		this.id = id;
		this.study_arm = study_arm;		
	}		
	
	public void setInterpol_study_day(int[] interpol_study_day) {
		this.interpol_study_day = interpol_study_day;		
	}


	public void setInterpol_QALY(float[] interpol_QALY) {
		interpol[INDEX_QALY] = interpol_QALY;
	}

	public void setInterpol_CD4_count(float[] interpol_CD4_count) {
		interpol[INDEX_CD4] = interpol_CD4_count;
	}

	public long getId() {
		return id;
	}

	public int getStudy_arm() {
		return study_arm;
	}

	public int getDayPos(int day) {
		if(interpol_study_day == null) {
			throw new NullPointerException("interpol_study_day not defined.");
		}		
		return Arrays.binarySearch(interpol_study_day, day);	
	}
	
	
	
	public float getQALYByPos(int day) {
		return interpolation_linear_by_pos(interpol[INDEX_QALY],day, getDayPos(day));		
	}
	
	public float getCD4ByPos(int day) {
		return interpolation_linear_by_pos(interpol[INDEX_CD4],day,getDayPos(day));		
	}		
	
	public float getQALYByPos(int day, int pos) {
		return interpolation_linear_by_pos(interpol[INDEX_QALY],day,pos);		
	}
	
	public float getCD4ByPos(int day, int pos) {
		return interpolation_linear_by_pos(interpol[INDEX_CD4],day,pos);		
	}		
	
	
	public double getQALYByFit(int day) {
		return getPolyFit(polyFit[INDEX_QALY], day);
	}
	
	public double getCD4ByFit(int day) {
		return getPolyFit(polyFit[INDEX_CD4], day);
	}
	
	private double getPolyFit(double[] polyCoff, int day) {
		if(polyCoff == null) {
			throw new NullPointerException("Fitting not defined.");
		}else {			
			float res = 0;			
			for(int deg = 0; deg < polyCoff.length; deg++) {				
				res += polyCoff[deg] * Math.pow(day, deg);				
			}							
			return res;			
		}							
	}
	
	public void generatePolyFit(int deg) {
		for(int i = 0; i < interpol.length; i++) {
			if(interpol[i] != null) {
				final WeightedObservedPoints obs = new WeightedObservedPoints();
				for(int j = 0; j < interpol[i].length; j++) {
					obs.add(interpol_study_day[j], interpol[i][j]);
				}
				final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(deg);
				polyFit[i] = fitter.fit(obs.toList());							   
			}
			
		}
		
	}
	
	
	private float interpolation_linear_by_pos(float[] src_val, int day, int pos) {
		if (src_val == null) {
			throw new NullPointerException("Input source values not defined.");
		}
		if (interpol_study_day == null) {
			throw new NullPointerException("Field interpol_study_day not defined.");
		}
		if (pos >= 0) {
			return src_val[pos];
		} else {
			pos = ~pos; // insert point
			if (pos > 0 && pos < src_val.length) {
				return src_val[pos - 1] + (src_val[pos] - src_val[pos - 1]) * (day - interpol_study_day[pos - 1])
						/ (interpol_study_day[pos] - interpol_study_day[pos - 1]);
			} else {
				// Using ending point
				return 	src_val[src_val.length-1];						
				
				//throw new IllegalArgumentException(String.format("Day %s is outside interpolation range of [%d,%d]",
				//		day, interpol_study_day[0], interpol_study_day[interpol_study_day.length - 1]));
			}
		}

	}
	
	

	
	

	

}
