package com.canteen.service;

import com.canteen.entity.SysSetting;
import com.canteen.mapper.SysSettingMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysSettingService {

    @Resource
    private SysSettingMapper sysSettingMapper;

    public Map<String, String> getAll() {
        Map<String, String> map = new HashMap<>();
        for (SysSetting s : sysSettingMapper.listAll()) {
            map.put(s.getSettingKey(), s.getSettingValue());
        }
        return map;
    }

    public String get(String key, String def) {
        SysSetting s = sysSettingMapper.findByKey(key);
        return s == null || s.getSettingValue() == null ? def : s.getSettingValue();
    }

    public BigDecimal getDecimal(String key, BigDecimal def) {
        String v = get(key, null);
        if (v == null) return def;
        try { return new BigDecimal(v); } catch (Exception e) { return def; }
    }

    public void set(String key, String value, String remark) {
        SysSetting s = new SysSetting();
        s.setSettingKey(key);
        s.setSettingValue(value);
        s.setRemark(remark);
        sysSettingMapper.upsert(s);
    }

    public void updateAll(Map<String, String> map) {
        for (Map.Entry<String, String> e : map.entrySet()) {
            set(e.getKey(), e.getValue(), null);
        }
    }
}
