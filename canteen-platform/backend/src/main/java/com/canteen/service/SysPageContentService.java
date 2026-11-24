package com.canteen.service;

import com.canteen.entity.SysPageContent;
import com.canteen.mapper.SysPageContentMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class SysPageContentService {

    @Resource
    private SysPageContentMapper mapper;

    public List<SysPageContent> list() {
        return mapper.listAll();
    }

    public SysPageContent get(String key) {
        return mapper.findByKey(key);
    }

    public void save(String key, String title, String content, String operator) {
        SysPageContent p = new SysPageContent();
        p.setPageKey(key);
        p.setPageTitle(title == null || title.trim().isEmpty() ? key : title.trim());
        p.setContent(content == null ? "" : content);
        p.setUpdatedBy(operator);
        mapper.upsert(p);
    }
}
