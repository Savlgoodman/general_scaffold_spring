package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "系统监控信息")
@Data
public class SystemInfoVO {

    @Schema(description = "CPU信息")
    private SystemCpuInfo cpu;

    @Schema(description = "内存信息")
    private SystemMemoryInfo memory;

    @Schema(description = "JVM信息")
    private SystemJvmInfo jvm;

    @Schema(description = "磁盘信息")
    private List<SystemDiskInfo> disks;

    @Schema(description = "Redis信息")
    private SystemRedisInfo redis;

    @Schema(description = "数据库连接池信息")
    private SystemDbPoolInfo dbPool;

    @Schema(description = "服务器信息")
    private SystemServerInfo server;

    @Schema(description = "CPU信息")
    @Data
    public static class SystemCpuInfo {
        @Schema(description = "核心数")
        private int coreCount;
        @Schema(description = "系统使用率(%)")
        private double systemUsage;
        @Schema(description = "用户使用率(%)")
        private double userUsage;
        @Schema(description = "空闲率(%)")
        private double idle;
    }

    @Schema(description = "内存信息")
    @Data
    public static class SystemMemoryInfo {
        @Schema(description = "总内存(字节)")
        private long total;
        @Schema(description = "已用内存(字节)")
        private long used;
        @Schema(description = "可用内存(字节)")
        private long available;
        @Schema(description = "使用率(%)")
        private double usageRate;
    }

    @Schema(description = "JVM信息")
    @Data
    public static class SystemJvmInfo {
        @Schema(description = "最大内存(字节)")
        private long maxMemory;
        @Schema(description = "已分配内存(字节)")
        private long totalMemory;
        @Schema(description = "已用内存(字节)")
        private long usedMemory;
        @Schema(description = "空闲内存(字节)")
        private long freeMemory;
        @Schema(description = "使用率(%)")
        private double usageRate;
        @Schema(description = "Java版本")
        private String javaVersion;
        @Schema(description = "运行时长")
        private String uptime;
    }

    @Schema(description = "磁盘信息")
    @Data
    public static class SystemDiskInfo {
        @Schema(description = "盘符/挂载点")
        private String mount;
        @Schema(description = "文件系统类型")
        private String fsType;
        @Schema(description = "总空间(字节)")
        private long total;
        @Schema(description = "已用空间(字节)")
        private long used;
        @Schema(description = "可用空间(字节)")
        private long available;
        @Schema(description = "使用率(%)")
        private double usageRate;
    }

    @Schema(description = "Redis信息")
    @Data
    public static class SystemRedisInfo {
        @Schema(description = "Redis版本")
        private String version;
        @Schema(description = "已用内存")
        private String usedMemory;
        @Schema(description = "连接数")
        private String connectedClients;
        @Schema(description = "运行天数")
        private String uptimeDays;
        @Schema(description = "Key数量")
        private String keyCount;
    }

    @Schema(description = "数据库连接池信息")
    @Data
    public static class SystemDbPoolInfo {
        @Schema(description = "活跃连接数")
        private int activeConnections;
        @Schema(description = "空闲连接数")
        private int idleConnections;
        @Schema(description = "总连接数")
        private int totalConnections;
        @Schema(description = "等待线程数")
        private int threadsAwaitingConnection;
    }

    @Schema(description = "服务器信息")
    @Data
    public static class SystemServerInfo {
        @Schema(description = "操作系统")
        private String osName;
        @Schema(description = "系统架构")
        private String osArch;
        @Schema(description = "主机名")
        private String hostName;
        @Schema(description = "IP地址")
        private String ip;
    }
}
