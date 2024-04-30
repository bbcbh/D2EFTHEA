package util;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import chart.D2EFT_ChartFactory;

public class XLSX_Extract_HEA extends XLSX_Extract {

	public static final String[] STUDY_ARM = new String[] { "SOC", "DOL", "D2N" };
	public static final String[] VISIT_NUM = new String[] { "Day 0", "Wk48", "Wk96" };

	public static final String WORKSHEET_NAME_SF_12 = "SF12";
	public static final String WORKSHEET_NAME_HEALTH_UTIL = "Health Utility";

	protected int[][] resp_sf12;
	protected Date[] dateCompleted_sf12;

	public static final int SF12_RESP_ID = 0;
	public static final int SF12_RESP_SITE = SF12_RESP_ID + 1;
	public static final int SF12_RESP_ARM = SF12_RESP_SITE + 1;
	public static final int SF12_RESP_VISIT = SF12_RESP_ARM + 1;
	public static final int SF12_RESP_SF12_START = SF12_RESP_VISIT + 1;
	public static final int SF12_RESP_LENGTH = SF12_RESP_SF12_START + D2EFT_QALY_SF12.SF12_LENGTH;

	// From 20240305 version
	public static final int COL_INDEX_SF_12_ID = 0;
	public static final int COL_INDEX_SF_12_SITE = 4;
	public static final int COL_INDEX_SF_12_ARM = 1;
	public static final int COL_INDEX_SF_12_VISIT = 60;
	public static final int COL_INDEX_SF_12_DATECOMPLETED = 11;
	public static final int COL_INDEX_SF_12_RESP_START = 14;
	public static final int COL_SKIP_SF_12_RESP = 3;
	
	public static int scaleOffset_SF6D = 0;

	public static final HashMap<Integer, Integer> sf12_lookup = new HashMap<>();

	protected int[][] resp_healthUtil;
	protected Date[] dateCompleted_healthUtil;
	public static final int HEALTHUTIL_RESP_ID = 0;

	public static final int HEALTHUTIL_RESP_SITE = HEALTHUTIL_RESP_ID + 1;
	public static final int HEALTHUTIL_RESP_VISIT = HEALTHUTIL_RESP_SITE + 1;
	public static final int HEALTHUTIL_RESP_ARM = HEALTHUTIL_RESP_VISIT + 1;
	// 1 # ER visits
	public static final int HEALTHUTIL_RESP_ER_VISIT = HEALTHUTIL_RESP_ARM + 1;
	// 2b # hospital admissions
	public static final int HEALTHUTIL_RESP_HOSPITAL_ADIM = HEALTHUTIL_RESP_ER_VISIT + 1;
	// 2c # nights in Hospital
	public static final int HEALTHUTIL_RESP_NIGHTS_IN_HOSPITAL = HEALTHUTIL_RESP_HOSPITAL_ADIM + 1;
	// 3 # nights in nursing home
	public static final int HEALTHUTIL_RESP_NIGHTS_IN_NURSING = HEALTHUTIL_RESP_NIGHTS_IN_HOSPITAL + 1;
	// 4 # visits outpatient
	public static final int HEALTHUTIL_RESP_OUTPATIENT = HEALTHUTIL_RESP_NIGHTS_IN_NURSING + 1;
	// 5 # visits social worker
	public static final int HEALTHUTIL_RESP_VISIT_SOCIAL_WORKER = HEALTHUTIL_RESP_OUTPATIENT + 1;
	// 6 # visits home care
	public static final int HEALTHUTIL_RESP_VISIT_HOME_CARE_NURSE = HEALTHUTIL_RESP_VISIT_SOCIAL_WORKER + 1;
	// 7 # days family member care
	public static final int HEALTHUTIL_RESP_CARE_BY_FAMILY = HEALTHUTIL_RESP_VISIT_HOME_CARE_NURSE + 1;
	// 8 # days missed
	public static final int HEALTHUTIL_RESP_DAY_MISSED = HEALTHUTIL_RESP_CARE_BY_FAMILY + 1;

	public static final int HEALTHUTIL_RESP_LENGTH = HEALTHUTIL_RESP_DAY_MISSED + 1;

