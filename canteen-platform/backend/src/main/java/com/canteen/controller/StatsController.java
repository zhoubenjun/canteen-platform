package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.mapper.StatsMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/** 数据统计：食材统计 / 菜品统计（按实际用餐日期区间） */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Resource
    private StatsMapper statsMapper;

    /** 食材统计：startDate/endDate 格式 yyyy-MM-dd */
    @GetMapping("/ingredient")
    public ApiResult<List<Map<String, Object>>> ingredient(@RequestParam("startDate") String startDate,
                                                           @RequestParam("endDate") String endDate) {
        return ApiResult.ok(statsMapper.ingredientStats(startDate, endDate));
    }

    /** 菜品统计：startDate/endDate 格式 yyyy-MM-dd */
    @GetMapping("/dish")
    public ApiResult<List<Map<String, Object>>> dish(@RequestParam("startDate") String startDate,
                                                     @RequestParam("endDate") String endDate) {
        return ApiResult.ok(statsMapper.dishStats(startDate, endDate));
    }
}
