package com.canteen.util;

import com.canteen.entity.Companion;
import com.canteen.entity.Consumption;
import org.apache.poi.ss.usermodel.*;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

/** 文件解析工具：陪餐 xlsx、消费 csv */
public class ParseUtil {

    // ---------------- 陪餐 xlsx ----------------

    public static List<Companion> readCompanion(byte[] bytes) throws Exception {
        List<Companion> list = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            // 在前 5 行内定位表头行（含“陪餐人员”与“餐次”）
            int headerRowIdx = -1;
            Map<Integer, String> headers = new LinkedHashMap<>();
            for (int r = 0; r <= Math.min(5, sheet.getLastRowNum()); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<Integer, String> h = new LinkedHashMap<>();
                boolean hasPerson = false, hasMeal = false;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String v = cellString(row.getCell(c));
                    h.put(c, v);
                    if ("陪餐人员".equals(v)) hasPerson = true;
                    if ("餐次".equals(v)) hasMeal = true;
                }
                if (hasPerson && hasMeal) {
                    headerRowIdx = r;
                    headers = h;
                    break;
                }
            }
            if (headerRowIdx < 0) {
                throw new IllegalArgumentException("未找到陪餐数据表头（需包含“陪餐人员”“餐次”列）");
            }

            for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> m = new HashMap<>();
                for (Map.Entry<Integer, String> e : headers.entrySet()) {
                    if (e.getValue() == null || e.getValue().isEmpty()) continue;
                    m.put(e.getValue(), cellString(row.getCell(e.getKey())));
                }
                String name = trim(m.get("陪餐人员"));
                String meal = trim(m.get("餐次"));
                if (name.isEmpty() && meal.isEmpty()) continue;

                Companion c = new Companion();
                c.setSchoolName(m.get("学校名称"));
                c.setCompanionDate(normalizeDate(firstNonEmpty(m.get("陪餐日期"), m.get("日期"))));
                c.setWeek(m.get("星期"));
                // 中餐 改为 午餐
                c.setMealType(normalizeMeal(meal));
                c.setDishName(m.get("菜品名称"));
                c.setCompanionName(name);
                c.setCompanionIdentity(m.get("陪餐身份"));
                c.setRating(m.get("评价等级"));
                c.setComment(m.get("陪餐评价"));
                c.setCreatedTime(m.get("创建时间"));
                list.add(c);
            }
        }
        return list;
    }

    // ---------------- 消费 csv ----------------

    public static List<Consumption> readConsumption(byte[] bytes) throws Exception {
        List<Consumption> list = new ArrayList<>();
        Charset charset = detectCharset(bytes);
        String text = new String(bytes, charset);
        // 去掉 BOM
        if (text.startsWith("﻿")) text = text.substring(1);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(text.getBytes(charset)), charset));
        String line;
        String[] headers = null;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            String[] cols = parseCsvLine(line);
            if (headers == null) {
                headers = cols;
                continue;
            }
            Map<String, String> m = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                m.put(trim(headers[i]), i < cols.length ? trim(cols[i]) : "");
            }
            // 仅保留登记类型为“教职工”
            if (!"教职工".equals(m.get("登记类型"))) continue;

            Consumption c = new Consumption();
            c.setRegisterType(cleanNull(m.get("登记类型")));
            c.setRegisterName(cleanNull(m.get("登记姓名")));
            c.setClassName(cleanNull(m.get("班级名称")));
            c.setAmount(toDecimal(m.get("消费金额(元)")));
            c.setPayStatus(cleanNull(m.get("支付状态")));
            c.setConsumeDate(normalizeDate(m.get("消费时间")));
            c.setBusinessType(normalizeBusiness(m.get("消费业务类型")));
            c.setOrderNo(cleanCode(m.get("消费订单号")));
            c.setDish(cleanNull(m.get("消费菜品")));
            list.add(c);
        }
        return list;
    }

    // ---------------- 工具方法 ----------------

    private static String normalizeMeal(String meal) {
        String t = trim(meal);
        if ("中餐".equals(t)) return "午餐";
        return t;
    }

    private static String normalizeBusiness(String type) {
        String t = trim(type);
        if ("其它".equals(t)) return "其他";
        return t;
    }

    /** 字面量 NULL/null 视为空 */
    private static String cleanNull(String s) {
        String t = trim(s);
        if ("null".equalsIgnoreCase(t)) return "";
        return t;
    }

    /** 订单号：去掉前导撇号，NULL 视为空 */
    private static String cleanCode(String s) {
        String t = trim(s);
        if (t.startsWith("'")) t = t.substring(1);
        return cleanNull(t);
    }

    /** 日期归一化为 yyyy-MM-dd；无法解析时返回原值去空格 */
    public static String normalizeDate(String raw) {
        String s = trim(raw);
        if (s.isEmpty()) return "";
        // 去掉时间部分
        String datePart = s.split("\\s+")[0];
        datePart = datePart.replace('/', '-').replace('.', '-');
        String[] parts = datePart.split("-");
        try {
            if (parts.length == 3) {
                int y = Integer.parseInt(parts[0]);
                int mo = Integer.parseInt(parts[1]);
                int d = Integer.parseInt(parts[2]);
                return String.format("%04d-%02d-%02d", y, mo, d);
            }
        } catch (Exception ignore) {
        }
        return s;
    }

    private static BigDecimal toDecimal(String s) {
        String t = trim(s).replaceAll("[^0-9.\\-]", "");
        if (t.isEmpty()) return null;
        try {
            return new BigDecimal(t);
        } catch (Exception e) {
            return null;
        }
    }

    private static String cellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return trim(cell.getStringCellValue());
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                }
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return trim(cell.getStringCellValue());
                } catch (Exception e) {
                    return "";
                }
            default:
                return "";
        }
    }

    /** 极简 CSV 行解析，支持双引号包裹与转义引号 */
    private static String[] parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(ch);
                }
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private static Charset detectCharset(byte[] bytes) {
        // 先尝试 UTF-8，失败回退 GBK
        try {
            String utf8 = new String(bytes, Charset.forName("UTF-8"));
            if (!utf8.contains("�")) return Charset.forName("UTF-8");
        } catch (Exception ignore) {
        }
        return Charset.forName("GBK");
    }

    private static String firstNonEmpty(String... vals) {
        for (String v : vals) if (v != null && !v.trim().isEmpty()) return v.trim();
        return "";
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
