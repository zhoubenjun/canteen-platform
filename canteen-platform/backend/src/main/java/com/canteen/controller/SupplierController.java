package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.dto.PageResult;
import com.canteen.entity.Supplier;
import com.canteen.mapper.SupplierMapper;
import com.canteen.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supplier")
public class SupplierController {

    @Resource
    private SupplierMapper mapper;

    @Resource
    private FileService fileService;

    @GetMapping("/list")
    public ApiResult<PageResult<Supplier>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                @RequestParam(value = "size", defaultValue = "10") int size) {
        int offset = (page - 1) * size;
        long total = mapper.countByName(keyword);
        return ApiResult.ok(new PageResult<>(total, mapper.pageByName(keyword, offset, size)));
    }

    @PostMapping("/upload")
    public ApiResult<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws Exception {
        String url = fileService.saveImage(file, 1200, 0.85);
        Map<String, Object> m = new HashMap<>();
        m.put("url", url);
        return ApiResult.ok(m);
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody Supplier s, HttpSession session) {
        if (s.getName() == null || s.getName().trim().isEmpty()) {
            return ApiResult.error("供应商名称不能为空");
        }
        String name = s.getName().trim();
        Supplier ex = mapper.findByName(name);
        if (ex != null) {
            return ApiResult.error("供应商「" + name + "」已存在");
        }
        s.setName(name);
        String user = userOf(session);
        s.setCreatedBy(user);
        s.setUpdatedBy(user);
        mapper.insert(s);
        return ApiResult.ok();
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody Supplier s, HttpSession session) {
        if (s.getId() == null) {
            return ApiResult.error("缺少供应商ID");
        }
        if (s.getName() == null || s.getName().trim().isEmpty()) {
            return ApiResult.error("供应商名称不能为空");
        }
        s.setName(s.getName().trim());
        s.setUpdatedBy(userOf(session));
        mapper.update(s);
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
