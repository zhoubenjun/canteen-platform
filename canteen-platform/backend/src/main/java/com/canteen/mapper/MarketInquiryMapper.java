package com.canteen.mapper;

import com.canteen.entity.MarketInquiry;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface MarketInquiryMapper {
    long count(@Param("keyword") String keyword, @Param("source") String source);
    List<MarketInquiry> page(@Param("keyword") String keyword, @Param("source") String source,
                             @Param("offset") int offset, @Param("size") int size);
    MarketInquiry findById(@Param("id") Integer id);
    int insert(MarketInquiry m);
    int update(MarketInquiry m);
    int delete(@Param("id") Integer id);
    // 蔬菜类食材（供新增时下拉选择）
    List<String> vegetables();
    // 各食材在各询价单位的最新报价（价格对比）
    List<Map<String, Object>> compareLatest(@Param("keyword") String keyword);
    // 食材管理蔬菜类各供应商单价 vs 市场询价均价/最高/最低（价格比对页）
    List<Map<String, Object>> vegVsMarket(@Param("keyword") String keyword);
}
