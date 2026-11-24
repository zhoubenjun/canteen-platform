package com.canteen.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

/** Excel 导入导出工具 (xlsx)，支持内嵌图片 */
public class ExcelUtil {

    /** 导出：headers 列名, data 为每行的值列表, images 与 data 行一一对应(可为 null 表示该行无图) */
    public static byte[] export(String[] headers, List<List<Object>> data) throws IOException {
        return exportWithImages(headers, data, null);
    }

    /** 导出并内嵌图片(图片列=最后一列) */
    public static byte[] exportWithImages(String[] headers, List<List<Object>> data, List<byte[]> images) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("菜品数据");
            CellStyle headStyle = wb.createCellStyle();
            headStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headFont = wb.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = head.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }
            int picCol = headers.length - 1;
            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            int r = 1;
            for (int i = 0; i < data.size(); i++) {
                List<Object> row = data.get(i);
                Row rr = sheet.createRow(r);
                rr.setHeightInPoints(52);
                for (int c = 0; c < row.size(); c++) {
                    Object v = row.get(c);
                    Cell cell = rr.createCell(c);
                    if (v == null) cell.setCellValue("");
                    else if (v instanceof BigDecimal || v instanceof Double || v instanceof Float) {
                        cell.setCellValue(((Number) v).doubleValue());
                    } else if (v instanceof Integer || v instanceof Long) {
                        cell.setCellValue(((Number) v).intValue());
                    } else cell.setCellValue(String.valueOf(v));
                }
                byte[] img = images != null ? images.get(i) : null;
                if (img != null && img.length > 0) {
                    int picIdx = wb.addPicture(img, guessType(img));
                    XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, picCol, r, picCol + 1, r + 1);
                    drawing.createPicture(anchor, picIdx);
                }
                r++;
            }
            sheet.setColumnWidth(picCol, 22 * 256);
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    /** 多工作表导出：一个 SheetSpec 对应一张 sheet，便于把明细(如配料)分页导出 */
    public static byte[] exportMulti(List<SheetSpec> sheets) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CellStyle headStyle = wb.createCellStyle();
            headStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headFont = wb.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            for (SheetSpec spec : sheets) {
                Sheet sheet = wb.createSheet(spec.name);
                Row head = sheet.createRow(0);
                for (int i = 0; i < spec.headers.length; i++) {
                    Cell c = head.createCell(i);
                    c.setCellValue(spec.headers[i]);
                    c.setCellStyle(headStyle);
                    sheet.setColumnWidth(i, 16 * 256);
                }
                int r = 1;
                for (List<Object> row : spec.rows) {
                    Row rr = sheet.createRow(r++);
                    for (int c = 0; c < row.size(); c++) {
                        Object v = row.get(c);
                        Cell cell = rr.createCell(c);
                        if (v == null) cell.setCellValue("");
                        else if (v instanceof BigDecimal || v instanceof Double || v instanceof Float) {
                            cell.setCellValue(((Number) v).doubleValue());
                        } else if (v instanceof Integer || v instanceof Long) {
                            cell.setCellValue(((Number) v).intValue());
                        } else cell.setCellValue(String.valueOf(v));
                    }
                }
                // 列宽按内容自适应（中文按 2 个字符计），上限 60 字符
                for (int c = 0; c < spec.headers.length; c++) {
                    int maxLen = displayWidth(spec.headers[c]);
                    for (List<Object> row : spec.rows) {
                        if (c < row.size()) maxLen = Math.max(maxLen, displayWidth(row.get(c)));
                    }
                    sheet.setColumnWidth(c, Math.min(60, Math.max(10, maxLen + 2)) * 256);
                }
                sheet.createFreezePane(0, 1);
            }
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    /** 显示宽度：中文/全角按 2，其余按 1 */
    private static int displayWidth(Object v) {
        if (v == null) return 0;
        String s = String.valueOf(v);
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            w += (ch > 0x2E80) ? 2 : 1;
        }
        return w;
    }

    /** 一张工作表的规格：表名 + 表头 + 数据行 */
    public static class SheetSpec {
        public String name;
        public String[] headers;
        public List<List<Object>> rows;

        public SheetSpec(String name, String[] headers, List<List<Object>> rows) {
            this.name = name;
            this.headers = headers;
            this.rows = rows;
        }
    }

    /** 读取 xlsx 第一张表：返回每行 map(列头->值) */
    public static List<Map<String, String>> read(byte[] bytes) throws IOException {
        return read(bytes, null);
    }

    /** 读取指定表名的工作表（表名不存在时用第一张），供导入时跳过配料页 */
    public static List<Map<String, String>> read(byte[] bytes, String preferredSheetName) throws IOException {
        List<RowData> rows = readWithImages(bytes, preferredSheetName);
        List<Map<String, String>> result = new ArrayList<>();
        for (RowData rd : rows) result.add(rd.fields);
        return result;
    }

    /** 读取 xlsx 第一张表(含内嵌图片)：每行 = 字段 + 该行图片字节(可为 null) */
    public static List<RowData> readWithImages(byte[] bytes) throws IOException {
        return readWithImages(bytes, null);
    }

    /** 读取指定表名的工作表；表名不存在时退回第一张表 */
    public static List<RowData> readWithImages(byte[] bytes, String preferredSheetName) throws IOException {
        List<RowData> result = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            if (preferredSheetName != null) {
                Sheet s = wb.getSheet(preferredSheetName);
                if (s != null) sheet = s;
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return result;
            String[] headers = new String[headerRow.getLastCellNum()];
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell c = headerRow.getCell(i);
                headers[i] = c == null ? "" : c.getStringCellValue().trim();
            }
            // 提取内嵌图片, 按锚定行(row1, 数据行从1开始)分组
            Map<Integer, PictureData> picByRow = new HashMap<>();
            if (sheet instanceof XSSFSheet) {
                XSSFDrawing drawing = ((XSSFSheet) sheet).getDrawingPatriarch();
                if (drawing != null) {
                    for (XSSFShape shape : drawing.getShapes()) {
                        if (shape instanceof XSSFPicture) {
                            XSSFPicture pic = (XSSFPicture) shape;
                            XSSFAnchor anchor = pic.getAnchor();
                            if (anchor instanceof XSSFClientAnchor) {
                                int row1 = ((XSSFClientAnchor) anchor).getRow1();
                                if (row1 >= 1) picByRow.putIfAbsent(row1, pic.getPictureData());
                            }
                        }
                    }
                }
            }
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> map = new LinkedHashMap<>();
                boolean empty = true;
                for (int i = 0; i < headers.length; i++) {
                    Cell c = row.getCell(i);
                    String v = "";
                    if (c != null) {
                        switch (c.getCellType()) {
                            case STRING: v = c.getStringCellValue().trim(); break;
                            case NUMERIC: v = String.valueOf(c.getNumericCellValue()); break;
                            case BOOLEAN: v = String.valueOf(c.getBooleanCellValue()); break;
                            default: v = "";
                        }
                    }
                    map.put(headers[i], v);
                    if (!v.isEmpty()) empty = false;
                }
                RowData rd = new RowData();
                rd.fields = map;
                PictureData pd = picByRow.get(r);
                if (pd != null) { rd.image = pd.getData(); rd.imageExt = pd.suggestFileExtension(); }
                if (!empty || rd.image != null) result.add(rd);
            }
        }
        return result;
    }

    /** 判断图片类型(JPEG/PNG) */
    private static int guessType(byte[] b) {
        if (b != null && b.length >= 8 && (b[0] & 0xff) == 0x89 && b[1] == 'P') return Workbook.PICTURE_TYPE_PNG;
        return Workbook.PICTURE_TYPE_JPEG;
    }

    /** 单行数据：字段 + 可选内嵌图片 */
    public static class RowData {
        public Map<String, String> fields;
        public byte[] image;
        public String imageExt;
    }

    /** 安全转数字 */
    public static BigDecimal toDecimal(String s, BigDecimal def) {
        if (s == null || s.trim().isEmpty()) return def;
        try {
            String t = s.trim().replaceAll("[^0-9.\\-]", "");
            if (t.isEmpty()) return def;
            return new BigDecimal(t);
        } catch (Exception e) { return def; }
    }
    public static Integer toInt(String s, Integer def) {
        if (s == null || s.trim().isEmpty()) return def;
        try { return (int) Math.round(Double.parseDouble(s.trim())); } catch (Exception e) { return def; }
    }
}
