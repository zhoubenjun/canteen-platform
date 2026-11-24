package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.dto.PageResult;
import com.canteen.entity.*;
import com.canteen.service.IngredientService;
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
@RequestMapping("/api/ingredient")
public class IngredientController {

    @Resource private IngredientService service;

    // ---------------- 食材 ----------------
    @GetMapping("/page")
    public ApiResult<PageResult<Ingredient>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Integer categoryId,
                                                  @RequestParam(required = false) Integer supplierId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "15") int size) {
        return ApiResult.ok(service.page(keyword, categoryId, supplierId, page, size));
    }

    @GetMapping("/all")
    public ApiResult<List<Ingredient>> all() {
        return ApiResult.ok(service.listAll());
    }

    @PostMapping("/save")
    public ApiResult<Ingredient> save(@RequestBody Ingredient i) {
        if (i.getId() == null) return ApiResult.ok(service.add(i));
        return ApiResult.ok(service.update(i));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ApiResult.ok();
    }

    @PostMapping("/deleteBatch")
    public ApiResult<Void> deleteBatch(@RequestBody List<Integer> ids) {
        service.deleteByIds(ids);
        return ApiResult.ok();
    }

    @DeleteMapping("/clearAll")
    public ApiResult<Void> clearAll() {
        service.deleteAll();
        return ApiResult.ok();
    }

    // ---------------- 类别 ----------------
    @GetMapping("/categories")
    public ApiResult<List<IngredientCategory>> categories() {
        return ApiResult.ok(service.categories());
    }
    @PostMapping("/category")
    public ApiResult<IngredientCategory> addCategory(@RequestBody IngredientCategory c) {
        return ApiResult.ok(service.addCategory(c.getName()));
    }
    @PutMapping("/category")
    public ApiResult<Void> updateCategory(@RequestBody IngredientCategory c) {
        service.updateCategory(c); return ApiResult.ok();
    }
    @DeleteMapping("/category/{id}")
    public ApiResult<Void> deleteCategory(@PathVariable Integer id) {
        service.deleteCategory(id); return ApiResult.ok();
    }

    // ---------------- 供应商 ----------------
    @GetMapping("/suppliers")
    public ApiResult<List<Supplier>> suppliers() {
        return ApiResult.ok(service.suppliers());
    }
    @PostMapping("/supplier")
    public ApiResult<Supplier> addSupplier(@RequestBody Supplier s) {
        return ApiResult.ok(service.addSupplier(s.getName()));
    }
    @PutMapping("/supplier")
    public ApiResult<Void> updateSupplier(@RequestBody Supplier s) {
        service.updateSupplier(s); return ApiResult.ok();
    }
    @DeleteMapping("/supplier/{id}")
    public ApiResult<Void> deleteSupplier(@PathVariable Integer id) {
        service.deleteSupplier(id); return ApiResult.ok();
    }

    // ---------------- 价格日记 ----------------
    @GetMapping("/logs")
    public ApiResult<List<PriceLog>> logs(@RequestParam(required = false) Integer ingredientId,
                                          @RequestParam(required = false) Integer categoryId,
                                          @RequestParam(required = false) Integer supplierId,
                                          @RequestParam(required = false) String changeType,
                                          @RequestParam(required = false) String start,
                                          @RequestParam(required = false) String end) {
        return ApiResult.ok(service.queryLog(ingredientId, categoryId, supplierId, changeType, start, end));
    }

    @GetMapping("/history/{ingredientId}")
    public ApiResult<List<Map<String, Object>>> history(@PathVariable Integer ingredientId) {
        return ApiResult.ok(service.history(ingredientId));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(service.stats());
    }

    @GetMapping("/exportLog")
    public ResponseEntity<byte[]> exportLog(@RequestParam(required = false) Integer ingredientId,
                                            @RequestParam(required = false) Integer categoryId,
                                            @RequestParam(required = false) Integer supplierId,
                                            @RequestParam(required = false) String changeType,
                                            @RequestParam(required = false) String start,
                                            @RequestParam(required = false) String end) throws Exception {
        List<PriceLog> logs = service.queryLog(ingredientId, categoryId, supplierId, changeType, start, end);
        byte[] b = service.exportLog(logs);
        return excel(b, "价格修改日记.xlsx");
    }

    // ---------------- 历史版本快照 ----------------
    @PostMapping("/snapshot")
    public ApiResult<PriceHistory> snapshot(@RequestBody(required = false) Map<String, String> body) throws Exception {
        String name = body == null ? null : body.get("versionName");
        return ApiResult.ok(service.saveSnapshot(name));
    }
    @GetMapping("/snapshots")
    public ApiResult<List<PriceHistory>> snapshots() {
        return ApiResult.ok(service.listSnapshots());
    }
    @PostMapping("/loadSnapshot/{id}")
    public ApiResult<Void> loadSnapshot(@PathVariable Integer id) {
        service.loadSnapshot(id); return ApiResult.ok();
    }
    @DeleteMapping("/snapshot/{id}")
    public ApiResult<Void> deleteSnapshot(@PathVariable Integer id) {
        service.deleteSnapshot(id); return ApiResult.ok();
    }

    // ---------------- Excel 导入导出 ----------------
    @GetMapping("/export")
    public ResponseEntity<byte[]> export() throws Exception {
        return excel(service.exportIngredients(), "食材价格数据.xlsx");
    }

    @PostMapping("/import")
    public ApiResult<Map<String, Object>> importData(@RequestParam("file") MultipartFile file) throws Exception {
        return ApiResult.ok(service.importIngredients(file.getBytes()));
    }

    private ResponseEntity<byte[]> excel(byte[] b, String filename) {
        String fn = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fn)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(b);
    }
}
