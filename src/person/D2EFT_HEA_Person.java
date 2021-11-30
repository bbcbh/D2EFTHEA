package person;

import java.util.Arrays;

public class D2EFT_HEA_Person{
	private long id;
	private int study_arm;

	private int[] interpol_study_day;
	private float[] interpol_QALY;
	private float[] interpol_CD4_count;		

	public static final int STUDY_ARM_SOC = 0;
	public static final int STUDY_ARM_DOL = STUDY_ARM_SOC + 1;
	public static final int STUDY_ARM_D2N = STUDY_ARM_DOL + 1;

	public D2EFT_HEA_Person(long id, int study_arm) {
		super();
		this.id = id;
		this.study_arm = study_arm;		
	}		
	
	public void setInterpol_study_day(int[] interpol_study_day) {
		this.interpol_study_day = interpol_study_day;		
	}


	public void setInterpol_QALY(float[] interpol_QALY) {
		this.interpol_QALY = interpol_QALY;
	}

	public void setInterpol_CD4_count(float[] interpol_CD4_count) {
		this.interpol_CD4_count = interpol_CD4_count;
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
	
	public float getQALYByPos(int day, int pos) {
		return interpolation_linear_by_pos(interpol_QALY,day,pos);		
	}
	
	public float getCD4ByPos(int day, int pos) {
		return interpolation_linear_by_pos(interpol_CD4_count,day,pos);		
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
				throw new IllegalArgumentException(String.format("Day %s is outside interpolation range of [%d,%d]",
						day, interpol_study_day[0], interpol_study_day[interpol_study_day.length - 1]));
			}
		}

	}
	
	

	
	

	

}
