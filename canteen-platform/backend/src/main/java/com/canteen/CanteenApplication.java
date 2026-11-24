package com.canteen;

import com.canteen.mapper.SysUserMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@MapperScan("com.canteen.mapper")
public class CanteenApplication {
    public static void main(String[] args) {
        SpringApplication.run(CanteenApplication.class, args);
    }

    /** 启动时服务端会话已全部丢失，统一把 online 标记清为离线，避免残留"假在线" */
    @Bean
    public CommandLineRunner resetOnlineState(SysUserMapper userMapper) {
        return args -> {
            try {
                userMapper.clearAllOnline();
            } catch (Exception ignored) {
            }
        };
    }
}
