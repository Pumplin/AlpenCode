package org.ruoyi.system.crawler.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient 配置类
 * 用于配置 LeetCode API 客户端的 HTTP 请求设置
 *
 * @author ruoyi
 */
@Configuration
public class WebClientConfig {

    /**
     * LeetCode 配置属性
     */
    public static class LeetCodeApiProperties {
        /**
         * LeetCode API 地址
         */
        private String url = "https://leetcode.cn/graphql/";

        /**
         * 请求超时时间（毫秒）
         */
        private Integer timeout = 10000;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * 注册 LeetCode API 配置属性 Bean
     */
    @Bean
    @ConfigurationProperties(prefix = "leetcode.api")
    public LeetCodeApiProperties leetCodeApiProperties() {
        return new LeetCodeApiProperties();
    }

    /**
     * 配置 WebClient.Builder Bean
     * 设置 baseUrl、headers、timeout 等参数
     *
     * @param properties LeetCode API 配置属性
     * @return WebClient.Builder
     */
    @Bean
    public WebClient.Builder webClientBuilder(LeetCodeApiProperties properties) {
        // 配置 HttpClient，设置连接超时和读写超时
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getTimeout())
            .responseTimeout(Duration.ofMillis(properties.getTimeout()))
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(properties.getTimeout(), TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(properties.getTimeout(), TimeUnit.MILLISECONDS))
            );

        // 创建 WebClient.Builder
        return WebClient.builder()
            .baseUrl(properties.getUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
