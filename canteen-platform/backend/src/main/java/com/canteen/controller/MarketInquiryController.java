package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.dto.PageResult;
import com.canteen.entity.MarketInquiry;
import com.canteen.mapper.MarketInquiryMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inquiry")
public class MarketInquiryController {

    @Resource
    private MarketInquiryMapper mapper;

    @GetMapping("/list")
    public ApiResult<PageResult<MarketInquiry>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                                      @RequestParam(value = "source", required = false) String source,
                                                      @RequestParam(value = "page", defaultValue = "1") int page,
                                                      @RequestParam(value = "size", defaultValue = "10") int size) {
        int offset = (page - 1) * size;
        long total = mapper.count(keyword, source);
        return ApiResult.ok(new PageResult<>(total, mapper.page(keyword, source, offset, size)));
    }

    // 新增时食材名称下拉：只能选食材管理中蔬菜类的食材
    @GetMapping("/vegetables")
    public ApiResult<List<String>> vegetables() {
        return ApiResult.ok(mapper.vegetables());
    }

    // 价格对比：同一食材在三个询价单位的最新报价
    @GetMapping("/compare")
    public ApiResult<List<Map<String, Object>>> compare(@RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResult.ok(mapper.compareLatest(keyword));
    }

    // 价格比对：食材管理蔬菜类各供应商单价 vs 市场询价均价/最高/最低
    @GetMapping("/veg-board")
    public ApiResult<List<Map<String, Object>>> vegBoard(@RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResult.ok(mapper.vegVsMarket(keyword));
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody MarketInquiry m, HttpSession session) {
        String err = validate(m);
        if (err != null) return ApiResult.error(err);
        String user = userOf(session);
        m.setCreatedBy(user);
        m.setUpdatedBy(user);
        mapper.insert(m);
        return ApiResult.ok();
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody MarketInquiry m, HttpSession session) {
        if (m.getId() == null) return ApiResult.error("缺少记录ID");
        String err = validate(m);
        if (err != null) return ApiResult.error(err);
        m.setUpdatedBy(userOf(session));
        mapper.update(m);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") Integer id) {
        mapper.delete(id);
        return ApiResult.ok();
    }

    private String validate(MarketInquiry m) {
        if (m.getSource() == null || m.getSource().trim().isEmpty()) return "请选择询价单位";
        if (m.getIngredientName() == null || m.getIngredientName().trim().isEmpty()) return "请选择食材名称";
        if (m.getPrice() == null) return "请填写单价";
        if (m.getPrice().compareTo(BigDecimal.ZERO) <= 0) return "单价必须大于 0";
        m.setSource(m.getSource().trim());
        m.setIngredientName(m.getIngredientName().trim());
        m.setUnit("kg");
        return null;
    }

    private String userOf(HttpSession session) {
        Object u = session.getAttribute("user");
        return u == null ? "admin" : u.toString();
    }
}
