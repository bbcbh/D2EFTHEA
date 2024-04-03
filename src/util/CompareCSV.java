package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class CompareCSV {	
	
	File baseCSV;
	File[] compareCSVs;
	
	public CompareCSV(File baseCSV, File[] compareCSVs) {		
		this.baseCSV = baseCSV;
		this.compareCSVs = compareCSVs;		
	}	
	
	private static String convertFilename(String filename) {
		return (filename.split("\\.")[0]).replaceAll("\\s", "_");
	}
	
	
	
	public void compareEntry() throws IOException {
		File baseDir = baseCSV.getParentFile();
		PrintWriter[] pWri_compare = new PrintWriter[compareCSVs.length];
		
		BufferedReader reader_base;
		BufferedReader[] reader_compare = new BufferedReader[compareCSVs.length];
		
		String baseline;
		String firstline = null;
		int lineNum =0;
		
		for(int f = 0; f < compareCSVs.length; f++) {			
			pWri_compare[f] = new PrintWriter(baseDir, 
					String.format("Diff_%s_%s.csv",
							convertFilename(compareCSVs[f].getName()), 
							convertFilename(baseCSV.getName())));					
			reader_compare[f] = new BufferedReader(new FileReader(compareCSVs[f]));					
		}
		
		reader_base = new BufferedReader(new FileReader(baseCSV));
		
		while((baseline = reader_base.readLine()) != null){
			if(firstline == null) {
				firstline = baseline;				
				for(int f = 0; f < compareCSVs.length; f++) {	
					reader_compare[f].readLine();
					pWri_compare[f].println(baseline);
				}						
			}else {
				String[] baseEnts = baseline.split(",");
				
				for(int f = 0; f < compareCSVs.length; f++) {
					String[] compEnt =  (reader_compare[f].readLine()).split(",");
					if(baseEnts[0].equals(compEnt[0])) {
						pWri_compare[f].print(baseEnts[0]);
						for(int i = 1; i < baseEnts.length; i++) {
							pWri_compare[f].print(',');
							
							float diff = Float.parseFloat(compEnt[i]) - Float.parseFloat(baseEnts[i]);							
							pWri_compare[f].print(Float.toString(diff));
						}
						pWri_compare[f].println();
						
					}else {
						System.err.printf("Error! Line %d mismatch between %s and %s\n", 
								lineNum, baseCSV.getName() , compareCSVs[f].getName());
					}
				
				
				}
			}
			
			
			
			
			lineNum++;
		}
	
		
		
		
		
		
		
		reader_base.close();
		for(int f = 0; f < compareCSVs.length; f++) {
			pWri_compare[f].close();
			reader_compare[f].close();
		}
		
		
		
		
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
