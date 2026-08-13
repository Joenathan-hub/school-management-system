package com.school.sms.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Generic Excel (.xlsx) export used for payments, expenditures, workers,
 * and students lists. Pass headers + rows, get a saved .xlsx file back.
 */
public class ExcelExportService {

    public void export(String sheetTitle, String[] headers, List<String[]> rows, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetTitle);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (String[] rowData : rows) {
                Row row = sheet.createRow(rowNum++);
                for (int col = 0; col < rowData.length; col++) {
                    row.createCell(col).setCellValue(rowData[col]);
                }
            }

            for (int col = 0; col < headers.length; col++) {
                sheet.autoSizeColumn(col);
            }

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                workbook.write(out);
            }
        }
    }
}