	// From 20240305 version
	public static final int COL_INDEX_HU_ID = 0;
	public static final int COL_INDEX_HU_SITE = 4;
	public static final int COL_INDEX_HU_VISIT = 61;
	public static final int COL_INDEX_HU_ARM = 1;
	public static final int COL_INDEX_HU_DATE = 12;
	public static final int COL_INDEX_HU_RESP_ER_VISIT = 14;
	public static final int COL_INDEX_HU_RESP_HOSPITAL_ADIM = 18;
	public static final int COL_INDEX_HU_RESP_NIGHT_IN_HOSPITAL = 19;
	public static final int COL_INDEX_HU_RESP_NIGHT_IN_NURSING = 27;
	public static final int COL_INDEX_HU_RESP_OUTPATIENT = 31;
	public static final int COL_INDEX_HU_RESP_SOCIAL_WORKER = 35;
	public static final int COL_INDEX_HU_RESP_HOME_CARE_NURSE = 39;
	public static final int COL_INDEX_HU_RESP_CARE_BY_FAMILY = 43;
	public static final int COL_INDEX_HU_RESP_DAY_MISSED = 47;

	public static final int SF_6D_MAP_STUDY_ARM = 0;
	public static final int SF_6D_MAP_WK_00 = SF_6D_MAP_STUDY_ARM + 1;
	public static final int SF_6D_MAP_WK_48 = SF_6D_MAP_WK_00 + 1;
	public static final int SF_6D_MAP_WK_96 = SF_6D_MAP_WK_48 + 1;
	public static final int SF_6D_MAP_LENGTH = SF_6D_MAP_WK_96 + 1;

	// Transient objects for look up
	@SuppressWarnings("unchecked")
	protected transient Collection<int[]>[][] sf12_resp_map = new Collection[STUDY_ARM.length][VISIT_NUM.length];
	protected transient HashMap<Integer, int[]> sf12_resp_index_by_pid = new HashMap<>();

	// protected transient Collection<int[]>[][] health_util_resp_map = new
	// Collection[STUDY_ARM.length][VISIT_NUM.length];
	protected transient HashMap<Integer, int[][]> health_util_resp_index_by_pid = new HashMap<>();
	
	
	protected DateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd");

	public XLSX_Extract_HEA() {
		sf12_lookup.put(COL_INDEX_SF_12_ID, SF12_RESP_ID);
		sf12_lookup.put(COL_INDEX_SF_12_SITE, SF12_RESP_SITE);
		sf12_lookup.put(COL_INDEX_SF_12_ARM, SF12_RESP_ARM);
		sf12_lookup.put(COL_INDEX_SF_12_VISIT, SF12_RESP_VISIT);
		sf12_lookup.put(COL_INDEX_SF_12_DATECOMPLETED, -1); // Not used

		for (int sf12_num = 0; sf12_num < D2EFT_QALY_SF12.SF12_LENGTH; sf12_num++) {
			sf12_lookup.put(COL_INDEX_SF_12_RESP_START + sf12_num * COL_SKIP_SF_12_RESP,
					SF12_RESP_SF12_START + sf12_num);
		}

	}

	public void loadWorkbook(File inpath) {
		extractWorkbook(inpath);
	}

