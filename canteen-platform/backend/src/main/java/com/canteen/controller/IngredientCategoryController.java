package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.dto.PageResult;
import com.canteen.entity.IngredientCategory;
import com.canteen.mapper.IngredientCategoryMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/ingredient-category")
public class IngredientCategoryController {

    @Resource
    private IngredientCategoryMapper mapper;

    @GetMapping("/list")
    public ApiResult<PageResult<IngredientCategory>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                                          @RequestParam(value = "page", defaultValue = "1") int page,
                                                          @RequestParam(value = "size", defaultValue = "10") int size) {
        int offset = (page - 1) * size;
        long total = mapper.countByName(keyword);
        return ApiResult.ok(new PageResult<>(total, mapper.pageByName(keyword, offset, size)));
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody IngredientCategory c, HttpSession session) {
        if (c.getName() == null || c.getName().trim().isEmpty()) {
            return ApiResult.error("类别名称不能为空");
        }
        String name = c.getName().trim();
        IngredientCategory ex = mapper.findByName(name);
        if (ex != null) {
            return ApiResult.error("类别「" + name + "」已存在");
        }
        c.setName(name);
        String user = userOf(session);
        c.setCreatedBy(user);
        c.setUpdatedBy(user);
        mapper.insert(c);
        return ApiResult.ok();
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody IngredientCategory c, HttpSession session) {
        if (c.getId() == null) {
            return ApiResult.error("缺少类别ID");
        }
        if (c.getName() == null || c.getName().trim().isEmpty()) {
            return ApiResult.error("类别名称不能为空");
        }
        c.setName(c.getName().trim());
        c.setUpdatedBy(userOf(session));
        mapper.update(c);
        return ApiResult.ok();
    }

    @PostMapping("/status")
    public ApiResult<Void> setStatus(@RequestParam(value = "id") Integer id,
                                     @RequestParam(value = "status") Integer status,
                                     HttpSession session) {
        IngredientCategory c = new IngredientCategory();
        c.setId(id);
        c.setStatus(status == null ? 1 : status);
        c.setUpdatedBy(userOf(session));
        mapper.updateStatus(c);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") Integer id) {
        mapper.delete(id);
        return ApiResult.ok();
    }

    private String userOf(HttpSession session) {
        Object u = session.getAttribute("user");
        return u == null ? "admin" : u.toString();
    }
}
