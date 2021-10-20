package util;

import java.util.Arrays;

public class QALY_SF12 {

	// From Ware et al, "SF-12: how to score the SF-12 ...", 2020

	// Q1: General Health
	public static final int SF12_GH1 = 0;
	// Q2a: Limitation in moderate physical activities
	public static final int SF12_PF02 = SF12_GH1 + 1;
	// Q2b: Limitation in climbing stairs
	public static final int SF12_PF04 = SF12_PF02 + 1;
	// Q3a: Accomplished less due to physical health
	public static final int SF12_RP2 = SF12_PF04 + 1;
	// Q3b: Limited in kind of work or activities
	public static final int SF12_RP3 = SF12_RP2 + 1;
	// Q4a: Accomplished less due to emotional problem
	public static final int SF12_RE2 = SF12_RP3 + 1;
	// Q4b: Not careful in work or activities due to emotional problem
	public static final int SF12_RE3 = SF12_RE2 + 1;
	// Q5: Pain interference with work inside or outside home
	public static final int SF12_BP2 = SF12_RE3 + 1;
	// Q6a: Feel calm and peaceful
	public static final int SF12_MH3 = SF12_BP2 + 1;
	// Q6b: Having a lot of energy
	public static final int SF12_VT2 = SF12_MH3 + 1;
	// Q6c: Feeling downhearted or blue
	public static final int SF12_MH4 = SF12_VT2 + 1;
	// Q7: Interference of physical health or emotional problems with social
	// activities
	public static final int SF12_SF2 = SF12_MH4 + 1;

	public static final int SF112_LENGTH = SF12_SF2 + 1; // 12

	public static final int PCS_12 = 0;
	public static final int MCS_12 = PCS_12 + 1;
	public static final int SUMMARY_SCALE_LENGTH = MCS_12 + 1;

	// From Ware et al, "SF-12: how to score the SF-12 ...", 2020, page 25
	public static float[] calulateSummaryScale(int[] sf12) {

		float[] summaryScale = new float[SUMMARY_SCALE_LENGTH];

		// Constant
		summaryScale[PCS_12] = 56.57706f;
		summaryScale[MCS_12] = 60.75781f;

		for (int i = 0; i < sf12.length; i++) {
			int val = sf12[i];

			switch (i) {
			
			case SF12_GH1:
				switch (val) {
				case 1: // Excellence					
					break;
				case 2: // V. good
					summaryScale[PCS_12] += -1.31872f;
					summaryScale[MCS_12] += -0.06064f;					
					break;
				case 3: // Good
					summaryScale[PCS_12] += -3.02396f;
					summaryScale[MCS_12] +=  0.03482f;
					break;
				case 4: // Fair
					summaryScale[PCS_12] += -5.56461f;
					summaryScale[MCS_12] += -0.16891f;
					break;
				case 5: // Poor
					summaryScale[PCS_12] += -8.37399f;
					summaryScale[MCS_12] += -1.71175f;
					break;
				default:
					System.err.println("Error in SF-12 index #" + i + ": " + Arrays.toString(sf12));
				}
				break;
			case SF12_PF02:
				switch (val) {
				case 1: // Limit a lot	
					summaryScale[PCS_12] += -7.23216f;
					summaryScale[MCS_12] +=  3.93115f;
					break;
				case 2: // Limit little
					summaryScale[PCS_12] += -3.45555f;
					summaryScale[MCS_12] +=  1.86840f;	
					break;
				case 3: // No limit				
					break;
				default:
					System.err.println("Error in SF-12 index #" + i + ": " + Arrays.toString(sf12));				
				}				
				break;				
			case SF12_PF04:
				switch (val) {
				case 1: // Limit a lot	
					summaryScale[PCS_12] += -6.24397f;
					summaryScale[MCS_12] +=  2.68282f;
					break;
				case 2: // Limit little
					summaryScale[PCS_12] += -2.73557f;
					summaryScale[MCS_12] +=  1.43103f;
					break;
				case 3: // No limit				
					break;
				default:
					System.err.println("Error in SF-12 index #" + i + ": " + Arrays.toString(sf12));				
				}										
				break;	
				
			case SF12_RP2:
				if(val!= 5) { // Yes
					summaryScale[PCS_12] += -4.61617f;
					summaryScale[MCS_12] +=  1.44060f;
				}			
				break;
			case SF12_RP3:
				if(val!= 5) { // Yes
					summaryScale[PCS_12] += -5.51747f;
					summaryScale[MCS_12] +=  1.66968f;
				}				
				break;
			default:
				System.err.println("Error in SF-12 index #" + i + ": " + Arrays.toString(sf12));
			}

		}

		return summaryScale;
	}

}
