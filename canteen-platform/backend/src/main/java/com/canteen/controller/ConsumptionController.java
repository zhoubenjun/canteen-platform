package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.dto.PageResult;
import com.canteen.entity.Consumption;
import com.canteen.service.ConsumptionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Map;

/** 陪餐分析管理 - 消费数据分析 */
@RestController
@RequestMapping("/api/pa/consumption")
public class ConsumptionController {

    @Resource
    private ConsumptionService service;

    @GetMapping("/page")
    public ApiResult<PageResult<Consumption>> page(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "registerType", required = false) String registerType,
            @RequestParam(value = "businessType", required = false) String businessType,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ApiResult.ok(service.page(name, registerType, businessType, start, end, page, size));
    }

    @PostMapping("/import")
    public ApiResult<Map<String, Object>> importFile(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("month") String month) throws Exception {
        return ApiResult.ok(service.importFile(file, month));
    }
}
