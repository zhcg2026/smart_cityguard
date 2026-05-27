package com.cityguard.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Paths;

/**
 * 本地开发可选：由后端进程顺带拉起 MinIO，无需单独开终端。
 * 生产环境务必保持 enabled=false。
 */
@ConfigurationProperties(prefix = "minio.embedded")
public class MinioEmbeddedProperties {

    /** 是否由应用启动时拉起 minio 进程 */
    private boolean enabled = false;

    /** MinIO 数据目录 */
    private String dataDir = Paths.get(System.getProperty("user.home"), "minio-data").toString();

    /** minio 可执行文件：在 PATH 中的名称（如 minio）或绝对路径（如 D:/tools/minio.exe） */
    private String binary = "minio";

    /** 控制台监听，如 :9001 */
    private String consoleAddress = ":9001";

    /** 等待 API 端口可连的最长时间（秒） */
    private int startupTimeoutSeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getBinary() {
        return binary;
    }

    public void setBinary(String binary) {
        this.binary = binary;
    }

    public String getConsoleAddress() {
        return consoleAddress;
    }

    public void setConsoleAddress(String consoleAddress) {
        this.consoleAddress = consoleAddress;
    }

    public int getStartupTimeoutSeconds() {
        return startupTimeoutSeconds;
    }

    public void setStartupTimeoutSeconds(int startupTimeoutSeconds) {
        this.startupTimeoutSeconds = startupTimeoutSeconds;
    }
}
