package sim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.random.MersenneTwister;

import person.D2EFT_HEA_Person;
import sim.runnable.Runnable_SingleQALYComparsion;
import util.XLSX_Extract_SF_12;

public class RunSimulations {

	private XLSX_Extract_SF_12 wk_extract = null;
	private float[][] qaly_mapping; // []{study_arm, wk_0, wk_48, wk_96}
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
			pt_diff[(int) qaly_mapping[pt_total][XLSX_Extract_SF_12.SF_6D_MAP_STUDY_ARM]]++;
			pt_total++;
		}

		Arrays.sort(day_0_qalys);

		// Sort by study arm, then first day QALY
		Arrays.sort(qaly_mapping, new Comparator<float[]>() {
			@Override
			public int compare(float[] o1, float[] o2) {
				int r = Integer.compare((int) o1[XLSX_Extract_SF_12.SF_6D_MAP_STUDY_ARM],
						(int) o2[XLSX_Extract_SF_12.SF_6D_MAP_STUDY_ARM]);

				if (r == 0) {
					r = Float.compare(o1[XLSX_Extract_SF_12.SF_6D_MAP_WK_00], o2[XLSX_Extract_SF_12.SF_6D_MAP_WK_00]);
				}
				if (r == 0) {
					r = Float.compare(o1[XLSX_Extract_SF_12.SF_6D_MAP_WK_48], o2[XLSX_Extract_SF_12.SF_6D_MAP_WK_48]);
				}
				if (r == 0) {
					r = Float.compare(o1[XLSX_Extract_SF_12.SF_6D_MAP_WK_96], o2[XLSX_Extract_SF_12.SF_6D_MAP_WK_96]);
				}

				return r;
			}
		});

		for (int a = 1; a < mapping_study_arm_offset.length; a++) {
			mapping_study_arm_offset[a] = mapping_study_arm_offset[a - 1] + pt_diff[a - 1];
		}

		// Debug

	}

	public void runSimulations(int numSim, int numThreads, File outputDir) {

		System.out.println(String.format("Generating %d outputs (%d at a time) at %s", numSim, numThreads,
				outputDir.getAbsolutePath()));

		long tic = System.currentTimeMillis();
		final long BASESEED = 2251912207291119l;
		MersenneTwister rng = new MersenneTwister(BASESEED);

		Runnable_SingleQALYComparsion[] runnable = new Runnable_SingleQALYComparsion[numSim];
		ExecutorService exec = null;

		boolean useThread = numThreads > 1;
		int thread_count = 0;

		for (int r = 0; r < runnable.length; r++) {
			runnable[r] = new Runnable_SingleQALYComparsion(rng.nextLong(), qaly_mapping, mapping_study_arm_offset,
					day_0_qalys);

			if (useThread) {
				if (exec == null) {
					exec = Executors.newFixedThreadPool(numThreads);
				}
				exec.submit(runnable[r]);
				thread_count++;
				if (thread_count == numThreads) {
					exec.shutdown();

					try {
						if (!exec.awaitTermination(60, TimeUnit.MINUTES)) {
							System.err.println(
									"Thread pool did not terminate. Using serial for rest of the result instead");
							useThread = false;
						}

						thread_count = 0;

					} catch (InterruptedException e) {
						e.printStackTrace();
						System.err
								.println("Thread pool did not terminate. Using serial for rest of the result instead");
						useThread = false;
					}
					exec = null;

				}

			} else {
				runnable[r].run();
			}
		}

		if (exec != null) {
			try {
				exec.shutdown();

				if (!exec.awaitTermination(60, TimeUnit.MINUTES)) {
					System.err.println("Thread pool did not terminate.");
					useThread = false;
				}

				thread_count = 0;
				exec = null;

			} catch (InterruptedException e) {
				e.printStackTrace();
				System.err.println("Thread pool did not terminate");
				useThread = false;
			}
		}

		System.out.println(String.format("%d simulations completed in %.3f seconds", numSim,
				(System.currentTimeMillis() - tic) / 1000f));

		// Printing output

		if (useThread) {
			exec = Executors.newFixedThreadPool(numThreads);
		}

		for (int r = 0; r < runnable.length; r++) {

			final D2EFT_HEA_Person[] cmpPerson = (D2EFT_HEA_Person[]) runnable[r].getResult()
					.get(Runnable_SingleQALYComparsion.KEY_HEA_PERSON);

			final File outputFile = new File(outputDir, String.format("QALY_%d.csv", r));

			Runnable r_print = new Runnable() {
				@Override
				public void run() {
					final int[] WEEKS_TO_SAMPLE = { 0, 48 / 2, 48, (96 + 48) / 2, 96, 52 * 5, 52 * 10 };
					final int[] WEEKS_DATA = { 0, 48, 96 };

					try {
						PrintWriter fWri = new PrintWriter(new FileWriter(outputFile));

						fWri.println("Input");
						fWri.println("Week,SOC,DOL,D2N");

						for (int t = 0; t < WEEKS_DATA.length; t++) {
							fWri.print(WEEKS_DATA[t]);
							for (int s = 0; s < cmpPerson.length; s++) {
								int dayPos = cmpPerson[s].getDayPos(WEEKS_DATA[t] * 7);
								fWri.print(',');
								fWri.print(cmpPerson[s].getQALYByPos(WEEKS_DATA[t] * 7, dayPos));

							}
							fWri.println();
						}

						fWri.println("Simulations");
						fWri.println("Week,SOC,DOL,D2N");
						for (int t = 0; t < WEEKS_TO_SAMPLE.length; t++) {

							fWri.print(WEEKS_TO_SAMPLE[t]);

							for (int s = 0; s < cmpPerson.length; s++) {
								fWri.print(',');
								fWri.print(Math.max(0, Math.min(cmpPerson[s].getQALYByFit(WEEKS_TO_SAMPLE[t] * 7), 1)));
							}
							fWri.println();
						}
						fWri.close();

					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			};

			if (useThread) {
				exec.submit(r_print);

			} else {
				r_print.run();
			}

		}

		if (useThread) {
			try {
				exec.shutdown();

				if (!exec.awaitTermination(60, TimeUnit.MINUTES)) {
					System.err.println("Thread pool did not terminate.");
					useThread = false;
				}
				exec = null;

			} catch (InterruptedException e) {
				e.printStackTrace();
				System.err.println("Thread pool did not terminate");
				useThread = false;
			}
		}

		System.out.println(String.format("%d outputs generated at <%s> in %.3f seconds", numSim,
				outputDir.getAbsolutePath(), (System.currentTimeMillis() - tic) / 1000f));

	}

	public static void main(String[] args) {
		File xlsx_file = null;
		int numSim = Runtime.getRuntime().availableProcessors();
		int numThreads = Runtime.getRuntime().availableProcessors();

		if (args.length > 0) {
			xlsx_file = new File(args[0]);
			if(args.length > 1) {
				numSim = Integer.parseInt(args[1]);
				if(args.length > 2) {
					numThreads = Integer.parseInt(args[2]);
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
			RunSimulations sim = new RunSimulations();
			sim.loadWorkbook(xlsx_file);
			sim.runSimulations(numSim, numThreads, xlsx_file.getParentFile());

		}
	}

}
