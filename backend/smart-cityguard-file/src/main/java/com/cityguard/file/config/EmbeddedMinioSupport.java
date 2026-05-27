package com.cityguard.file.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 在 JVM 内启动 MinIO 子进程（仅用于 minio.embedded.enabled=true 的本地场景）。
 */
final class EmbeddedMinioSupport {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedMinioSupport.class);

    private static volatile Process process;

    private EmbeddedMinioSupport() {}

    static synchronized void ensureRunning(
            MinioEmbeddedProperties embedded,
            String endpoint,
            String accessKey,
            String secretKey
    ) {
        if (!embedded.isEnabled()) {
            return;
        }

        String host = normalizeHost(parseHost(endpoint));
        int port = parsePort(endpoint);

        if (portOpen(host, port, 250)) {
            log.info("MinIO 已在 {}:{} 监听，跳过内置启动", host, port);
            return;
        }

        Path data = Path.of(embedded.getDataDir());
        try {
            Files.createDirectories(data);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建 MinIO 数据目录: " + data, e);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(embedded.getBinary());
        cmd.add("server");
        cmd.add(data.toAbsolutePath().normalize().toString());
        cmd.add("--console-address");
        cmd.add(embedded.getConsoleAddress());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("MINIO_ROOT_USER", accessKey);
        pb.environment().put("MINIO_ROOT_PASSWORD", secretKey);
        pb.redirectErrorStream(true);
        pb.inheritIO();

        try {
            process = pb.start();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "启动 MinIO 失败。请将 minio 加入 PATH，或设置 minio.embedded.binary 为 minio.exe 绝对路径。",
                    e
            );
        }

        Runtime.getRuntime().addShutdownHook(new Thread(EmbeddedMinioSupport::destroyQuietly, "minio-embedded-shutdown"));

        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(embedded.getStartupTimeoutSeconds());
        while (System.currentTimeMillis() < deadline) {
            if (portOpen(host, port, 300)) {
                log.info("内置 MinIO 已就绪，API {}:{}，控制台 {}", host, port, embedded.getConsoleAddress());
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                destroyQuietly();
                throw new IllegalStateException("等待 MinIO 就绪被打断", e);
            }
            if (process != null && !process.isAlive()) {
                throw new IllegalStateException("MinIO 进程已退出，请查看控制台输出");
            }
        }
        destroyQuietly();
        throw new IllegalStateException(
                "MinIO 在 " + embedded.getStartupTimeoutSeconds() + " 秒内未在端口 " + port + " 就绪"
        );
    }

    static void destroyQuietly() {
        Process p = process;
        if (p != null && p.isAlive()) {
            p.destroyForcibly();
        }
        process = null;
    }

    private static boolean portOpen(String host, int port, int timeoutMs) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static int parsePort(String endpoint) {
        try {
            URI u = URI.create(endpoint.trim());
            int p = u.getPort();
            if (p > 0) {
                return p;
            }
            if ("https".equalsIgnoreCase(u.getScheme())) {
                return 443;
            }
            return 80;
        } catch (Exception e) {
            return 9000;
        }
    }

    private static String parseHost(String endpoint) {
        try {
            URI u = URI.create(endpoint.trim());
            String h = u.getHost();
            return h != null ? h : "127.0.0.1";
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private static String normalizeHost(String host) {
        if ("localhost".equalsIgnoreCase(host)) {
            return "127.0.0.1";
        }
        return host;
    }
}
