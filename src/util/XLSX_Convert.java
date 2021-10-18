package util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Convert XLSX (from Yuvi) to a format for my own database
 *
 * @author Ben Hui
 */
public class XLSX_Convert {

    File inpath;
    File outpath;

    XSSFSheet[] sheets;
    XSSFWorkbook srcWorkbook;

    Calendar cal_local = Calendar.getInstance();
    SimpleDateFormat[] dateFormatCollection = new SimpleDateFormat[]{
        new SimpleDateFormat("MMM dd, yyyy")
    };

    public XLSX_Convert(File inpath, File outpath) {
        this.inpath = inpath;
        this.outpath = outpath;

        if (outpath == null) {
            this.outpath = inpath.getParentFile();
        }

    }

    public void convert() {
        extractWorkbook();

        cal_local.setTimeInMillis(System.currentTimeMillis());

        CellStyle cs = srcWorkbook.createCellStyle();
        CreationHelper createHelper = srcWorkbook.getCreationHelper();
        cs.setDataFormat(createHelper.createDataFormat().getFormat("d-MMM-yyyy"));

        for (int s = 0; s < sheets.length; s++) {
            if (s == 0) {
                XSSFSheet srcSheet = sheets[s];
                int r = 0;
                for (Row row : srcSheet) {
                    if (r > 0) {
                        int c = 0;
                        for (Cell cell : row) {
                            try {
                                switch (c) {
                                    //0: Subject Initials
                                    //1: Subject Number
                                    case 1:
                                        convertNumberCell(cell);
                                        break;
                                    //2: Site Mnemonic	
                                    //3: Visit Mnemonic
                                    case 3:
                                        if (!"Day 0".equals(cell.getStringCellValue())
                                                && !"Wk48".equals(cell.getStringCellValue())
                                                && !"Wk96".equals(cell.getStringCellValue())) {
                                            throw new IllegalStateException(
                                                    String.format(" '%s' not valid option for Col %s",
                                                            cell.getStringCellValue(), getColumnName(c + 1)));
                                        }
                                        break;
                                    //4: RANDARM	
                                    case 4:
                                        if (!"SOC".equals(cell.getStringCellValue())
                                                && !"DOL".equals(cell.getStringCellValue())
                                                && !"D2N".equals(cell.getStringCellValue())) {
                                            throw new IllegalStateException(
                                                    String.format(" '%s' not valid option for Col %s",
                                                            cell.getStringCellValue(), getColumnName(c + 1)));
                                        }
                                        break;
                                    //5: Date	
                                    case 5:
                                        // Check date
                                        cell.getDateCellValue();
                                        break;
                                    //6: 1. How many VISITS did the participant make to an emergency room/department?
                                    //7: Number of visits	
                                    case 7:
                                        convertNumberCell(cell);
                                        checkRefVal(cell, row, 6);
                                        break;
                                    //8: 2a. Has the participant been admitted to hospital OVERNIGHT? If Yes, please complete 2b and 2c	
                                    //9: Yes ~ HLTHNight2a	
                                    //10: 2b How many ADMISSIONS did the participant have?
                                    case 10:
                                        convertNumberCell(cell);
                                        checkRefVal(cell, row, 8, 9);
                                        break;
                                    //11: itmNights2cNONE ~ HLTHNight2a
                                    //12: 2c How many NIGHTS in total did the participant spend in hospital?	
                                    case 12:
                                        convertNumberCell(cell);
                                        checkRefVal(cell, row, 8, 11);
                                        break;

                                    //13: 3. How many NIGHTS did the participant stay at a nursing home?	
                                    //14: Number of nights ~ HEALTHNIGHTS	
                                    case 14:
                                        convertNumberCell(cell);
                                        checkRefVal(cell, row, 13);
                                        break;
                                    //15: itmHLTHOutpatient0 ~ HLTHOUTPATIENT	
                                    //16: Number of visits ~ HLTHOUTPATIENT	
                                    case 16:
                                        convertNumberCell(cell);
                                        checkRefVal(cell, row, 15);
                                        break;
                                    //17: itmHLTHSW0 ~ HLTHSW0	
                                    //18: Number of times ~ HLTHSW0	
                                    case 18:
                                        convertNumberCell(cell);
                                        checkRefVal(cell, row, 17);
                                        break;
                                    //19: itmHLTHNurse0 ~ HEALTHNURSE	
                                    //20: Number of times ~ HEALTHNURSE	
                                    case 20:
                                        convertNumberCell(cell);
                                        checkRefVal(cell, row, 19);
                                        break;
                                    //21: itmHLTHFam0 ~ HEALTHFAMFRI	
                                    //22: Number of days ~ HEALTHFAMFRI	
                                    case 22:
                                        convertNumberCell(cell);
                                        checkRefVal(cell, row, 21);
                                        break;

                                    //23: itmHLTHActiv0 ~ HEALTHMISSAC	
                                    //24: Number of days ~ HEALTHMISSAC                                        
                                    case 24:
                                        convertNumberCell(cell);
                                        checkRefVal(cell, row, 23);
                                        break;

                                    default:
                                        cell.setCellValue(cell.getStringCellValue().trim());

                                }
                            } catch (IllegalStateException | NullPointerException | NumberFormatException ex) {

                                String output = cell.getCellType() == CellType.NUMERIC
                                        ? Double.toString(cell.getNumericCellValue())
                                        : cell.getStringCellValue();

                                printOutput(String.format("Error in formatting cell at row %d, col %s: [%s]\n",
                                        r + 1, getColumnName(c + 1), output));

                                if (ex.getMessage() != null) {
                                    printOutput(ex.getMessage() + "\n");
                                }

                            }

                            c++;
                        }
                    }
                    r++;
                }

            }

        }

        // Write new file
        StringBuilder fileName = new StringBuilder();
        fileName.append("Reformatted_");
        fileName.append(cal_local.get(Calendar.YEAR));
        fileName.append(String.format("%02d", cal_local.get(Calendar.MONTH) + 1));
        fileName.append(String.format("%02d", cal_local.get(Calendar.DAY_OF_MONTH)));
        fileName.append("_");
        fileName.append(inpath.getName());
        File res_file = new File(outpath, fileName.toString());

        try (OutputStream fileOut = new FileOutputStream(res_file)) {
            srcWorkbook.write(fileOut);
            printOutput("Refomatted result generated at " + res_file.getAbsolutePath() + "\n");
        } catch (IOException ex) {
            StringWriter err = new StringWriter();
            ex.printStackTrace(new PrintWriter(err));
            printOutput(err.toString() + "\n");

        }

    }

