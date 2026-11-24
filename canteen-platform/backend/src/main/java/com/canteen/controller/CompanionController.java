package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.dto.PageResult;
import com.canteen.entity.Companion;
import com.canteen.service.CompanionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Map;

/** 陪餐分析管理 - 陪餐数据分析 */
@RestController
@RequestMapping("/api/pa/companion")
public class CompanionController {

    @Resource
    private CompanionService service;

    @GetMapping("/page")
    public ApiResult<PageResult<Companion>> page(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end,
            @RequestParam(value = "mealType", required = false) String mealType,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ApiResult.ok(service.page(name, start, end, mealType, page, size));
    }

    @PostMapping("/import")
    public ApiResult<Map<String, Object>> importFile(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("month") String month) throws Exception {
        return ApiResult.ok(service.importFile(file, month));
    }
}
