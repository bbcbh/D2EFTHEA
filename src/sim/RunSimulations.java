package sim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.random.MersenneTwister;

import person.D2EFT_HEA_Person;
import sim.runnable.Runnable_SingleQALYComparsion;
import util.XLSX_Extract_HEA;

public class RunSimulations {

	// SF-12 and QALY
	private XLSX_Extract_HEA wk_extract = null;
	private float[][] qaly_mapping; // []{study_arm, wk_0, wk_48, wk_96}
	private int[] mapping_study_arm_offset = new int[XLSX_Extract_HEA.STUDY_ARM.length];
	private float[] day_0_qalys;

	// Health utilisation
	// healthUtil_QALY_LIST[study_arm][visit_arm][qaly_index]
	private float[][][] healthUtil_QALY_LIST = new float[XLSX_Extract_HEA.STUDY_ARM.length][XLSX_Extract_HEA.VISIT_NUM.length][];
	@SuppressWarnings("unchecked")
	// healthUtil_USAGE_LIST[study_arm][visit_arm][qaly_index][response]
	private ArrayList<Float>[][][][] healthUtil_USAGE_LIST = new ArrayList[XLSX_Extract_HEA.STUDY_ARM.length][XLSX_Extract_HEA.VISIT_NUM.length][][];

	// Lookup by PID
	private HashMap<Integer, float[]> sf_6d_mapping_by_pid; // PID -> {study_arm, qaly_wk_0, qaly_wk_48, qaly_wk_96}
	HashMap<Integer, int[][]> health_util_resp_index_by_pid; // PID -> [VISIT_NUM]{HEALTHUTIL_RESP_ER_VISIT,
																// HEALTHUTIL_RESP_HOSPITAL_ADIM ... }

	// Other parameters
	public static final int NUM_HEALTHUTIL_RESP = XLSX_Extract_HEA.HEALTHUTIL_RESP_LENGTH
			- XLSX_Extract_HEA.HEALTHUTIL_RESP_ER_VISIT;

