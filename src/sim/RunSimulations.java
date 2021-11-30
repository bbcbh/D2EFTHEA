package sim;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import sim.runnable.GenerateSingleQALYComparsion;
import util.XLSX_Extract_SF_12;

public class RunSimulations {

	private XLSX_Extract_SF_12 wk_extract = null;
	private float[][] qaly_mapping; //[]{study_arm, wk_0, wk_48, wk_96}	
	private int[] mapping_study_arm_offset = new int[XLSX_Extract_SF_12.STUDY_ARM.length];
	private float[] day_0_qalys;

	public void loadWorkbook(File workbookFile) {
		wk_extract = new XLSX_Extract_SF_12();
		wk_extract.loadWorkbook(workbookFile);
		
		HashMap<Integer, float[]> sf_6d_mapping = wk_extract.generate_SF6D_Mapping();
		
		qaly_mapping = new float[sf_6d_mapping.size()][XLSX_Extract_SF_12.SF_6D_MAP_LENGTH];	
		day_0_qalys = new float[sf_6d_mapping.size()];
		
		int pt_total = 0;
		int[] pt_diff = new int[XLSX_Extract_SF_12.STUDY_ARM.length];
		
		for (Integer pid : sf_6d_mapping.keySet()) {
			qaly_mapping[pt_total] = sf_6d_mapping.get(pid);	
			day_0_qalys[pt_total] = qaly_mapping[pt_total][XLSX_Extract_SF_12.SF_6D_MAP_WK_00];
			pt_diff[(int) qaly_mapping[pt_total][XLSX_Extract_SF_12.SF_6D_MAP_STUDY_ARM]] ++;
			pt_total++;
		}	
		
		Arrays.sort(day_0_qalys); 
		
		// Sort by study arm, then first day QALY
		Arrays.sort(qaly_mapping, new Comparator<float[]>() {
			@Override
			public int compare(float[] o1, float[] o2) {	
				int r = Integer.compare((int) o1[XLSX_Extract_SF_12.SF_6D_MAP_STUDY_ARM], 
						(int) o2[XLSX_Extract_SF_12.SF_6D_MAP_STUDY_ARM]);
				
				if(r == 0) {
					r = Float.compare(o1[XLSX_Extract_SF_12.SF_6D_MAP_WK_00], 
							o2[XLSX_Extract_SF_12.SF_6D_MAP_WK_00]);
				}
				if(r == 0) {
					r = Float.compare(o1[XLSX_Extract_SF_12.SF_6D_MAP_WK_48], 
							o2[XLSX_Extract_SF_12.SF_6D_MAP_WK_48]);
				}
				if(r == 0) {
					r = Float.compare(o1[XLSX_Extract_SF_12.SF_6D_MAP_WK_96], 
							o2[XLSX_Extract_SF_12.SF_6D_MAP_WK_96]);
				}
				
				
				return r;
			}
		});
		
		for (int a = 1; a < mapping_study_arm_offset.length; a++) {
			mapping_study_arm_offset[a] = mapping_study_arm_offset[a - 1] + pt_diff[a - 1];
		}
		
		
		// Debug
		
		System.out.println("Study arm offset = " + Arrays.toString(mapping_study_arm_offset));
		for(int i = 0; i < qaly_mapping.length; i++) {
			System.out.println(String.format("%4d: %s", i, Arrays.toString(qaly_mapping[i])));
		}		
		
		GenerateSingleQALYComparsion cmp = new GenerateSingleQALYComparsion(22519121217l, 
				qaly_mapping, mapping_study_arm_offset, day_0_qalys);
		
		cmp.run();
		
		
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
