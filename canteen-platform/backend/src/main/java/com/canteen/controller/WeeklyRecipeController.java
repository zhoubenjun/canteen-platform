package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.dto.AutoArrangeDto;
import com.canteen.entity.RecipeTemplate;
import com.canteen.entity.WeeklyRecipe;
import com.canteen.service.WeeklyRecipeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipe")
public class WeeklyRecipeController {

    @Resource private WeeklyRecipeService service;

    @GetMapping("/list")
    public ApiResult<List<WeeklyRecipe>> list(@RequestParam String weekKey) {
        return ApiResult.ok(service.listByWeek(weekKey));
    }

    @GetMapping("/weekKeys")
    public ApiResult<List<String>> weekKeys() {
        return ApiResult.ok(service.weekKeys());
    }

    @GetMapping("/currentMonday")
    public ApiResult<Map<String, Object>> currentMonday() {
        String monday = service.currentMonday();
        return ApiResult.ok(Map.of("weekKey", monday, "weekNumber", service.weekNumber(monday)));
    }

    @GetMapping("/weekNumber")
    public ApiResult<Map<String, Object>> weekNumber(@RequestParam String weekKey) {
        return ApiResult.ok(service.weekNumber(weekKey));
    }

    @PostMapping("/save")
    public ApiResult<Void> save(@RequestParam String weekKey, @RequestBody List<WeeklyRecipe> list) {
        service.saveWeek(weekKey, list);
        return ApiResult.ok();
    }

    @PostMapping("/calcPortions")
    public ApiResult<List<WeeklyRecipe>> calcPortions(@RequestBody List<WeeklyRecipe> list) {
        service.calcPortions(list);
        return ApiResult.ok(list);
    }

    @PostMapping("/copyDay")
    public ApiResult<Void> copyDay(@RequestParam String weekKey, @RequestParam int currentDay, @RequestParam int sourceDay) {
        service.copyDay(weekKey, currentDay, sourceDay);
        return ApiResult.ok();
    }

    @PostMapping("/copyLastWeek")
    public ApiResult<Map<String, Object>> copyLastWeek(@RequestParam String weekKey) throws Exception {
        return ApiResult.ok(service.copyLastWeek(weekKey));
    }

    /** 复制本周整周食谱到下一周（覆盖下一周原有食谱） */
    @PostMapping("/copyToNextWeek")
    public ApiResult<Map<String, Object>> copyToNextWeek(@RequestParam String weekKey) throws Exception {
        return ApiResult.ok(service.copyToNextWeek(weekKey));
    }

    // ---------------- 模板 ----------------
    @PostMapping("/template")
    public ApiResult<RecipeTemplate> saveTemplate(@RequestParam String weekKey,
                                                  @RequestParam(required = false) String name) throws Exception {
        return ApiResult.ok(service.saveTemplate(weekKey, name));
    }
    @GetMapping("/templates")
    public ApiResult<List<RecipeTemplate>> templates(@RequestParam(required = false) String keyword) {
        return ApiResult.ok(service.listTemplates(keyword));
    }
    @PostMapping("/loadTemplate")
    public ApiResult<Void> loadTemplate(@RequestParam Integer id, @RequestParam String weekKey) throws Exception {
        service.loadTemplate(id, weekKey);
        return ApiResult.ok();
    }
    @PutMapping("/template/rename")
    public ApiResult<Void> renameTemplate(@RequestParam Integer id, @RequestParam String name) {
        service.renameTemplate(id, name);
        return ApiResult.ok();
    }
    @DeleteMapping("/template/{id}")
    public ApiResult<Void> deleteTemplate(@PathVariable Integer id) {
        service.deleteTemplate(id);
        return ApiResult.ok();
    }

    // ---------------- 自动编排 ----------------
    @PostMapping("/autoArrange")
    public ApiResult<List<WeeklyRecipe>> autoArrange(@RequestBody AutoArrangeDto dto) {
        return ApiResult.ok(service.autoArrange(dto));
    }

    // ---------------- Excel ----------------
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String weekKey) throws Exception {
        byte[] b = service.exportWeek(weekKey);
        String fn = URLEncoder.encode("周食谱-" + weekKey + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fn)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(b);
    }

    @PostMapping("/import")
    public ApiResult<Map<String, Object>> importWeek(@RequestParam String weekKey,
                                                     @RequestParam("file") MultipartFile file) throws Exception {
        return ApiResult.ok(service.importWeek(weekKey, file.getBytes()));
    }
}