	@SuppressWarnings("unchecked")
	public void loadWorkbook(File workbookFile) {

		// SF_12

		wk_extract = new XLSX_Extract_HEA();
		wk_extract.loadWorkbook(workbookFile);
		sf_6d_mapping_by_pid = wk_extract.generate_SF6D_Mapping();
		health_util_resp_index_by_pid = wk_extract.getHealth_util_resp_index_by_pid();

		qaly_mapping = new float[sf_6d_mapping_by_pid.size()][XLSX_Extract_HEA.SF_6D_MAP_LENGTH];
		day_0_qalys = new float[sf_6d_mapping_by_pid.size()];

		int pt_total = 0;
		int[] pt_diff = new int[XLSX_Extract_HEA.STUDY_ARM.length];

		// Health Util
		HashMap<Float, ArrayList<Integer>>[][] healthUtil_QALY_PID_Map = new HashMap[XLSX_Extract_HEA.STUDY_ARM.length][XLSX_Extract_HEA.VISIT_NUM.length]; // [study_arm][visit_num]{QALY->{pids}}

		for (Integer pid : sf_6d_mapping_by_pid.keySet()) {
			qaly_mapping[pt_total] = sf_6d_mapping_by_pid.get(pid);
			day_0_qalys[pt_total] = qaly_mapping[pt_total][XLSX_Extract_HEA.SF_6D_MAP_WK_00];
			pt_diff[(int) qaly_mapping[pt_total][XLSX_Extract_HEA.SF_6D_MAP_STUDY_ARM]]++;

			int studyArm = (int) qaly_mapping[pt_total][XLSX_Extract_HEA.SF_6D_MAP_STUDY_ARM];

			for (int v = XLSX_Extract_HEA.SF_6D_MAP_WK_00; v <= XLSX_Extract_HEA.SF_6D_MAP_WK_96; v++) {
				HashMap<Float, ArrayList<Integer>> study_visit_map = healthUtil_QALY_PID_Map[studyArm][v
						- XLSX_Extract_HEA.SF_6D_MAP_WK_00];
				if (study_visit_map == null) {
					study_visit_map = new HashMap<>();
					healthUtil_QALY_PID_Map[studyArm][v - XLSX_Extract_HEA.SF_6D_MAP_WK_00] = study_visit_map;
				}
				ArrayList<Integer> qaly_ent = study_visit_map.get(qaly_mapping[pt_total][v]);
				if (qaly_ent == null) {
					qaly_ent = new ArrayList<>();
					study_visit_map.put(qaly_mapping[pt_total][v], qaly_ent);
				}
				qaly_ent.add(pid);
			}

			pt_total++;

		}

		for (int s = 0; s < XLSX_Extract_HEA.STUDY_ARM.length; s++) {
			for (int v = 0; v < XLSX_Extract_HEA.VISIT_NUM.length; v++) {
				HashMap<Float, ArrayList<Integer>> qaly_pid_map = healthUtil_QALY_PID_Map[v][s];
				healthUtil_QALY_LIST[s][v] = new float[qaly_pid_map.size()];
				int c = 0;
				for (Float f : qaly_pid_map.keySet()) {
					healthUtil_QALY_LIST[s][v][c] = f;
					c++;
				}
				Arrays.sort(healthUtil_QALY_LIST[s][v]);
				healthUtil_USAGE_LIST[s][v] = new ArrayList[healthUtil_QALY_LIST[s][v].length][RunSimulations.NUM_HEALTHUTIL_RESP];

				for (int q = 0; q < healthUtil_QALY_LIST[s][v].length; q++) {
					Float qaly = healthUtil_QALY_LIST[s][v][q];
					ArrayList<Float>[] healthUtil = healthUtil_USAGE_LIST[s][v][q];

					ArrayList<Integer> pids = qaly_pid_map.get(qaly);
					for (Integer pid : pids) {
						int[][] util_by_visit = health_util_resp_index_by_pid.get(pid);
						if (util_by_visit != null && util_by_visit[v] != null) {
							for (int u = 0; u < healthUtil.length; u++) {
								float util_usage = Math.max(0, (float) util_by_visit[v][u]);
								if (healthUtil[u] == null) {
									healthUtil[u] = new ArrayList<Float>();
								}
								healthUtil[u].add(util_usage);
							}
						}
					}

				}
			}
		}

		Arrays.sort(day_0_qalys);

		try {
			PrintWriter pWri = new PrintWriter(
					new FileWriter(new File(workbookFile.getParentFile(), "Day_00_QALY_RAW.csv")));
			pWri.println("Day 00 QALY (RAW)");
			for (float q : day_0_qalys) {
				pWri.println(q);
			}
			pWri.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}

		// Sort by study arm, then first day QALY
		Arrays.sort(qaly_mapping, new Comparator<float[]>() {
			@Override
			public int compare(float[] o1, float[] o2) {
				int r = Integer.compare((int) o1[XLSX_Extract_HEA.SF_6D_MAP_STUDY_ARM],
						(int) o2[XLSX_Extract_HEA.SF_6D_MAP_STUDY_ARM]);

				if (r == 0) {
					r = Float.compare(o1[XLSX_Extract_HEA.SF_6D_MAP_WK_00], o2[XLSX_Extract_HEA.SF_6D_MAP_WK_00]);
				}
				if (r == 0) {
					r = Float.compare(o1[XLSX_Extract_HEA.SF_6D_MAP_WK_48], o2[XLSX_Extract_HEA.SF_6D_MAP_WK_48]);
				}
				if (r == 0) {
					r = Float.compare(o1[XLSX_Extract_HEA.SF_6D_MAP_WK_96], o2[XLSX_Extract_HEA.SF_6D_MAP_WK_96]);
				}

				return r;
			}
		});

		for (int a = 1; a < mapping_study_arm_offset.length; a++) {
			mapping_study_arm_offset[a] = mapping_study_arm_offset[a - 1] + pt_diff[a - 1];
		}

	}