	@Override
	protected void extractWorkbook(File inpath) {

		long tic = System.currentTimeMillis();

		super.extractWorkbook(inpath);

		XSSFSheet srcSheet_sf_12 = null;
		XSSFSheet srcSheet_HealthUtil = null;
		for (int i = 0; i < sheets.length; i++) {
			if (WORKSHEET_NAME_SF_12.equals(sheets[i].getSheetName())) {
				srcSheet_sf_12 = sheets[i];
			}
			if (WORKSHEET_NAME_HEALTH_UTIL.equals(sheets[i].getSheetName())) {
				srcSheet_HealthUtil = sheets[i];
			}
		}

		if (srcSheet_sf_12 != null) {

			int numRows = srcSheet_sf_12.getPhysicalNumberOfRows();

			resp_sf12 = new int[numRows][SF12_RESP_LENGTH]; // ID, SITE, STUDY_ARM, VISIT_NUM, SF12
			dateCompleted_sf12 = new Date[numRows]; // Date completed;

			for (int[] s : resp_sf12) {
				Arrays.fill(s, -1);
			}

			int rowNum = 0;
			int rPt = 0;
			int val;
			String[] toMatch;

			for (Row row : srcSheet_sf_12) {
				if (rowNum > 0) { // Skip the first row					
					for (Cell cell : row) {						
						int colNum = cell.getColumnIndex();
						try {
							switch (colNum) {
							case COL_INDEX_SF_12_ID: // ID
							case COL_INDEX_SF_12_SITE: // SITE
								resp_sf12[rPt][sf12_lookup.get(colNum)] = 
										cell.getCellType() == CellType.NUMERIC ?
												(int) cell.getNumericCellValue()
												: Integer.parseInt(cell.getStringCellValue());
								break;
							case COL_INDEX_SF_12_ARM: // ARM
							case COL_INDEX_SF_12_VISIT: // VISIT
								toMatch = colNum == COL_INDEX_SF_12_ARM ? STUDY_ARM : VISIT_NUM;
								val = matchString(cell.getStringCellValue(), toMatch);
								if (val < 0) {
									throw new IllegalStateException();
								} else {
									resp_sf12[rPt][sf12_lookup.get(colNum)] = val;
								}
								break;
							case COL_INDEX_SF_12_DATECOMPLETED: // Date completed
								dateCompleted_sf12[rPt] = cell.getDateCellValue();
								break;
							default:																
								// Check if it is SF_12 response column 
								if(sf12_lookup.containsKey(colNum)) {									
									int sf_12_Q_num = sf12_lookup.get(colNum);																										
									resp_sf12[rPt][sf_12_Q_num] = cell.getCellType() == CellType.NUMERIC ?
											(int) cell.getNumericCellValue()
											: Integer.parseInt(cell.getStringCellValue());
								}																							
							}
						} catch (IllegalStateException | NumberFormatException ex) {
							String output = cell.getCellType() == CellType.NUMERIC
									? Double.toString(cell.getNumericCellValue())
									: cell.getStringCellValue();

							printOutput(String.format("Error in formatting cell at (%d, %s) in %s : [%s]\n", rowNum + 1,
									getColumnName(colNum + 1), srcSheet_sf_12.getSheetName(), output));

						}						
					}
					rPt++;
				}
				rowNum++;
			}
			printOutput(String.format("Extraction of %s completed. Time required = %.3f s\n",
					srcSheet_sf_12.getSheetName(), (System.currentTimeMillis() - tic) / 1000f));

			// Mapping for quicker lookup
			Collection<int[]> sf12_resp;

			for (int r = 0; r < resp_sf12.length; r++) {

				int[] resp = resp_sf12[r];
				if (resp[SF12_RESP_ARM] != -1 && resp[SF12_RESP_VISIT] != -1) {
					sf12_resp = sf12_resp_map[resp[SF12_RESP_ARM]][resp[SF12_RESP_VISIT]];
					if (sf12_resp == null) {
						sf12_resp_map[resp[SF12_RESP_ARM]][resp[SF12_RESP_VISIT]] = new ArrayList<>();
						sf12_resp = sf12_resp_map[resp[SF12_RESP_ARM]][resp[SF12_RESP_VISIT]];
					}
					sf12_resp.add(Arrays.copyOfRange(resp, SF12_RESP_SF12_START, resp.length));

					Integer pid = resp[SF12_RESP_ID];
					int[] index = sf12_resp_index_by_pid.get(pid);
					if (index == null) {
						index = new int[VISIT_NUM.length];
						Arrays.fill(index, -1);
						sf12_resp_index_by_pid.put(pid, index);
					}
					index[resp[SF12_RESP_VISIT]] = r;
				}

			}
		}

		if (srcSheet_HealthUtil != null) {
			int rowNum = 0;

			int numRows = srcSheet_HealthUtil.getPhysicalNumberOfRows();
			int rPt = 0;
			resp_healthUtil = new int[numRows][HEALTHUTIL_RESP_LENGTH];
			dateCompleted_healthUtil = new Date[numRows];
			int val;

			
			for (Row row : srcSheet_HealthUtil) {
				if (rowNum > 0) { // Skip the first row
					
					
					Loop_ReadingRow:
					for (Cell cell : row) {
						try {
							int colNum = cell.getColumnIndex();
							switch (colNum) {							
							// 1: Subject Number
							case COL_INDEX_HU_ID:
								resp_healthUtil[rPt][HEALTHUTIL_RESP_ID] = convertNumberCell(cell);
								break;
							// 2: Site Id
							case COL_INDEX_HU_SITE:
								resp_healthUtil[rPt][HEALTHUTIL_RESP_SITE] = convertNumberCell(cell);
								break;
							// 3: Visit Mnemonic
							case COL_INDEX_HU_VISIT:
								val = matchString(cell.getStringCellValue(), VISIT_NUM);
								if (val < 0) {
									throw new IllegalStateException();
								} else {
									resp_healthUtil[rPt][HEALTHUTIL_RESP_VISIT] = val;
								}
								break;
							// 4: RANDARM
							case COL_INDEX_HU_ARM:
								val = matchString(cell.getStringCellValue(), STUDY_ARM);
								if (val < 0) {
									throw new IllegalStateException();
								} else {
									resp_healthUtil[rPt][HEALTHUTIL_RESP_ARM] = val;
								}
								break;
							// 5: Date
							case COL_INDEX_HU_DATE:						
								// Check date
								dateCompleted_healthUtil[rPt] = cell.getCellType() == CellType.NUMERIC ?
										cell.getDateCellValue() :
											defaultDateFormat.parse(cell.getStringCellValue());
										
								break;
							// 6: 1. How many VISITS did the participant make to an emergency
							// room/department?
							// 7: Number of visits
							case COL_INDEX_HU_RESP_ER_VISIT:
								resp_healthUtil[rPt][HEALTHUTIL_RESP_ER_VISIT] = convertNumberCell(cell);
								break;
							// 8: 2a. Has the participant been admitted to hospital OVERNIGHT? If Yes,
							// please complete 2b and 2c
							// 9: Yes ~ HLTHNight2a
							// 10: 2b How many ADMISSIONS did the participant have?
							case COL_INDEX_HU_RESP_HOSPITAL_ADIM:
								resp_healthUtil[rPt][HEALTHUTIL_RESP_HOSPITAL_ADIM] = convertNumberCell(cell);
								break;
							// 11: itmNights2cNONE ~ HLTHNight2a
							// 12: 2c How many NIGHTS in total did the participant spend in hospital?
							case COL_INDEX_HU_RESP_NIGHT_IN_HOSPITAL:
								resp_healthUtil[rPt][HEALTHUTIL_RESP_NIGHTS_IN_HOSPITAL] = convertNumberCell(cell);
								break;
							// 13: 3. How many NIGHTS did the participant stay at a nursing home?
							// 14: Number of nights ~ HEALTHNIGHTS
							case COL_INDEX_HU_RESP_NIGHT_IN_NURSING:
								resp_healthUtil[rPt][HEALTHUTIL_RESP_NIGHTS_IN_NURSING] = convertNumberCell(cell);
								break;
							// 15: itmHLTHOutpatient0 ~ HLTHOUTPATIENT
							// 16: Number of visits ~ HLTHOUTPATIENT
							case COL_INDEX_HU_RESP_OUTPATIENT:
								resp_healthUtil[rPt][HEALTHUTIL_RESP_OUTPATIENT] = convertNumberCell(cell);
								break;
							// 17: itmHLTHSW0 ~ HLTHSW0
							// 18: Number of times ~ HLTHSW0
							case COL_INDEX_HU_RESP_SOCIAL_WORKER:
								resp_healthUtil[rPt][HEALTHUTIL_RESP_VISIT_SOCIAL_WORKER] = convertNumberCell(cell);
								break;
							// 19: itmHLTHNurse0 ~ HEALTHNURSE
							// 20: Number of times ~ HEALTHNURSE
							case COL_INDEX_HU_RESP_HOME_CARE_NURSE:
								resp_healthUtil[rPt][HEALTHUTIL_RESP_VISIT_HOME_CARE_NURSE] = convertNumberCell(cell);
								break;
							// 21: itmHLTHFam0 ~ HEALTHFAMFRI
							// 22: Number of days ~ HEALTHFAMFRI
							case COL_INDEX_HU_RESP_CARE_BY_FAMILY:
								resp_healthUtil[rPt][HEALTHUTIL_RESP_CARE_BY_FAMILY] = convertNumberCell(cell);
								break;
							// 23: itmHLTHActiv0 ~ HEALTHMISSAC
							// 24: Number of days ~ HEALTHMISSAC
							case COL_INDEX_HU_RESP_DAY_MISSED:
								resp_healthUtil[rPt][HEALTHUTIL_RESP_DAY_MISSED] = convertNumberCell(cell);
								break;
							default:
								// Do nothing
							}

						} catch (IllegalStateException | NumberFormatException | ParseException ex) {
							String output = cell.getCellType() == CellType.NUMERIC
									? Double.toString(cell.getNumericCellValue())
									: cell.toString();

							printOutput(String.format("Error in formatting cell at (%d, %s) in %s : [%s]\n", rowNum + 1,
									getColumnName(cell.getColumnIndex() + 1), srcSheet_HealthUtil.getSheetName(), output));
							
							
							// Skip the entire row
							Arrays.fill(resp_healthUtil[rPt], -1);
							break Loop_ReadingRow;
						}
						
					}
					rPt++;
				}
				rowNum++;
			}

			// Mapping for quicker lookup
			Collection<int[]> healthutil_resp;

			for (int r = 0; r < resp_healthUtil.length; r++) {
				int[] resp = resp_healthUtil[r];
				if (resp[HEALTHUTIL_RESP_ARM] != -1 && resp[HEALTHUTIL_RESP_VISIT] != -1) {
					//healthutil_resp = health_util_resp_map[resp[HEALTHUTIL_RESP_ARM]][resp[HEALTHUTIL_RESP_VISIT]];
					//if (healthutil_resp == null) {
					//	health_util_resp_map[resp[HEALTHUTIL_RESP_ARM]][resp[HEALTHUTIL_RESP_VISIT]] = new ArrayList<>();
					//	healthutil_resp = health_util_resp_map[resp[HEALTHUTIL_RESP_ARM]][resp[HEALTHUTIL_RESP_VISIT]];
					//}
					//healthutil_resp.add(Arrays.copyOfRange(resp, HEALTHUTIL_RESP_ER_VISIT, resp.length));
					
					Integer pid = resp[HEALTHUTIL_RESP_ID];
					
					int[][] healthutil_resp_arr =  health_util_resp_index_by_pid.get(pid);
					
					if(healthutil_resp_arr == null) {
						healthutil_resp_arr = new int[VISIT_NUM.length][];
						health_util_resp_index_by_pid.put(pid, healthutil_resp_arr);						
					}
					healthutil_resp_arr[resp[HEALTHUTIL_RESP_VISIT]]= Arrays.copyOfRange(resp, HEALTHUTIL_RESP_ER_VISIT, resp.length);
				
				}
			}

		}

	}

