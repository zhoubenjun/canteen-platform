package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.entity.PurchasePlan;
import com.canteen.service.PurchaseService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseController {

    @Resource private PurchaseService service;

    @GetMapping("/summary")
    public ApiResult<Map<String, Object>> summary(@RequestParam String weekKey,
                                                  @RequestParam(required = false) List<Integer> days,
                                                  @RequestParam(required = false) String mealType) {
        return ApiResult.ok(service.summary(weekKey, days, mealType));
    }

    @PostMapping("/savePlan")
    public ApiResult<Map<String, Object>> savePlan(@RequestParam String weekKey,
                                                   @RequestParam(required = false) List<Integer> days,
                                                   @RequestParam(required = false) String mealType) {
        String planKey = service.savePlan(weekKey, days, mealType);
        return ApiResult.ok(Map.of("planKey", planKey));
    }

    @GetMapping("/plans")
    public ApiResult<List<String>> plans() {
        return ApiResult.ok(service.planKeys());
    }

    @GetMapping("/plan")
    public ApiResult<List<PurchasePlan>> plan(@RequestParam String planKey) {
        return ApiResult.ok(service.listPlan(planKey));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String planKey) throws Exception {
        List<PurchasePlan> plans = service.listPlan(planKey);
        byte[] b = service.exportPlan(plans);
        String fn = URLEncoder.encode("采购计划-" + planKey + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fn)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(b);
    }
}