	public void runSimulations(int numSim, int numThreads, File outputDir) throws Exception {

		System.out.println(String.format("Generating %d outputs (%d at a time) at %s", numSim, numThreads,
				outputDir.getAbsolutePath()));

		long tic = System.currentTimeMillis();
		final long BASESEED = 2251912207291119l;
		MersenneTwister rng = new MersenneTwister(BASESEED);

		Runnable_SingleQALYComparsion[] runnable = new Runnable_SingleQALYComparsion[numSim];
		ExecutorService exec = null;

		boolean useThread = false; // numThreads > 1;
		int thread_count = 0;

		for (int r = 0; r < runnable.length; r++) {
			runnable[r] = new Runnable_SingleQALYComparsion(rng.nextLong(), qaly_mapping, mapping_study_arm_offset,
					day_0_qalys);

			runnable[r].setHealthUtilisationLookup(healthUtil_QALY_LIST, healthUtil_USAGE_LIST);

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

		final int[] WEEKS_TO_SAMPLE = { 0, 48 / 2, 48, (96 + 48) / 2, 96, };
		final int[] WEEKS_DATA = { 0, 48, 96 };
		final boolean genIndivCSV = runnable.length <= 8;

		Object[][] resultSet_All = new Object[runnable.length][];
		// float[][][] resultSet = new float[runnable.length][][];
		ArrayList<Future<Object[]>> futureRes = new ArrayList<>();

		for (int r = 0; r < runnable.length; r++) {

			final D2EFT_HEA_Person[] cmpPerson = (D2EFT_HEA_Person[]) runnable[r].getResult()
					.get(Runnable_SingleQALYComparsion.KEY_HEA_PERSON);
			final int rIndex = r;

			Callable<Object[]> r_print = new Callable<Object[]>() {

				@Override
				public Object[] call() {

					float[][] res_qaly = new float[cmpPerson.length][WEEKS_TO_SAMPLE.length];
					
					float[][][] res_heathUtil = new float[cmpPerson.length][WEEKS_TO_SAMPLE.length][RunSimulations.NUM_HEALTHUTIL_RESP];
					for (int t = 0; t < WEEKS_TO_SAMPLE.length; t++) {
						// res[t] = (float) Math.max(0,
						// Math.min(cmpPerson[s].getQALYByFit(WEEKS_TO_SAMPLE[t]* 7), 1));
						for (int a = 0; a < cmpPerson.length; a++) {
							int day = WEEKS_TO_SAMPLE[t] * 7;
							int dayPos = cmpPerson[a].getDayPos(day);
							res_qaly[a][t] = cmpPerson[a].getQALYByPos(day, dayPos);
							res_heathUtil[a][t] = cmpPerson[a].getHealthUtil(day, dayPos);
						}
					}

					// System.out.println(String.format("Simualtion #%d completed.", rIndex));

					if (genIndivCSV) {
						final String fileName = String.format("QALY_%d.csv", rIndex);
						final File outputFile = new File(outputDir, fileName);
						try {
							PrintWriter fWri = new PrintWriter(new FileWriter(outputFile));

							fWri.println("Input");
							fWri.println("Week,SOC,DOL,D2N,,HealthUtil");

							for (int t = 0; t < WEEKS_DATA.length; t++) {
								fWri.print(WEEKS_DATA[t]);
								int dayVal = WEEKS_DATA[t] * 7;

								for (int s = 0; s < cmpPerson.length; s++) {
									fWri.print(',');
									fWri.print(cmpPerson[s].getQALYByPos(dayVal));
								}

								for (int s = 0; s < cmpPerson.length; s++) {
									float[] util = cmpPerson[s].getHealthUtil(dayVal);
									fWri.print(',');
									for (float u : util) {
										fWri.print(',');
										fWri.print(u);
									}
								}
								fWri.println();
							}

							fWri.println("Simulations");
							fWri.println("Week,SOC,DOL,D2N,,HealthUtil");
							for (int t = 0; t < WEEKS_TO_SAMPLE.length; t++) {
								fWri.print(WEEKS_TO_SAMPLE[t]);
								for (int s = 0; s < cmpPerson.length; s++) {
									fWri.print(',');
									fWri.print(res_qaly[s][t]);
								}
								fWri.print(',');
								for (int s = 0; s < cmpPerson.length; s++) {
									fWri.print(',');
									float[] utili = res_heathUtil[s][t];
									for (int u = 0; u < utili.length; u++) {
										fWri.print(',');
										fWri.print(utili[u]);
									}
								}
								fWri.println();
							}
							fWri.close();

						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					return new Object[] { res_qaly, res_heathUtil };

				}

			};

			if (useThread) {
				futureRes.add(exec.submit(r_print));

			} else {
				resultSet_All[r] = r_print.call();
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

		for (int r = 0; r < resultSet_All.length; r++) {
			if (resultSet_All[r] == null && futureRes.get(r) != null) {
				resultSet_All[r] = futureRes.get(r).get();
			}
		}

		for (int arm = 0; arm < XLSX_Extract_HEA.STUDY_ARM.length; arm++) {

			float[][] outcomeByArm = new float[resultSet_All.length][WEEKS_TO_SAMPLE.length
					+ WEEKS_TO_SAMPLE.length * RunSimulations.NUM_HEALTHUTIL_RESP];

			for (int r = 0; r < resultSet_All.length; r++) {
				for (int t = 0; t < WEEKS_TO_SAMPLE.length; t++) {
					Object[] resSet = resultSet_All[r];
					float[][] qaly = (float[][]) resSet[0];
					float[][][] util_res = (float[][][]) resSet[1]; //[sim#][WeekToSampleIndex][respNum]
					outcomeByArm[r][t] = qaly[arm][t];
					int util_offset = WEEKS_TO_SAMPLE.length + t * RunSimulations.NUM_HEALTHUTIL_RESP;
					for (int utilNum = 0; utilNum < util_res[arm][t].length; utilNum++) {
						outcomeByArm[r][util_offset + utilNum] = util_res[arm][t][utilNum];
					}
				}

			}

			Arrays.sort(outcomeByArm, new Comparator<float[]>() {
				@Override
				public int compare(float[] o1, float[] o2) {
					int cmp = 0;

					for (int i = 0; i < o1.length; i++) {
						cmp = Float.compare(o1[i], o2[i]);
						if (cmp != 0) {
							return cmp;
						}
					}
					return cmp;
				}
			});

			File outputFileAll = new File(outputDir, String.format("QALY_%s.csv", XLSX_Extract_HEA.STUDY_ARM[arm]));

			PrintWriter fWriAll = new PrintWriter(new FileWriter(outputFileAll));

			StringBuffer firstLine = new StringBuffer("Sim");

			for (int wk_i = 0; wk_i < WEEKS_TO_SAMPLE.length; wk_i++) {
				firstLine.append(',');
				firstLine.append(String.format(" Wk %2d", WEEKS_TO_SAMPLE[wk_i]));
			}
			
			for (int wk_i = 0; wk_i < WEEKS_TO_SAMPLE.length; wk_i++) {
				for (int utilNum = 0; utilNum < RunSimulations.NUM_HEALTHUTIL_RESP; utilNum++) {
					firstLine.append(',');
					firstLine.append(String.format(" Wk %2d_Util_%d", 
							WEEKS_TO_SAMPLE[wk_i], utilNum));
				}
				
			}

			fWriAll.println(firstLine.toString());

			for (int r = 0; r < outcomeByArm.length; r++) {
				StringBuffer line = null;
				for (int t = 0; t < outcomeByArm[r].length; t++) {
					if (line == null) {
						line = new StringBuffer();
						line.append(r);

					}
					line.append(',');

					line.append(outcomeByArm[r][t]);
				}
				fWriAll.println(line.toString());

			}

			fWriAll.close();
		}

		System.out.println(String.format("%d outputs generated at <%s> in %.3f seconds", numSim,
				outputDir.getAbsolutePath(), (System.currentTimeMillis() - tic) / 1000f));

	}

	public static void main(String[] args) throws Exception {
		File xlsx_file = null;

		int numSim = Runtime.getRuntime().availableProcessors();
		int numThreads = Runtime.getRuntime().availableProcessors();

		if (args.length > 0) {
			xlsx_file = new File(args[0]);
			if (args.length > 2) {
				numSim = Integer.parseInt(args[1]);
				if (args.length > 3) {
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
