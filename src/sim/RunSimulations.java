package sim;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import util.D2EFT_QALY_SF12;
import util.XLSX_Extract_SF_12;

public class RunSimulations {

	private XLSX_Extract_SF_12 wk_extract = null;
	private Float[][] day_zero_qaly; // [Study arm]{...}

	public void loadWorkbook(File workbookFile) {
		wk_extract = new XLSX_Extract_SF_12();
		wk_extract.loadWorkbook(workbookFile);

		day_zero_qaly = new Float[XLSX_Extract_SF_12.STUDY_ARM.length][];

		ArrayList<Float>[] qaly_tally = new ArrayList[day_zero_qaly.length];

		for (int s = 0; s < qaly_tally.length; s++) {
			qaly_tally[s] = new ArrayList<Float>();

		}

		HashMap<Integer, int[]> resp_by_id = wk_extract.getResp_index_by_pid();

		for (Integer pid : resp_by_id.keySet()) {
			int[] rowNum = resp_by_id.get(pid);
			int[] resp_wk00 = wk_extract.response_lookup_by_row(rowNum[0]);
			int study_arm = resp_wk00[XLSX_Extract_SF_12.RESP_ARM];
			float[] summary_wk00 = D2EFT_QALY_SF12.calulateSummaryScale(
					Arrays.copyOfRange(resp_wk00, XLSX_Extract_SF_12.RESP_SF12_START, resp_wk00.length), 1);
			qaly_tally[study_arm].add(summary_wk00[D2EFT_QALY_SF12.SF_6D_CONSISTENT]);

		}

		for (int s = 0; s < qaly_tally.length; s++) {
			day_zero_qaly[s] = qaly_tally[s].toArray(new Float[qaly_tally[s].size()]);
			
			
			
		}

	}

	public static void main(String[] args) {
		File xlsx_file = null;

		if (args.length > 0) {
			xlsx_file = new File(args[0]);
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
			RunSimulations sim = new RunSimulations();
			sim.loadWorkbook(xlsx_file);

		}
	}

}
