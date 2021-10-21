package util;

public class D2EFT_QALY_SF12 {

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

	public static final String[] SF12_QUESTIONS_TEXT = new String[] { "Would say your health is ... ",
			"Your health limited moderate activities", "Your health limited climbing stairs",
			"Accomplished less than you would like due to physical health",
			"Were limited in the kind of work or other activities due to physical health",
			"Accomplished less than you would like due to emotional problems",
			"Didn't do work or other activities as carefully as usual",
			"Pain interfere with normal work", "Feel calm and peaceful", "Had a lot of energy",
			"Felt downhearted and depressed",
			"Physical health or emotional problems interfere with social activities", };

	public static final String[][] SF12_Options = new String[][] {
			new String[] { "1: Excellent", "2: Very Good", "3: Good", "4: Fair", "5: Poor" },
			new String[] { "1: Limited a lot", "2: Limited a little", "3: Not limited at all" },
			new String[] { "1: Limited a lot", "2: Limited a little", "3: Not limited at all" },
			new String[] { "1: All of the time", "2: Most of the time", "3: Some of the time",
					"4: A little of the time", "5: None of the time" },
			new String[] { "1: All of the time", "2: Most of the time", "3: Some of the time",
					"4: A little of the time", "5: None of the time" },
			new String[] { "1: All of the time", "2: Most of the time", "3: Some of the time",
					"4: A little of the time", "5: None of the time" },
			new String[] { "1: All of the time", "2: Most of the time", "3: Some of the time",
					"4: A little of the time", "5: None of the time" },
			new String[] { "1: Not at all", "2: A little bit", "3: Moderately", "4: Quite a bit", "5: Extremely" },
			new String[] { "1: All of the time", "2: Most of the time", "3: Some of the time",
					"4: A little of the time", "5: None of the time" },
			new String[] { "1: All of the time", "2: Most of the time", "3: Some of the time",
					"4: A little of the time", "5: None of the time" },
			new String[] { "1: All of the time", "2: Most of the time", "3: Some of the time",
					"4: A little of the time", "5: None of the time" },
			new String[] { "1: All of the time", "2: Most of the time", "3: Some of the time",
					"4: A little of the time", "5: None of the time" }, };

