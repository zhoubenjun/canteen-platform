package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.service.SysSettingService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Resource private SysSettingService service;

    @GetMapping("/all")
    public ApiResult<Map<String, String>> all() {
        return ApiResult.ok(service.getAll());
    }

    @PutMapping("/all")
    public ApiResult<Void> update(@RequestBody Map<String, String> map) {
        service.updateAll(map);
        return ApiResult.ok();
    }
}
