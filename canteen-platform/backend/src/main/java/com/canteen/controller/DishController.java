package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.dto.CostSettingsDto;
import com.canteen.dto.DishSaveDto;
import com.canteen.dto.PageResult;
import com.canteen.entity.Dish;
import com.canteen.service.DishService;
import com.canteen.service.FileService;
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
@RequestMapping("/api/dish")
public class DishController {

    @Resource private DishService dishService;
    @Resource private FileService fileService;

    @GetMapping("/categories")
    public ApiResult<List<String>> categories() {
        return ApiResult.ok(dishService.categories());
    }

    @GetMapping("/page")
    public ApiResult<PageResult<Dish>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) Boolean unPriced,
                                            @RequestParam(required = false) String ingredientKeyword,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "12") int size) {
        return ApiResult.ok(dishService.page(keyword, category, unPriced, ingredientKeyword, page, size));
    }

    @GetMapping("/all")
    public ApiResult<List<Dish>> all() {
        return ApiResult.ok(dishService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResult<Dish> one(@PathVariable Integer id) {
        return ApiResult.ok(dishService.findById(id));
    }

    @PostMapping("/save")
    public ApiResult<Dish> save(@RequestBody DishSaveDto dto) {
        return ApiResult.ok(dishService.save(dto));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Integer id) {
        dishService.delete(id);
        return ApiResult.ok();
    }

    @PostMapping("/deleteBatch")
    public ApiResult<Void> deleteBatch(@RequestBody List<Integer> ids) {
        dishService.deleteByIds(ids);
        return ApiResult.ok();
    }

    @PostMapping("/costSettings")
    public ApiResult<Void> saveCostSettings(@RequestBody CostSettingsDto dto) {
        dishService.saveCostSettings(dto);
        return ApiResult.ok();
    }

    @GetMapping("/costSettings")
    public ApiResult<Map<String, Object>> costSettings() {
        return ApiResult.ok(dishService.costSettings());
    }

    @PostMapping("/recalcAll")
    public ApiResult<Void> recalcAll() {
        dishService.recalcAll();
        return ApiResult.ok();
    }

    @GetMapping("/byRecipe")
    public ApiResult<List<Dish>> byRecipe(@RequestParam String weekKey,
                                          @RequestParam(required = false) String mealType) {
        return ApiResult.ok(dishService.listByRecipe(weekKey, mealType));
    }

    /** 上传菜品图片（800px/0.8压缩） */
    @PostMapping("/uploadImage")
    public ApiResult<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(required = false) Integer dishId) throws Exception {
        String url = fileService.saveImage(file, 800, 0.8);
        if (dishId != null) dishService.updateImage(dishId, url);
        return ApiResult.ok(Map.of("url", url));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() throws Exception {
        byte[] b = dishService.exportDishes();
        return excel(b, "菜品库数据.xlsx");
    }

    @PostMapping("/import")
    public ApiResult<Map<String, Object>> importDishes(@RequestParam("file") MultipartFile file) throws Exception {
        return ApiResult.ok(dishService.importDishes(file.getBytes()));
    }

    private ResponseEntity<byte[]> excel(byte[] b, String filename) {
        String fn = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fn)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(b);
    }
}