    protected void checkRefVal(Cell cell, Row row, int ref) throws IllegalStateException {
        boolean err;

        err = cell.getNumericCellValue() > 0
                ? "<small>None</small>".equals(row.getCell(ref).getStringCellValue())
                : !"<small>None</small>".equals(row.getCell(ref).getStringCellValue());
        if (err) {
            throw new IllegalStateException(
                    String.format(" Cell value = %.1f or N/A, yet entry at Col %s is '%s'",
                            cell.getNumericCellValue(), getColumnName(ref + 1),
                            row.getCell(ref).getStringCellValue()));

        }
    }

    protected void checkRefVal(Cell cell, Row row, int refNo, int refNA) throws IllegalStateException {
        boolean err;

        err = cell.getNumericCellValue() > 0
                ? ("No".equals(row.getCell(refNo).getStringCellValue()) || "N/A".equals(row.getCell(refNA).getStringCellValue()))
                : (!"No".equals(row.getCell(refNo).getStringCellValue()) || !"N/A".equals(row.getCell(refNA).getStringCellValue()));

        if (err) {
            throw new IllegalStateException(
                    String.format(" Cell value = %.1f or N/A, yet entry at Col %s is '%s' and Col %s is '%s'",
                            cell.getNumericCellValue(), getColumnName(refNo + 1),
                            row.getCell(refNo).getStringCellValue(),
                            getColumnName(refNA + 1), row.getCell(refNA).getStringCellValue()));
        }

    }

    protected void convertNumberCell(Cell cell) throws NumberFormatException {
        if (cell.getCellType() == CellType.STRING) {
            if ("N/A".equals(cell.getStringCellValue())) {
                cell.setCellValue(0);
            } else {
                cell.setCellValue(Double.parseDouble(cell.getStringCellValue()));

            }
        }
    }

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

    protected void extractWorkbook() {
        printOutput("Decoding " + inpath.getAbsolutePath() + "...");
        srcWorkbook = null;
        try {
            OPCPackage pkg = OPCPackage.open(inpath);
            srcWorkbook = new XSSFWorkbook(pkg);
            printOutput("success!\n");
        } catch (InvalidFormatException | IOException ex) {
            printOutput("FAILED!\n");
            StringWriter err = new StringWriter();
            ex.printStackTrace(new PrintWriter(err));
            printOutput(err.toString() + "\n");
        }

        if (srcWorkbook != null) {
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

    public static void main(String[] arg) {
        File inPath = new File("C:\\Users\\bhui\\OneDrive - UNSW\\D2EFT\\Data\\Health Eco data for Ben.xlsx");
        File outPath = new File("C:\\Users\\bhui\\Desktop\\Reformatted");
        XLSX_Convert coverter = new XLSX_Convert(inPath, outPath);
        coverter.convert();
    }

}
