package com.canteen.service;

import com.canteen.dto.PageResult;
import com.canteen.entity.Companion;
import com.canteen.entity.PaymentAnalysis;
import com.canteen.mapper.AnalysisMapper;
import com.canteen.mapper.CompanionMapper;
import com.canteen.mapper.ConsumptionMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnalysisService {

    @Resource
    private CompanionMapper companionMapper;
    @Resource
    private ConsumptionMapper consumptionMapper;
    @Resource
    private AnalysisMapper analysisMapper;

    public PageResult<PaymentAnalysis> page(String name, String start, String end, String mealType,
                                            String paid, int page, int size) {
        int offset = Math.max(0, (page - 1) * size);
        List<PaymentAnalysis> rows = analysisMapper.page(name, start, end, mealType, paid, offset, size);
        long total = analysisMapper.count(name, start, end, mealType, paid);
        return new PageResult<>(total, rows);
    }

    /** 按当前查询条件导出支付分析结果为 xlsx */
    public byte[] exportExcel(String name, String start, String end, String mealType, String paid) throws Exception {
        List<PaymentAnalysis> list = analysisMapper.listFiltered(name, start, end, mealType, paid);
        String[] headers = {"序号", "陪餐人员", "陪餐日期", "餐次", "是否支付"};

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("陪餐支付分析");

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

            int r = 1;
            for (PaymentAnalysis a : list) {
                Row row = sheet.createRow(r);
                row.createCell(0).setCellValue(r);
                row.createCell(1).setCellValue(nv(a.getCompanionName()));
                row.createCell(2).setCellValue(nv(a.getCompanionDate()));
                row.createCell(3).setCellValue(nv(a.getMealType()));
                row.createCell(4).setCellValue(nv(a.getPaid()));
                r++;
            }

            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private String nv(String s) {
        return s == null ? "" : s;
    }

    /**
     * 支付比对分析（按月份）：先删除该月已有分析结果，再以该月陪餐的
     * 陪餐人员 + 陪餐日期 + 餐次为基准，与消费数据的
     * 登记姓名 + 消费日期 + 消费业务类型 一一对应匹配。
     * 存在匹配 => 已支付，否则 => 未支付。
     */
    public Map<String, Object> runAnalysis(String month) {
        List<Companion> companions = companionMapper.listAll();
        Set<String> consumeKeys = new HashSet<>(consumptionMapper.listMatchKeys());

        List<PaymentAnalysis> result = new ArrayList<>();
        int paidCount = 0, unpaidCount = 0;
        for (Companion c : companions) {
            String date = c.getCompanionDate() == null ? "" : c.getCompanionDate().trim();
            if (!inMonth(date, month)) continue;
            String name = c.getCompanionName() == null ? "" : c.getCompanionName().trim();
            String meal = c.getMealType() == null ? "" : c.getMealType().trim();
            String key = name + "|" + date + "|" + meal;
            boolean paid = consumeKeys.contains(key);

            PaymentAnalysis a = new PaymentAnalysis();
            a.setCompanionName(name);
            a.setCompanionDate(date);
            a.setMealType(meal);
            a.setPaid(paid ? "已支付" : "未支付");
            result.add(a);
            if (paid) paidCount++; else unpaidCount++;
        }

        analysisMapper.deleteByMonth(month);
        int chunk = 500;
        for (int i = 0; i < result.size(); i += chunk) {
            analysisMapper.batchInsert(new ArrayList<>(result.subList(i, Math.min(i + chunk, result.size()))));
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("month", month);
        stats.put("total", result.size());
        stats.put("paid", paidCount);
        stats.put("unpaid", unpaidCount);
        return stats;
    }

    private boolean inMonth(String date, String month) {
        return date != null && date.startsWith(month + "-");
    }
}
