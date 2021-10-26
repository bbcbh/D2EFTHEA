package util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;

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
		return sf_12_resp;
	}

	public static void main(String[] args) {
		File xlsx_file = new File("/home/ben/Downloads/SF-12 data.xlsx");

		XLSX_Extract_SF_12 wk_extract = new XLSX_Extract_SF_12();
		wk_extract.extractWorkbook(xlsx_file);

		for (int studyArm = 0; studyArm < STUDY_ARM.length; studyArm++) {
			for (int visitNum = 0; visitNum < VISIT_NUM.length; visitNum++) {
				Collection<int[]> sf_12_resp = wk_extract.response_lookup(studyArm, visitNum);
				System.out.println(String.format("# results for (%s, %s) = %d", STUDY_ARM[studyArm], VISIT_NUM[visitNum],
						sf_12_resp.size()));
			}

		}

	}

}
