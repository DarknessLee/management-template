package com.mgmt.module.job;

/**
 * 定时任务处理器。任务只允许从这个注册表中选择，避免执行数据库里的任意代码。
 */
public interface JobHandler {

    /** 唯一标识，入库保存在 sys_job.handler_key */
    String key();

    String name();

    String description();

    /** 执行任务，返回值会作为本次执行结果展示 */
    String execute() throws Exception;
}
