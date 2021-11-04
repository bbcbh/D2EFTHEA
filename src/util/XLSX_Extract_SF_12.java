package util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import chart.D2EFT_ChartFactory;
import gui.D2EFT_SF12_Form;

public class XLSX_Extract_SF_12 extends XLSX_Extract {

	public static final String[] STUDY_ARM = new String[] { "SOC", "DOL", "D2N" };
	public static final String[] VISIT_NUM = new String[] { "Day 0", "Wk48", "Wk96" };

	protected int[][] resp_sf12;
	protected Date[] dateCompleted;

	protected static final int RESP_ID = 0;
	protected static final int RESP_SITE = RESP_ID + 1;
	protected static final int RESP_ARM = RESP_SITE + 1;
	protected static final int RESP_VISIT = RESP_ARM + 1;
	protected static final int RESP_SF12_START = RESP_VISIT + 1;
	protected static final int RESP_LENGTH = RESP_SF12_START + D2EFT_QALY_SF12.SF12_LENGTH;

	// Transient objects for look up
	protected transient Collection<int[]>[][] resp_map = new Collection[STUDY_ARM.length][VISIT_NUM.length];
	protected transient HashMap<Integer, int[]> resp_index_by_pid = new HashMap<>();

	@Override
	protected void extractWorkbook(File inpath) {

		long tic = System.currentTimeMillis();

		super.extractWorkbook(inpath);
		XSSFSheet srcSheet = sheets[0];
		int numRows = srcSheet.getPhysicalNumberOfRows();

		resp_sf12 = new int[numRows][RESP_LENGTH]; // ID, SITE, STUDY_ARM, VISIT_NUM, SF12
		dateCompleted = new Date[numRows]; // Date completed;

		for (int[] s : resp_sf12) {
			Arrays.fill(s, -1);
		}

		int rowNum = 0;
		int rPt = 0;
		int val;
		String[] toMatch;

		for (Row row : srcSheet) {
			if (rowNum > 0) { // Skip the first row
				int colNum = 0;
				for (Cell cell : row) {
					try {
						switch (colNum) {
						case 0: // ID
						case 1: // SITE
							resp_sf12[rPt][colNum] = Integer.parseInt(cell.getStringCellValue());
							break;
						case 2: // ARM
						case 3: // VISIT
							toMatch = colNum == RESP_ARM ? STUDY_ARM : VISIT_NUM;
							val = matchString(cell.getStringCellValue(), toMatch);
							if (val < 0) {
								throw new IllegalStateException();
							} else {
								resp_sf12[rPt][colNum] = val;
							}
							break;
						case 4: // Date completed
							dateCompleted[rPt] = cell.getDateCellValue();
							break;
						default:
							// SF_12
							int sf_12_Q_num = colNum - 5;
							if (sf_12_Q_num < D2EFT_QALY_SF12.SF12_LENGTH) {
								// Offset by date completed
								toMatch = D2EFT_QALY_SF12.SF12_Options[sf_12_Q_num];
								val = matchString(cell.getStringCellValue(), toMatch);
								if (val < 0) {
									throw new IllegalStateException();
								} else {
									resp_sf12[rPt][colNum - 1] = val;
								}

							}
						}
					} catch (IllegalStateException ex) {
						String output = cell.getCellType() == CellType.NUMERIC
								? Double.toString(cell.getNumericCellValue())
								: cell.getStringCellValue();

						printOutput(String.format("Error in formatting cell at (%d, %s): [%s]\n", rowNum + 1,
								getColumnName(colNum + 1), output));

					}
					colNum++;
				}
				rPt++;
			}
			rowNum++;
		}
		printOutput(String.format("Extraction completed. Time required = %.3f s\n",
				(System.currentTimeMillis() - tic) / 1000f));

		// Mapping for quicker lookup
		Collection<int[]> sf12_resp;

		for (int r = 0; r < resp_sf12.length; r++) {

			int[] resp = resp_sf12[r];
			if (resp[RESP_ARM] != -1 && resp[RESP_VISIT] != -1) {
				sf12_resp = resp_map[resp[RESP_ARM]][resp[RESP_VISIT]];
				if (sf12_resp == null) {
					resp_map[resp[RESP_ARM]][resp[RESP_VISIT]] = new ArrayList<>();
					sf12_resp = resp_map[resp[RESP_ARM]][resp[RESP_VISIT]];
				}
				sf12_resp.add(Arrays.copyOfRange(resp, RESP_SF12_START, resp.length));

				Integer pid = resp[RESP_ID];
				int[] index = resp_index_by_pid.get(pid);
				if (index == null) {
					index = new int[VISIT_NUM.length];
					Arrays.fill(index, -1);
					resp_index_by_pid.put(pid, index);
				}
				index[resp[RESP_VISIT]] = r;
			}

		}

	}

