package com.canteen.service;

import com.canteen.dto.PageResult;
import com.canteen.entity.Companion;
import com.canteen.mapper.CompanionMapper;
import com.canteen.util.ParseUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompanionService {

    @Resource
    private CompanionMapper mapper;

    public PageResult<Companion> page(String name, String start, String end, String mealType,
                                       int page, int size) {
        int offset = Math.max(0, (page - 1) * size);
        List<Companion> rows = mapper.page(name, start, end, mealType, offset, size);
        long total = mapper.count(name, start, end, mealType);
        return new PageResult<>(total, rows);
    }

    /** 读取陪餐 xlsx：仅保留属于所选月份的记录；先删除该月旧数据再写入 */
    public Map<String, Object> importFile(MultipartFile file, String month) throws Exception {
        List<Companion> all = ParseUtil.readCompanion(file.getBytes());
        List<Companion> list = new ArrayList<>();
        int skipped = 0;
        for (Companion c : all) {
            if (inMonth(c.getCompanionDate(), month)) {
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

    private void batchInsert(List<Companion> list) {
        int chunk = 500;
        for (int i = 0; i < list.size(); i += chunk) {
            mapper.batchInsert(new ArrayList<>(list.subList(i, Math.min(i + chunk, list.size()))));
        }
    }
}