	protected int convertNumberCell(Cell cell) throws NumberFormatException {
		if (cell.getCellType() == CellType.STRING) {
			if ("N/A".equals(cell.getStringCellValue())) {
				return -1;
			} else {
				return Integer.parseInt(cell.getStringCellValue());

			}
		} else {
			return (int) cell.getNumericCellValue();
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

	public Collection<int[]> sf12_response_lookup(int study_arm, int visit) {
		return sf12_resp_map[study_arm][visit];
	}

	public int[] response_lookup_by_row(int row) {
		return resp_sf12[row];
	}

	public HashMap<Integer, int[]> getSF_12_Resp_index_by_pid() {
		return sf12_resp_index_by_pid;
	}

	public HashMap<Integer, int[][]> getHealth_util_resp_index_by_pid() {
		return health_util_resp_index_by_pid;
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

			XLSX_Extract_HEA wk_extract = new XLSX_Extract_HEA();
			wk_extract.extractWorkbook(xlsx_file);

			Collection<int[]> sf12_resp;
			for (int studyArm = 0; studyArm < STUDY_ARM.length; studyArm++) {
				for (int visitNum = 0; visitNum < VISIT_NUM.length; visitNum++) {
					sf12_resp = wk_extract.sf12_response_lookup(studyArm, visitNum);
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

	
	public HashMap<Integer, float[]> generate_SF6D_Mapping() {

		HashMap<Integer, int[]> resp_by_id = getSF_12_Resp_index_by_pid();
		HashMap<Integer, float[]> sf_6d_dataset = new HashMap<>(); // Id => study_arm, wk_0, wk_48, wk_96
		

		for (Integer pid : resp_by_id.keySet()) {

			float[] ent = new float[SF_6D_MAP_LENGTH];
			Arrays.fill(ent, Float.NaN);

			int[] rowNum = resp_by_id.get(pid);

			if (rowNum[0] == -1) {
				printOutput(String.format("Day 0 visit data missing for PID: %d\n", pid));
			} else {
				int[] resp_wk00 = response_lookup_by_row(rowNum[0]);

				ent[SF_6D_MAP_STUDY_ARM] = resp_wk00[SF12_RESP_ARM];

				try {
					float[] summary_wk00 = D2EFT_QALY_SF12.calulateSummaryScale(
							Arrays.copyOfRange(resp_wk00, SF12_RESP_SF12_START, resp_wk00.length), scaleOffset_SF6D);

					ent[SF_6D_MAP_WK_00] = summary_wk00[D2EFT_QALY_SF12.SF_6D_CONSISTENT];

				} catch (IllegalArgumentException ex) {
					System.out.println(String.format(
							"Error in calculating Day 0 QALY at Row #%d. Results set at NaN instead.", rowNum[0] + 2));
				}

				int[] resp_wk48 = null;
				int[] resp_wk96 = null;

				if (rowNum[1] > 0) {
					resp_wk48 = response_lookup_by_row(rowNum[1]);
					if (resp_wk48[SF12_RESP_ARM] != ent[SF_6D_MAP_STUDY_ARM]) {
						printOutput(String.format(
								"PID %d: Wk 48 visit study arm mismatch." + " Use Day 0 study arm (%s) instead.\n", pid,
								STUDY_ARM[(int) ent[SF_6D_MAP_STUDY_ARM]]));
					}

					try {

						float[] summary_wk48 = D2EFT_QALY_SF12.calulateSummaryScale(
								Arrays.copyOfRange(resp_wk48, SF12_RESP_SF12_START, resp_wk48.length), scaleOffset_SF6D);
						if (summary_wk48 != null) {
							ent[SF_6D_MAP_WK_48] = summary_wk48[D2EFT_QALY_SF12.SF_6D_CONSISTENT];
						}
					} catch (IllegalArgumentException ex) {
						System.out.println(
								String.format("Error in calculating Wk 48 QALY at Row #%d. Results set at NaN instead.",
										rowNum[1] + 2));
					}

				}
				if (rowNum[2] > 0) {
					resp_wk96 = response_lookup_by_row(rowNum[2]);
					if (resp_wk96[SF12_RESP_ARM] != ent[SF_6D_MAP_STUDY_ARM]) {
						printOutput(String.format(
								"PID %d: Wk 96 visit study arm mismatch for" + " Use Day 0 study arm (%s) instead.\n",
								pid, STUDY_ARM[(int) ent[SF_6D_MAP_STUDY_ARM]]));
					}
					if (resp_wk48 == null) {
						printOutput(String.format("PID %d: Wk 48 visit missing despite having Wk 96 resp.\n", pid));
					}

					try {

						float[] summary_wk96 = D2EFT_QALY_SF12.calulateSummaryScale(
								Arrays.copyOfRange(resp_wk96, SF12_RESP_SF12_START, resp_wk96.length), scaleOffset_SF6D);

						if (summary_wk96 != null) {
							ent[SF_6D_MAP_WK_96] = summary_wk96[D2EFT_QALY_SF12.SF_6D_CONSISTENT];
						}
					} catch (IllegalArgumentException ex) {
						System.out.println(
								String.format("Error in calculating Wk 96 QALY at Row #%d. Results set at NaN instead.",
										rowNum[2] + 2));
					}
				}
				sf_6d_dataset.put(pid, ent);
			}
		}

		return sf_6d_dataset;
	}

	public static JFreeChart[] generate_Summary_Chart(XLSX_Extract_HEA wk_extract) {
		HashMap<Integer, int[]> resp_by_id = wk_extract.getSF_12_Resp_index_by_pid();

		@SuppressWarnings("unchecked")
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
				int studyArm = resp_wk00[SF12_RESP_ARM];

				pid_study_arm_map.put(pid, studyArm);

				float[] summary_wk00 = D2EFT_QALY_SF12
						.calulateSummaryScale(Arrays.copyOfRange(resp_wk00, SF12_RESP_SF12_START, resp_wk00.length), scaleOffset_SF6D);

				float[] sf6d_by_pid = sf_6d_dataset.get(pid);

				if (sf6d_by_pid == null) {
					sf6d_by_pid = new float[VISIT_NUM.length];
					Arrays.fill(sf6d_by_pid, -1f);
					sf_6d_dataset.put(pid, sf6d_by_pid);
				}
				sf6d_by_pid[0] = summary_wk00[D2EFT_QALY_SF12.SF_6D_CONSISTENT];

				int[] resp_wk48 = null;
				int[] resp_wk96 = null;

				if (rowNum[1] > 0) {
					resp_wk48 = wk_extract.response_lookup_by_row(rowNum[1]);
					if (resp_wk48[SF12_RESP_ARM] != studyArm) {
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
					if (resp_wk96[SF12_RESP_ARM] != studyArm) {
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

		@SuppressWarnings("unchecked")
		ArrayList<Float>[] sf_6d_wk96_wk48 = new ArrayList[STUDY_ARM.length];

		for (int i = 0; i < sf_6d_wk96_wk48.length; i++) {
			sf_6d_wk96_wk48[i] = new ArrayList<Float>();
		}

		for (Integer pid : sf_6d_dataset.keySet()) {
			float[] sf6d_by_pid = sf_6d_dataset.get(pid);
			int studyArm = pid_study_arm_map.get(pid);
			if (sf6d_by_pid[2] != -1 && sf6d_by_pid[0] != -1) {
				sf_6d_wk96_wk48[studyArm].add(sf6d_by_pid[2] - sf6d_by_pid[1]);
			}
		}

		for (int studyArm = 0; studyArm < STUDY_ARM.length; studyArm++) {
			sf12_diff_dataset[D2EFT_QALY_SF12.SF_6D_CONSISTENT].add(sf_6d_wk96_wk48[studyArm], STUDY_ARM[studyArm],
					String.format("%s - %s", VISIT_NUM[2], VISIT_NUM[1]));
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

		for (int s = SF12_RESP_SF12_START; s < resp_wk_diff.length && !hasNA; s++) {
			if (resp_wk_diff[s] < 0) {
				hasNA = true;
			}
		}

		if (!hasNA) {
			float[] summary_diff = D2EFT_QALY_SF12.calulateSummaryScale(
					Arrays.copyOfRange(resp_wk_diff, SF12_RESP_SF12_START, resp_wk_diff.length), scaleOffset_SF6D);
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

	public static JFreeChart[] generate_Day00_Chart(XLSX_Extract_HEA wk_extract) {
		Collection<int[]> sf12_resp;
		int[][] sf12_gh1_Freq = new int[STUDY_ARM.length][D2EFT_QALY_SF12.SF12_Options[0].length];
		Number[][][] summary_val = new Number[STUDY_ARM.length][D2EFT_QALY_SF12.SUMMARY_SCALE_LENGTH][];

		for (int studyArm = 0; studyArm < STUDY_ARM.length; studyArm++) {
			sf12_resp = wk_extract.sf12_response_lookup(studyArm, 0);
			int pt = 0;
			summary_val[studyArm][D2EFT_QALY_SF12.PCS_12] = new Number[sf12_resp.size()];
			summary_val[studyArm][D2EFT_QALY_SF12.MCS_12] = new Number[sf12_resp.size()];

			for (int[] resp : sf12_resp) {
				sf12_gh1_Freq[studyArm][resp[D2EFT_QALY_SF12.SF12_GH1]]++;
				float[] summary = D2EFT_QALY_SF12.calulateSummaryScale(resp, scaleOffset_SF6D);
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
						((double) sf12_gh1_Freq[studyArm][c]) / wk_extract.sf12_response_lookup(studyArm, 0).size(),
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