	protected static int matchString(String ent, String[] toMatch) {
		for (int i = 0; i < toMatch.length; i++) {
			if (toMatch[i].equals(ent)) {
				return i;
			}
		}
		return -1;
	}

	public Collection<int[]> response_lookup(int study_arm, int visit) {
		ArrayList<int[]> sf_12_resp = new ArrayList<>();
		for (int[] resp : resp_sf12) {
			if (study_arm == -1 || resp[RESP_ARM] == study_arm) {
				if (visit == -1 || resp[RESP_VISIT] == visit) {
					sf_12_resp.add(Arrays.copyOfRange(resp, RESP_SF12_START, resp.length));
				}
			}
		}
		return resp_map[study_arm][visit];
	}

	public int[] response_lookup_by_row(int row) {
		return resp_sf12[row];
	}

	public HashMap<Integer, int[]> getResp_index_by_pid() {
		return resp_index_by_pid;
	}

	public static void main(String[] args) throws IOException {

		File xlsx_file = null;
		boolean showPlot = true;
		boolean genPlot = true;

		if (args.length > 0) {
			xlsx_file = new File(args[0]);

			for (int a = 1; a < args.length; a++) {
				if ("-noGen".equals(args[a])) {
					genPlot = false;
				}
				if ("-noShow".equals(args[a])) {
					showPlot = false;
				}
			}

		}

		if (xlsx_file == null || !xlsx_file.isFile()) {
			javax.swing.JFileChooser jc = new javax.swing.JFileChooser();
			jc.setFileFilter(new javax.swing.filechooser.FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().toLowerCase().endsWith(".xlsx");
				}

				@Override
				public String getDescription() {
					return "XLSX file";
				}

			});
			int res = jc.showOpenDialog(null);
			if (res == javax.swing.JFileChooser.APPROVE_OPTION) {
				xlsx_file = jc.getSelectedFile();
			}
		}

		if (xlsx_file != null) {

			XLSX_Extract_SF_12 wk_extract = new XLSX_Extract_SF_12();
			wk_extract.extractWorkbook(xlsx_file);

			Collection<int[]> sf12_resp;
			for (int studyArm = 0; studyArm < STUDY_ARM.length; studyArm++) {
				for (int visitNum = 0; visitNum < VISIT_NUM.length; visitNum++) {
					sf12_resp = wk_extract.response_lookup(studyArm, visitNum);
					System.out.println(String.format("# results for (%s, %s) = %d", STUDY_ARM[studyArm],
							VISIT_NUM[visitNum], sf12_resp.size()));
				}
			}
																					

			if (genPlot || showPlot) {

				// Day 0 result comparison

				JFreeChart[] day00_Charts = generate_Day00_Chart(wk_extract);

				// PC-12 and MC-12 comparison

				JFreeChart[] summmaryCharts = generate_Summary_Chart(wk_extract);

				if (showPlot) {
					for (JFreeChart c : day00_Charts) {
						D2EFT_ChartFactory.showJFreeChart(c);
					}
					for (JFreeChart c : summmaryCharts) {
						D2EFT_ChartFactory.showJFreeChart(c);
					}
				}

				if (genPlot) {

					File pngFile;

					pngFile = new File(xlsx_file.getParent(), "SF12_GH1_Wk0.png");
					ChartUtils.saveChartAsPNG(pngFile, day00_Charts[0], 1000, 700, null, true, 0);
					pngFile = new File(xlsx_file.getParent(), "SF12_Summary_Wk0.png");
					ChartUtils.saveChartAsPNG(pngFile, day00_Charts[1], 1000, 700, null, true, 0);

					pngFile = new File(xlsx_file.getParent(), "SF12_PCS-12.png");
					ChartUtils.saveChartAsPNG(pngFile, summmaryCharts[0], 1000, 700, null, true, 0);
					pngFile = new File(xlsx_file.getParent(), "SF12_MCS-12.png");
					ChartUtils.saveChartAsPNG(pngFile, summmaryCharts[1], 1000, 700, null, true, 0);
					pngFile = new File(xlsx_file.getParent(), "SF12_SF-6D_Full.png");
					ChartUtils.saveChartAsPNG(pngFile, summmaryCharts[2], 1000, 700, null, true, 0);
					pngFile = new File(xlsx_file.getParent(), "SF12_SF-6D_Consistent.png");
					ChartUtils.saveChartAsPNG(pngFile, summmaryCharts[3], 1000, 700, null, true, 0);
				}
			}

		}

		System.out.println("All done!");

	}

	public JFreeChart[] generate_Summary_Diff_Chart() {
		return generate_Summary_Chart(this);
	}

	public static JFreeChart[] generate_Summary_Chart(XLSX_Extract_SF_12 wk_extract) {
		HashMap<Integer, int[]> resp_by_id = wk_extract.getResp_index_by_pid();

		ArrayList<Float>[][][] SUMMARY_DIFF = new ArrayList[STUDY_ARM.length][VISIT_NUM.length
				- 1][D2EFT_QALY_SF12.SUMMARY_SCALE_LENGTH];

		HashMap<Integer, float[]> sf_6d_dataset = new HashMap<>();
		HashMap<Integer, Integer> pid_study_arm_map = new HashMap<>();

		for (Integer pid : resp_by_id.keySet()) {

			int[] rowNum = resp_by_id.get(pid);

			if (rowNum[0] == -1) {
				wk_extract.printOutput(String.format("Day 0 visit data missing for PID: %d\n", pid));

			} else {
				int[] resp_wk00 = wk_extract.response_lookup_by_row(rowNum[0]);
				int studyArm = resp_wk00[RESP_ARM];

				pid_study_arm_map.put(pid, studyArm);

				float[] summary_wk00 = D2EFT_QALY_SF12
						.calulateSummaryScale(Arrays.copyOfRange(resp_wk00, RESP_SF12_START, resp_wk00.length), 1);
				
				float[] sf6d_by_pid = sf_6d_dataset.get(pid);
				
				if(sf6d_by_pid == null) {
					sf6d_by_pid = new float[VISIT_NUM.length];
					Arrays.fill(sf6d_by_pid,-1f);
					sf_6d_dataset.put(pid, sf6d_by_pid);
				}				
				sf6d_by_pid[0] = summary_wk00[D2EFT_QALY_SF12.SF_6D_CONSISTENT];																

				int[] resp_wk48 = null;
				int[] resp_wk96 = null;

				if (rowNum[1] > 0) {
					resp_wk48 = wk_extract.response_lookup_by_row(rowNum[1]);
					if (resp_wk48[RESP_ARM] != studyArm) {
						wk_extract.printOutput(String.format(
								"PID %d: Wk 48 visit study arm mismatch." + " Use Day 0 study arm (%s) instead.\n", pid,
								STUDY_ARM[studyArm]));
					}

					float[] summary_wk48 = insertSummaryDiff(SUMMARY_DIFF, studyArm, 0, summary_wk00, resp_wk48);
					if (summary_wk48 != null) {
						sf6d_by_pid[1] = summary_wk48[D2EFT_QALY_SF12.SF_6D_CONSISTENT];
					}

				}
				if (rowNum[2] > 0) {
					resp_wk96 = wk_extract.response_lookup_by_row(rowNum[2]);
					if (resp_wk96[RESP_ARM] != studyArm) {
						wk_extract.printOutput(String.format(
								"PID %d: Wk 96 visit study arm mismatch for" + " Use Day 0 study arm (%s) instead.\n",
								pid, STUDY_ARM[studyArm]));
					}
					if (resp_wk48 == null) {
						wk_extract.printOutput(
								String.format("PID %d: Wk 48 visit missing despite having Wk 96 resp.\n", pid));
					}
					float[] summary_wk96 = insertSummaryDiff(SUMMARY_DIFF, studyArm, 1, summary_wk00, resp_wk96);
					if (summary_wk96 != null) {
						sf6d_by_pid[2] = summary_wk96[D2EFT_QALY_SF12.SF_6D_CONSISTENT];						
					}

				}

			}

		}

		DefaultBoxAndWhiskerCategoryDataset[] sf12_diff_dataset = new DefaultBoxAndWhiskerCategoryDataset[D2EFT_QALY_SF12.SUMMARY_SCALE_LENGTH];

		for (int s = 0; s < sf12_diff_dataset.length; s++) {
			sf12_diff_dataset[s] = new DefaultBoxAndWhiskerCategoryDataset();
		}

		for (int studyArm = 0; studyArm < STUDY_ARM.length; studyArm++) {
			for (int visitDiffNum = 0; visitDiffNum < SUMMARY_DIFF[studyArm].length; visitDiffNum++) {
				for (int s = 0; s < sf12_diff_dataset.length; s++) {
					sf12_diff_dataset[s].add(SUMMARY_DIFF[studyArm][visitDiffNum][s], STUDY_ARM[studyArm],
							String.format("%s - %s", VISIT_NUM[visitDiffNum + 1], VISIT_NUM[0]));					
				}									
			}
		}
		
		
		ArrayList<Float>[] sf_6d_wk96_wk48 = new ArrayList[STUDY_ARM.length];
		
		for(int i = 0; i < sf_6d_wk96_wk48.length; i++) {
			sf_6d_wk96_wk48[i] = new ArrayList<Float>();
		}
		


		for (Integer pid : sf_6d_dataset.keySet()) {
			float[] sf6d_by_pid = sf_6d_dataset.get(pid);
			int studyArm = pid_study_arm_map.get(pid);
			if (sf6d_by_pid[2] != -1 && sf6d_by_pid[0] != -1) {
				sf_6d_wk96_wk48[studyArm].add(sf6d_by_pid[2] - sf6d_by_pid[1]);				
			}
		}
		
		for(int studyArm = 0; studyArm < STUDY_ARM.length; studyArm++) {		
		sf12_diff_dataset[D2EFT_QALY_SF12.SF_6D_CONSISTENT].add(sf_6d_wk96_wk48[studyArm],
				STUDY_ARM[studyArm], String.format("%s - %s", VISIT_NUM[2], VISIT_NUM[1]));
		}
		
		JFreeChart chart_SF12_pcs_12_diff = ChartFactory.createBoxAndWhiskerChart("PCS-12", null, null,
				sf12_diff_dataset[D2EFT_QALY_SF12.PCS_12], true);
		((BoxAndWhiskerRenderer) chart_SF12_pcs_12_diff.getCategoryPlot().getRenderer()).setMeanVisible(false);

		JFreeChart chart_SF12_mcs_12_diff = ChartFactory.createBoxAndWhiskerChart("MCS-12", null, null,
				sf12_diff_dataset[D2EFT_QALY_SF12.MCS_12], true);
		((BoxAndWhiskerRenderer) chart_SF12_mcs_12_diff.getCategoryPlot().getRenderer()).setMeanVisible(false);

		JFreeChart chart_SF12_sf_6d_full_diff = ChartFactory.createBoxAndWhiskerChart("SF-6D (Full)", null, null,
				sf12_diff_dataset[D2EFT_QALY_SF12.SF_6D_FULL], true);
		((BoxAndWhiskerRenderer) chart_SF12_sf_6d_full_diff.getCategoryPlot().getRenderer()).setMeanVisible(false);

		JFreeChart chart_SF12_sf_6d_consistent_diff = ChartFactory.createBoxAndWhiskerChart("SF-6D", null, null,
				sf12_diff_dataset[D2EFT_QALY_SF12.SF_6D_CONSISTENT], true);
		((BoxAndWhiskerRenderer) chart_SF12_sf_6d_consistent_diff.getCategoryPlot().getRenderer())
				.setMeanVisible(false);

		JFreeChart[] summmaryChart = new JFreeChart[] { chart_SF12_pcs_12_diff, chart_SF12_mcs_12_diff,
				chart_SF12_sf_6d_full_diff, chart_SF12_sf_6d_consistent_diff };
		return summmaryChart;
	}

	private static float[] insertSummaryDiff(ArrayList<Float>[][][] SUMMARY_DIFF, int studyArm, int visitDiffNum,
			float[] summary_wk00, int[] resp_wk_diff) {

		boolean hasNA = false;

		for (int s = RESP_SF12_START; s < resp_wk_diff.length && !hasNA; s++) {
			if (resp_wk_diff[s] < 0) {
				hasNA = true;
			}
		}

		if (!hasNA) {
			float[] summary_diff = D2EFT_QALY_SF12
					.calulateSummaryScale(Arrays.copyOfRange(resp_wk_diff, RESP_SF12_START, resp_wk_diff.length), 1);
			for (int s = 0; s < SUMMARY_DIFF[studyArm][visitDiffNum].length; s++) {
				if (SUMMARY_DIFF[studyArm][visitDiffNum][s] == null) {
					SUMMARY_DIFF[studyArm][visitDiffNum][s] = new ArrayList<Float>();
				}
				SUMMARY_DIFF[studyArm][visitDiffNum][s].add(summary_diff[s] - summary_wk00[s]);
			}
			return summary_diff;
		}
		return null;
	}

	public JFreeChart[] generate_Day00_Chart() {
		return generate_Day00_Chart(this);
	}

	public static JFreeChart[] generate_Day00_Chart(XLSX_Extract_SF_12 wk_extract) {
		Collection<int[]> sf12_resp;
		int[][] sf12_gh1_Freq = new int[STUDY_ARM.length][D2EFT_QALY_SF12.SF12_Options[0].length];
		Number[][][] summary_val = new Number[STUDY_ARM.length][D2EFT_QALY_SF12.SUMMARY_SCALE_LENGTH][];

		for (int studyArm = 0; studyArm < STUDY_ARM.length; studyArm++) {
			sf12_resp = wk_extract.response_lookup(studyArm, 0);
			int pt = 0;
			summary_val[studyArm][D2EFT_QALY_SF12.PCS_12] = new Number[sf12_resp.size()];
			summary_val[studyArm][D2EFT_QALY_SF12.MCS_12] = new Number[sf12_resp.size()];

			for (int[] resp : sf12_resp) {
				sf12_gh1_Freq[studyArm][resp[D2EFT_QALY_SF12.SF12_GH1]]++;
				float[] summary = D2EFT_QALY_SF12.calulateSummaryScale(resp, 1);
				summary_val[studyArm][D2EFT_QALY_SF12.PCS_12][pt] = summary[D2EFT_QALY_SF12.PCS_12];
				summary_val[studyArm][D2EFT_QALY_SF12.MCS_12][pt] = summary[D2EFT_QALY_SF12.MCS_12];
				pt++;
			}
		}

		DefaultCategoryDataset sf12_gh1_dataset = new DefaultCategoryDataset();
		DefaultBoxAndWhiskerCategoryDataset sf12_summary_dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int studyArm = 0; studyArm < STUDY_ARM.length; studyArm++) {
			for (int c = 0; c < sf12_gh1_Freq[studyArm].length; c++) {
				sf12_gh1_dataset.addValue(
						((double) sf12_gh1_Freq[studyArm][c]) / wk_extract.response_lookup(studyArm, 0).size(),
						STUDY_ARM[studyArm], D2EFT_QALY_SF12.SF12_Options[0][c]);
			}

			sf12_summary_dataset.add(List.of(summary_val[studyArm][D2EFT_QALY_SF12.PCS_12]), STUDY_ARM[studyArm],
					"PCS-12");

			sf12_summary_dataset.add(List.of(summary_val[studyArm][D2EFT_QALY_SF12.MCS_12]), STUDY_ARM[studyArm],
					"MCS-12");
		}

		JFreeChart chart_SF12_GH1_Wk0 = ChartFactory.createBarChart(
				D2EFT_QALY_SF12.SF12_QUESTIONS_TEXT[D2EFT_QALY_SF12.SF12_GH1], "", "Relative Frequency",
				sf12_gh1_dataset);

		JFreeChart chart_SF12_Summary_Wk0 = ChartFactory.createBoxAndWhiskerChart(null, null, null,
				sf12_summary_dataset, true);
		((BoxAndWhiskerRenderer) chart_SF12_Summary_Wk0.getCategoryPlot().getRenderer()).setMeanVisible(false);

		return new JFreeChart[] { chart_SF12_GH1_Wk0, chart_SF12_Summary_Wk0 };
	}
}
