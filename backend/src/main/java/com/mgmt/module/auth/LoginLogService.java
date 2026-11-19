package com.mgmt.module.auth;

import com.mgmt.module.auth.mapper.LoginLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogMapper loginLogMapper;

    /**
     * 分批删除指定时间之前的登录日志。
     * 登录日志只增不改，删除时没有业务逻辑需要联动，所以这里只做分批控制。
     */
    public int deleteBefore(LocalDateTime before, int batchSize) {
        int total = 0;
        int rounds = 0;
        int deleted;
        do {
            deleted = loginLogMapper.deleteBefore(before, batchSize);
            total += deleted;
            rounds++;
        } while (deleted == batchSize && rounds < 100);
        return total;
    }
}
