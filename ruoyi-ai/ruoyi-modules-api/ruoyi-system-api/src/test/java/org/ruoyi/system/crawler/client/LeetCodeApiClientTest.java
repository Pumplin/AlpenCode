package org.ruoyi.system.crawler.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.system.crawler.domain.dto.LeetCodeProblemDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * LeetCodeApiClient 单元测试
 *
 * @author AlpenCode
 */
class LeetCodeApiClientTest {

    private MockWebServer mockWebServer;
    private LeetCodeApiClient apiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder webClientBuilder = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        apiClient = new LeetCodeApiClient(webClientBuilder);
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("测试获取题目列表 - 成功场景")
    void testFetchProblemList_Success() {
        // Given: 模拟 LeetCode API 响应
        String mockResponse = """
            {
              "data": {
                "problemsetQuestionList": {
                  "total": 2,
                  "questions": [
                    {
                      "questionId": "1",
                      "questionFrontendId": "1",
                      "title": "Two Sum",
                      "titleSlug": "two-sum",
                      "difficulty": "Easy",
                      "topicTags": [
                        {"name": "Array", "slug": "array"},
                        {"name": "Hash Table", "slug": "hash-table"}
                      ]
                    },
                    {
                      "questionId": "2",
                      "questionFrontendId": "2",
                      "title": "Add Two Numbers",
                      "titleSlug": "add-two-numbers",
                      "difficulty": "Medium",
                      "topicTags": [
                        {"name": "Linked List", "slug": "linked-list"}
                      ]
                    }
                  ]
                }
              }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setBody(mockResponse)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // When: 调用获取题目列表
        List<LeetCodeProblemDTO> problems = apiClient.fetchProblemList(2);

        // Then: 验证结果
        assertThat(problems).hasSize(2);

        LeetCodeProblemDTO problem1 = problems.get(0);
        assertThat(problem1.getTitle()).isEqualTo("Two Sum");
        assertThat(problem1.getTitleSlug()).isEqualTo("two-sum");
        assertThat(problem1.getDifficulty()).isEqualTo("Easy");
        assertThat(problem1.getTags()).containsExactly("Array", "Hash Table");

        LeetCodeProblemDTO problem2 = problems.get(1);
        assertThat(problem2.getTitle()).isEqualTo("Add Two Numbers");
        assertThat(problem2.getTitleSlug()).isEqualTo("add-two-numbers");
        assertThat(problem2.getDifficulty()).isEqualTo("Medium");
        assertThat(problem2.getTags()).containsExactly("Linked List");
    }

    @Test
    @DisplayName("测试获取题目列表 - 空列表")
    void testFetchProblemList_EmptyList() {
        // Given: 模拟空列表响应
        String mockResponse = """
            {
              "data": {
                "problemsetQuestionList": {
                  "total": 0,
                  "questions": []
                }
              }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setBody(mockResponse)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // When: 调用获取题目列表
        List<LeetCodeProblemDTO> problems = apiClient.fetchProblemList(10);

        // Then: 验证结果为空
        assertThat(problems).isEmpty();
    }

    @Test
    @DisplayName("测试获取题目列表 - API 错误响应")
    void testFetchProblemList_ApiError() {
        // Given: 模拟 API 错误响应
        String mockResponse = """
            {
              "errors": [
                {"message": "Invalid query"}
              ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setBody(mockResponse)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // When & Then: 验证抛出异常
        assertThatThrownBy(() -> apiClient.fetchProblemList(10))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("LeetCode API 错误");
    }

    @Test
    @DisplayName("测试获取题目详情 - 成功场景")
    void testFetchProblemDetail_Success() throws InterruptedException {
        // Given: 模拟题目详情响应
        String mockResponse = """
            {
              "data": {
                "question": {
                  "questionId": "1",
                  "questionFrontendId": "1",
                  "title": "Two Sum",
                  "titleSlug": "two-sum",
                  "translatedTitle": "两数之和",
                  "content": "<p>English description</p>",
                  "translatedContent": "<p>给定一个整数数组和一个目标值...</p>",
                  "difficulty": "Easy",
                  "topicTags": [
                    {"name": "Array", "slug": "array", "translatedName": "数组"},
                    {"name": "Hash Table", "slug": "hash-table", "translatedName": "哈希表"}
                  ],
                  "exampleTestcases": "[2,7,11,15]\\n9\\n[3,2,4]\\n6"
                }
              }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setBody(mockResponse)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // When: 调用获取题目详情
        LeetCodeProblemDTO problem = apiClient.fetchProblemDetail("two-sum");

        // Then: 验证结果
        assertThat(problem).isNotNull();
        assertThat(problem.getTitle()).isEqualTo("两数之和");
        assertThat(problem.getTitleSlug()).isEqualTo("two-sum");
        assertThat(problem.getDifficulty()).isEqualTo("Easy");
        assertThat(problem.getDescription()).contains("给定一个整数数组");
        assertThat(problem.getTags()).containsExactly("数组", "哈希表");
        assertThat(problem.getExampleTestCases()).hasSize(2);
    }

    @Test
    @DisplayName("测试获取题目详情 - 题目不存在")
    void testFetchProblemDetail_NotFound() {
        // Given: 模拟题目不存在响应
        String mockResponse = """
            {
              "data": {
                "question": null
              }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setBody(mockResponse)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // When & Then: 验证抛出异常
        assertThatThrownBy(() -> apiClient.fetchProblemDetail("non-existent"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("题目不存在");
    }

    @Test
    @DisplayName("测试请求频率控制")
    void testRateLimit() throws InterruptedException {
        // Given: 准备两个成功响应
        String mockResponse = """
            {
              "data": {
                "question": {
                  "questionId": "1",
                  "title": "Test",
                  "titleSlug": "test",
                  "translatedTitle": "测试",
                  "content": "test",
                  "translatedContent": "测试",
                  "difficulty": "Easy",
                  "topicTags": []
                }
              }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setBody(mockResponse)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        mockWebServer.enqueue(new MockResponse()
            .setBody(mockResponse)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // When: 连续调用两次
        long startTime = System.currentTimeMillis();
        apiClient.fetchProblemDetail("test1");
        apiClient.fetchProblemDetail("test2");
        long endTime = System.currentTimeMillis();

        // Then: 验证两次请求之间至少间隔 500ms
        long elapsed = endTime - startTime;
        assertThat(elapsed).isGreaterThanOrEqualTo(500);
    }
}
