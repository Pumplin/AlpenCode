package org.ruoyi.system.judge;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Docker 沙箱执行引擎（使用 docker-java SDK，不依赖本地 docker CLI）
 */
@Slf4j
@Component
public class DockerSandbox {

    private static final String JAVA_IMAGE = "java17-sandbox";
    private static final String PYTHON_IMAGE = "python:3.11-slim";

    @Value("${judge.docker.host:tcp://localhost:2375}")
    private String dockerHost;

    private DockerClient dockerClient;

    @PostConstruct
    public void init() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(dockerHost)
            .build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(URI.create(dockerHost))
            .maxConnections(20)
            .connectionTimeout(Duration.ofSeconds(10))
            .responseTimeout(Duration.ofSeconds(120))
            .build();

        dockerClient = DockerClientImpl.getInstance(config, httpClient);
        log.info("DockerSandbox 初始化完成，连接远程 Docker: {}", dockerHost);
    }

    public record ExecResult(boolean success, String stdout, String stderr, int timeCostMs) {}

    /**
     * 批量执行：一个容器跑完所有测试用例
     * @param inputs 每个用例的 stdin（每个参数一行，参数间已换行）
     * @return 整体 stdout，各用例结果之间用 CASE_END 分隔
     */
    public ExecResult runJavaBatch(String userCode, String driverCode, List<String> inputs, int timeLimitMs) {
        String combinedInput = buildBatchInput(inputs);
        return runJava(userCode, driverCode, combinedInput, timeLimitMs * inputs.size());
    }

    public ExecResult runPythonBatch(String userCode, String driverCode, List<String> inputs, int timeLimitMs) {
        String combinedInput = buildBatchInput(inputs);
        return runPython(userCode, driverCode, combinedInput, timeLimitMs * inputs.size());
    }

    /** 将多组输入拼成批量 stdin，用空行分隔，末尾加结束标记 */
    private String buildBatchInput(List<String> inputs) {
        StringBuilder sb = new StringBuilder();
        for (String input : inputs) {
            sb.append(input.trim()).append("\n\n");
        }
        sb.append(DriverCodeGenerator.INPUT_END).append("\n");
        return sb.toString();
    }

    public ExecResult runJava(String userCode, String driverCode, String stdin, int timeLimitMs) {
        String solutionB64 = b64(userCode);
        String mainB64 = b64(driverCode);
        String inputB64 = b64(stdin);
        int timeoutSec = Math.max(2, timeLimitMs / 1000 + 2);

        String script = String.format(
            "echo %s | base64 -d > /tmp/Solution.java && " +
            "echo %s | base64 -d > /tmp/Main.java && " +
            "echo %s | base64 -d > /tmp/input.txt && " +
            "cd /tmp && javac Solution.java Main.java 2>&1 && " +
            "timeout %d java -cp /tmp Main < /tmp/input.txt",
            solutionB64, mainB64, inputB64, timeoutSec
        );

        return dockerRun(JAVA_IMAGE, script, timeoutSec + 5);
    }

    public ExecResult runPython(String userCode, String driverCode, String stdin, int timeLimitMs) {
        String fullCode = userCode + "\n\n" + driverCode;
        String codeB64 = b64(fullCode);
        String inputB64 = b64(stdin);
        int timeoutSec = Math.max(2, timeLimitMs / 1000 + 2);

        String script = String.format(
            "echo %s | base64 -d > /tmp/solution.py && " +
            "echo %s | base64 -d > /tmp/input.txt && " +
            "timeout %d python3 /tmp/solution.py < /tmp/input.txt",
            codeB64, inputB64, timeoutSec
        );

        return dockerRun(PYTHON_IMAGE, script, timeoutSec + 5);
    }

    private ExecResult dockerRun(String image, String script, int timeoutSec) {
        String containerId = null;
        long start = System.currentTimeMillis();
        try {
            HostConfig hostConfig = HostConfig.newHostConfig()
                .withNetworkMode("none")
                .withMemory(256 * 1024 * 1024L)
                .withCpuPeriod(100000L)
                .withCpuQuota(50000L);  // 0.5 CPU

            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withCmd("sh", "-c", script)
                .withHostConfig(hostConfig)
                .exec();

            containerId = container.getId();
            dockerClient.startContainerCmd(containerId).exec();

            // 收集输出
            OutputCollector collector = new OutputCollector();
            dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(collector)
                .awaitCompletion(timeoutSec, TimeUnit.SECONDS);

            int timeCost = (int) (System.currentTimeMillis() - start);

            // 获取退出码
            Integer exitCode = dockerClient.inspectContainerCmd(containerId)
                .exec().getState().getExitCodeLong().intValue();

            boolean success = exitCode == 0;
            return new ExecResult(success, collector.getStdout(), collector.getStderr(), timeCost);

        } catch (Exception e) {
            int timeCost = (int) (System.currentTimeMillis() - start);
            log.error("Docker 执行异常", e);
            String msg = e.getMessage() != null ? e.getMessage() : "执行超时";
            boolean isTle = msg.contains("timeout") || msg.contains("Timeout");
            return new ExecResult(false, "", isTle ? "Time Limit Exceeded" : msg, timeCost);
        } finally {
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception ignore) {}
            }
        }
    }

    private String b64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /** 收集容器 stdout/stderr 输出 */
    private static class OutputCollector extends ResultCallback.Adapter<Frame> {
        private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        @Override
        public void onNext(Frame frame) {
            if (frame.getStreamType().name().equals("STDOUT")) {
                try { stdout.write(frame.getPayload()); } catch (Exception ignore) {}
            } else {
                try { stderr.write(frame.getPayload()); } catch (Exception ignore) {}
            }
        }

        public String getStdout() { return stdout.toString(StandardCharsets.UTF_8).trim(); }
        public String getStderr() { return stderr.toString(StandardCharsets.UTF_8).trim(); }
    }
}
