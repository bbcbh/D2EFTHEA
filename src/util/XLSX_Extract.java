package util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/**
 * A general purpose utility class to extract information from XLSX file.   
 * @author Ben Hui
 *
 */

public class XLSX_Extract {	
	
	public static String getColumnName(int number) {
	    final StringBuilder sb = new StringBuilder();
	
	    int num = number - 1;
	    while (num >= 0) {
	        int numChar = (num % 26) + 65;
	        sb.append((char) numChar);
	        num = (num / 26) - 1;
	    }
	    return sb.reverse().toString();
	}

	protected XSSFSheet[] sheets;
	protected XSSFWorkbook srcWorkbook;

	public XLSX_Extract() {
		super();
	}

	protected void extractWorkbook(File inpath) {
	    printOutput("Decoding " + inpath.getAbsolutePath() + "...");
	    srcWorkbook = null;
	    try {
	        OPCPackage pkg = OPCPackage.open(inpath);
	        srcWorkbook = new XSSFWorkbook(pkg);
	        printOutput("success!\n");	        
	        pkg.close();
	    } catch (InvalidFormatException | IOException ex) {
	        printOutput("FAILED!\n");
	        StringWriter err = new StringWriter();
	        ex.printStackTrace(new PrintWriter(err));
	        printOutput(err.toString() + "\n");
	    }
	
	    if (srcWorkbook != null) {
	    	printOutput("Worksheet list:\n");
	        sheets = new XSSFSheet[srcWorkbook.getNumberOfSheets()];
	        for (int i = 0; i < sheets.length; i++) {
	            sheets[i] = srcWorkbook.getSheetAt(i);
	            printOutput(String.format("Sheet #%d: %s\n", i, sheets[i].getSheetName()));
	        }
	
	    }
	    
	    
	}

	protected void printOutput(String line) {
	    System.out.print(line);
	}

	public XSSFSheet[] getSheets() {
		return sheets;
	}

	public XSSFWorkbook getSrcWorkbook() {
		return srcWorkbook;
	}



}