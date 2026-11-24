package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.dto.PageResult;
import com.canteen.entity.PaymentAnalysis;
import com.canteen.service.AnalysisService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** 陪餐分析管理 - 陪餐支付分析 */
@RestController
@RequestMapping("/api/pa/analysis")
public class AnalysisController {

    @Resource
    private AnalysisService service;

    @GetMapping("/page")
    public ApiResult<PageResult<PaymentAnalysis>> page(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end,
            @RequestParam(value = "mealType", required = false) String mealType,
            @RequestParam(value = "paid", required = false) String paid,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ApiResult.ok(service.page(name, start, end, mealType, paid, page, size));
    }

    /** 执行支付比对分析（按月份） */
    @PostMapping("/run")
    public ApiResult<Map<String, Object>> run(@RequestParam("month") String month) {
        return ApiResult.ok(service.runAnalysis(month));
    }

    /** 按当前查询条件导出支付分析结果 xlsx */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end,
            @RequestParam(value = "mealType", required = false) String mealType,
            @RequestParam(value = "paid", required = false) String paid) throws Exception {
        byte[] b = service.exportExcel(name, start, end, mealType, paid);
        String fn = URLEncoder.encode("陪餐支付分析.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fn)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(b);
    }
}