	public static final int[] SF12_Options_Default = new int[] { 1, 3, 3, 5, 5, 5, 5, 1, 1, 1, 5, 5 };

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
					summaryScale[MCS_12] += 0.03482f;
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
					throw new IllegalArgumentException();
				}
				break;
			case SF12_PF02:
				switch (val) {
				case 1: // Limit a lot
					summaryScale[PCS_12] += -7.23216f;
					summaryScale[MCS_12] += 3.93115f;
					break;
				case 2: // Limit little
					summaryScale[PCS_12] += -3.45555f;
					summaryScale[MCS_12] += 1.86840f;
					break;
				case 3: // No limit
					break;
				default:
					throw new IllegalArgumentException();
				}
				break;
			case SF12_PF04:
				switch (val) {
				case 1: // Limit a lot
					summaryScale[PCS_12] += -6.24397f;
					summaryScale[MCS_12] += 2.68282f;
					break;
				case 2: // Limit little
					summaryScale[PCS_12] += -2.73557f;
					summaryScale[MCS_12] += 1.43103f;
					break;
				case 3: // No limit
					break;
				default:
					throw new IllegalArgumentException();
				}
				break;

			case SF12_RP2:
				if (val != 5) { // Yes
					summaryScale[PCS_12] += -4.61617f;
					summaryScale[MCS_12] += 1.44060f;
				}
				break;
			case SF12_RP3:
				if (val != 5) { // Yes
					summaryScale[PCS_12] += -5.51747f;
					summaryScale[MCS_12] += 1.66968f;
				}
				break;
			case SF12_RE2:
				if (val != 5) { // Yes
					summaryScale[PCS_12] += 3.04365f;
					summaryScale[MCS_12] += -6.82672f;
				}
				break;
			case SF12_RE3:
				if (val != 5) { // Yes
					summaryScale[PCS_12] += 2.32091f;
					summaryScale[MCS_12] += -5.69921f;
				}
				break;
			case SF12_BP2:
				switch (val) {
				case 1: // Not at all
					break;
				case 2: // A little bit
					summaryScale[PCS_12] += -3.80130f;
					summaryScale[MCS_12] += 0.90384f;
					break;
				case 3: // Moderately
					summaryScale[PCS_12] += -6.50522f;
					summaryScale[MCS_12] += 1.49384f;
					break;
				case 4: // Quite a bit
					summaryScale[PCS_12] += -8.38063f;
					summaryScale[MCS_12] += 1.76691f;
					break;
				case 5: // Extremely
					summaryScale[PCS_12] += -11.25544f;
					summaryScale[MCS_12] += 1.48619f;
					break;
				default:
					throw new IllegalArgumentException();
				}
				break;
			case SF12_MH3:
				switch (val) {
				case 1: // All the time
					break;
				case 2: // Most of the time
					summaryScale[PCS_12] += 0.66514f;
					summaryScale[MCS_12] += -1.94949f;
					break;
				case 3: // Some of the time
					summaryScale[PCS_12] += 2.37241f;
					summaryScale[MCS_12] += -6.31121f;
					break;
				case 4: // A little of the time
					summaryScale[PCS_12] += 2.90426f;
					summaryScale[MCS_12] += -7.92717f;
					break;
				case 5: // None of the time
					summaryScale[PCS_12] += 3.46638f;
					summaryScale[MCS_12] += -10.19085f;
					break;
				default:
					throw new IllegalArgumentException();
				}
				break;
			case SF12_VT2:
				switch (val) {
				case 1: // All the time
					break;
				case 2: // Most of the time
					summaryScale[PCS_12] += -0.42251f;
					summaryScale[MCS_12] += -0.92057f;
					break;
				case 3: // Some of the time
					summaryScale[PCS_12] += -1.61850f;
					summaryScale[MCS_12] += -3.29805f;
					break;
				case 4: // A little of the time
					summaryScale[PCS_12] += -2.02168f;
					summaryScale[MCS_12] += -4.88962f;
					break;
				case 5: // None of the time
					summaryScale[PCS_12] += -2.44706f;
					summaryScale[MCS_12] += -6.02409f;
					break;
				default:
					throw new IllegalArgumentException();
				}
				break;
			case SF12_MH4:
				switch (val) {
				case 1: // All the time
					summaryScale[PCS_12] += 4.61446f;
					summaryScale[MCS_12] += -16.15395f;
					break;
				case 2: // Most of the time
					summaryScale[PCS_12] += 3.41593f;
					summaryScale[MCS_12] += -10.77911f;
					break;
				case 3: // Some of the time
					summaryScale[PCS_12] += 1.28044f;
					summaryScale[MCS_12] += -4.59055f;
					break;
				case 4: // A little of the time
					summaryScale[PCS_12] += 0.41188f;
					summaryScale[MCS_12] += -1.95934f;
					break;
				case 5: // None of the time
					break;
				default:
					throw new IllegalArgumentException();
				}
				break;
			case SF12_SF2:
				switch (val) {
				case 1: // All the time
					summaryScale[PCS_12] += -0.33682f;
					summaryScale[MCS_12] += -6.29724f;
					break;
				case 2: // Most of the time
					summaryScale[PCS_12] += -0.94342f;
					summaryScale[MCS_12] += -8.26066f;
					break;
				case 3: // Some of the time
					summaryScale[PCS_12] += -0.18043f;
					summaryScale[MCS_12] += -5.63286f;
					break;
				case 4: // A little of the time
					summaryScale[PCS_12] += 0.11038f;
					summaryScale[MCS_12] += -3.13896f;
					break;
				case 5: // None of the time
					break;
				default:
					throw new IllegalArgumentException();
				}
				break;
			default:
				throw new IllegalArgumentException();
			}

		}

		return summaryScale;
	}

}
