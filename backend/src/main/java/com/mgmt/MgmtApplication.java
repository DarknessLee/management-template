package com.mgmt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({
        "com.mgmt.module.auth.mapper",
        "com.mgmt.module.user.mapper",
        "com.mgmt.module.role.mapper",
        "com.mgmt.module.permission.mapper",
        "com.mgmt.module.setting.mapper",
        "com.mgmt.module.log.mapper"
})
public class MgmtApplication {

    public static void main(String[] args) {
        SpringApplication.run(MgmtApplication.class, args);
    }
}
