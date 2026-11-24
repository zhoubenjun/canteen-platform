package com.canteen.service;

import com.canteen.dto.PageResult;
import com.canteen.entity.Consumption;
import com.canteen.mapper.ConsumptionMapper;
import com.canteen.util.ParseUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConsumptionService {

    @Resource
    private ConsumptionMapper mapper;

    public PageResult<Consumption> page(String name, String registerType, String businessType,
                                        String start, String end, int page, int size) {
        int offset = Math.max(0, (page - 1) * size);
        List<Consumption> rows = mapper.page(name, registerType, businessType, start, end, offset, size);
        long total = mapper.count(name, registerType, businessType, start, end);
        return new PageResult<>(total, rows);
    }

    /** 读取消费 csv：仅保留教职工且属于所选月份的记录；先删除该月旧数据再写入 */
    public Map<String, Object> importFile(MultipartFile file, String month) throws Exception {
        List<Consumption> all = ParseUtil.readConsumption(file.getBytes());
        List<Consumption> list = new ArrayList<>();
        int skipped = 0;
        for (Consumption c : all) {
            if (inMonth(c.getConsumeDate(), month)) {
                list.add(c);
            } else {
                skipped++;
            }
        }
        mapper.deleteByMonth(month);
        batchInsert(list);
        Map<String, Object> result = new HashMap<>();
        result.put("month", month);
        result.put("total", list.size());
        result.put("skipped", skipped);
        return result;
    }

    private boolean inMonth(String date, String month) {
        return date != null && date.startsWith(month + "-");
    }

    private void batchInsert(List<Consumption> list) {
        int chunk = 500;
        for (int i = 0; i < list.size(); i += chunk) {
            mapper.batchInsert(new ArrayList<>(list.subList(i, Math.min(i + chunk, list.size()))));
        }
    }
}
