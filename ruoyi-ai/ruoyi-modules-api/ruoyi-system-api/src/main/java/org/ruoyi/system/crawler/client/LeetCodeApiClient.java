package org.ruoyi.system.crawler.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.system.crawler.domain.dto.LeetCodeProblemDTO;
import org.ruoyi.system.crawler.domain.dto.TestCaseDTO;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * LeetCode API 客户端
 * 负责调用 LeetCode 的 GraphQL API 获取题目数据
 *
 * @author AlpenCode
 */
@Component
@Slf4j
public class LeetCodeApiClient {

    /**
     * 请求延迟控制（毫秒）
     */
    private static final int REQUEST_DELAY_MS = 500;

    /**
     * 上次请求时间戳
     */
    private long lastRequestTime = 0;

    private final WebClient webClient;

    public LeetCodeApiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * 获取题目列表
     *
     * @param limit 获取题目数量
     * @return 题目列表
     */
    @Retryable(
        retryFor = {WebClientRequestException.class, TimeoutException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000)
    )
    public List<LeetCodeProblemDTO> fetchProblemList(int limit) {
        log.info("开始获取 LeetCode 题目列表，数量: {}", limit);

        try {
            // 构建 GraphQL 查询
            String query = buildProblemListQuery();
            Map<String, Object> variables = buildProblemListVariables(limit);
            Map<String, Object> requestBody = buildGraphQLRequest(query, variables);

            // 发送请求
            JsonNode response = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .block();

            // 验证响应
            validateApiResponse(response);

            // 解析题目列表
            List<LeetCodeProblemDTO> problems = parseProblemList(response);
            log.info("成功获取题目列表，数量: {}", problems.size());

            return problems;

        } catch (WebClientRequestException e) {
            log.error("网络请求失败: {}", e.getMessage());
            throw new ServiceException("无法连接到 LeetCode API");
        } catch (WebClientResponseException e) {
            log.error("LeetCode API 返回错误: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ServiceException("LeetCode API 返回错误: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("获取题目列表失败", e);
            throw new ServiceException("获取题目列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取题目详情
     *
     * @param titleSlug 题目唯一标识
     * @return 题目详情
     * @throws InterruptedException 线程中断异常
     */
    @Retryable(
        retryFor = {WebClientRequestException.class, TimeoutException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000)
    )
    public LeetCodeProblemDTO fetchProblemDetail(String titleSlug) throws InterruptedException {
        log.debug("开始获取题目详情: {}", titleSlug);

        // 请求频率控制
        applyRateLimit();

        try {
            // 构建 GraphQL 查询
            String query = buildProblemDetailQuery();
            Map<String, Object> variables = buildProblemDetailVariables(titleSlug);
            Map<String, Object> requestBody = buildGraphQLRequest(query, variables);

            // 发送请求
            JsonNode response = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .block();

            // 验证响应
            validateApiResponse(response);

            // 解析题目详情
            LeetCodeProblemDTO problem = parseProblemDetail(response);
            log.debug("成功获取题目详情: {}", titleSlug);

            return problem;

        } catch (WebClientRequestException e) {
            log.error("网络请求失败: titleSlug={}, error={}", titleSlug, e.getMessage());
            throw new ServiceException("无法连接到 LeetCode API");
        } catch (WebClientResponseException e) {
            log.error("LeetCode API 返回错误: titleSlug={}, status={}", titleSlug, e.getStatusCode());
            throw new ServiceException("LeetCode API 返回错误: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("获取题目详情失败: titleSlug={}", titleSlug, e);
            throw new ServiceException("获取题目详情失败: " + e.getMessage());
        }
    }

    /**
     * 应用请求频率限制
     * 确保两次请求之间至少间隔 500ms
     */
    private synchronized void applyRateLimit() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastRequestTime;

        if (lastRequestTime > 0 && elapsed < REQUEST_DELAY_MS) {
            long sleepTime = REQUEST_DELAY_MS - elapsed;
            log.debug("请求频率控制，等待 {} ms", sleepTime);
            Thread.sleep(sleepTime);
        }

        lastRequestTime = System.currentTimeMillis();
    }

    /**
     * 构建 GraphQL 请求体
     */
    private Map<String, Object> buildGraphQLRequest(String query, Map<String, Object> variables) {
        Map<String, Object> request = new HashMap<>();
        request.put("query", query);
        request.put("variables", variables);
        return request;
    }

    /**
     * 构建题目列表查询语句
     */
    private String buildProblemListQuery() {
        return """
            query problemsetQuestionList($categorySlug: String, $limit: Int, $skip: Int, $filters: QuestionListFilterInput) {
              problemsetQuestionList(
                categorySlug: $categorySlug
                limit: $limit
                skip: $skip
                filters: $filters
              ) {
                hasMore
                total
                questions {
                  frontendQuestionId
                  title
                  titleCn
                  titleSlug
                  difficulty
                  topicTags {
                    name
                    nameTranslated
                    slug
                  }
                }
              }
            }
            """;
    }

    /**
     * 构建题目列表查询变量
     */
    private Map<String, Object> buildProblemListVariables(int limit) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("categorySlug", "");
        variables.put("limit", limit);
        variables.put("skip", 0);
        variables.put("filters", new HashMap<>());
        return variables;
    }

    /**
     * 构建题目详情查询语句
     */
    private String buildProblemDetailQuery() {
        return """
            query ($titleSlug: String!) {
              question(titleSlug: $titleSlug) {
                questionId
                questionFrontendId
                title
                titleSlug
                content
                translatedTitle
                translatedContent
                difficulty
                topicTags {
                  name
                  slug
                  translatedName
                }
                exampleTestcases
                codeSnippets {
                  lang
                  langSlug
                  code
                }
                metaData
              }
            }
            """;
    }

    /**
     * 构建题目详情查询变量
     */
    private Map<String, Object> buildProblemDetailVariables(String titleSlug) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("titleSlug", titleSlug);
        return variables;
    }

    /**
     * 验证 API 响应
     */
    private void validateApiResponse(JsonNode response) {
        if (response == null) {
            throw new ServiceException("LeetCode API 返回空响应");
        }

        // 检查是否有错误
        if (response.has("errors") && !response.get("errors").isEmpty()) {
            JsonNode errors = response.get("errors");
            StringBuilder errorMsg = new StringBuilder("LeetCode API 错误: ");
            errors.forEach(error -> {
                if (error.has("message")) {
                    errorMsg.append(error.get("message").asText()).append("; ");
                }
            });
            throw new ServiceException(errorMsg.toString());
        }

        // 检查是否有数据
        if (!response.has("data") || response.get("data").isNull()) {
            throw new ServiceException("LeetCode API 返回空数据");
        }
    }

    /**
     * 解析题目列表响应
     */
    private List<LeetCodeProblemDTO> parseProblemList(JsonNode response) {
        List<LeetCodeProblemDTO> problems = new ArrayList<>();

        JsonNode data = response.get("data");
        if (!data.has("problemsetQuestionList")) {
            log.warn("响应中缺少 problemsetQuestionList 字段");
            return problems;
        }

        JsonNode questionList = data.get("problemsetQuestionList");
        if (!questionList.has("questions")) {
            log.warn("响应中缺少 questions 字段");
            return problems;
        }

        JsonNode questions = questionList.get("questions");
        for (JsonNode question : questions) {
            try {
                LeetCodeProblemDTO problem = new LeetCodeProblemDTO();
                problem.setTitleSlug(question.get("titleSlug").asText());
                // 优先使用中文标题
                String title = question.has("titleCn") && !question.get("titleCn").isNull()
                    ? question.get("titleCn").asText()
                    : question.get("title").asText();
                problem.setTitle(title);
                problem.setDifficulty(question.get("difficulty").asText());

                // 解析标签
                List<String> tags = new ArrayList<>();
                if (question.has("topicTags")) {
                    JsonNode topicTags = question.get("topicTags");
                    topicTags.forEach(tag -> {
                        if (tag.has("name")) {
                            tags.add(tag.get("name").asText());
                        }
                    });
                }
                problem.setTags(tags);

                problems.add(problem);
            } catch (Exception e) {
                log.warn("解析题目失败: {}", question, e);
            }
        }

        return problems;
    }

    /**
     * 解析题目详情响应
     */
    private LeetCodeProblemDTO parseProblemDetail(JsonNode response) {
        JsonNode data = response.get("data");
        if (!data.has("question")) {
            throw new ServiceException("响应中缺少 question 字段");
        }

        JsonNode question = data.get("question");
        if (question.isNull()) {
            throw new ServiceException("题目不存在或已被删除");
        }

        LeetCodeProblemDTO problem = new LeetCodeProblemDTO();
        problem.setTitleSlug(question.get("titleSlug").asText());
        problem.setTitle(question.has("translatedTitle") && !question.get("translatedTitle").isNull()
            ? question.get("translatedTitle").asText()
            : question.get("title").asText());
        problem.setDifficulty(question.get("difficulty").asText());

        // 使用中文描述，如果没有则使用英文描述
        String description = question.has("translatedContent") && !question.get("translatedContent").isNull()
            ? question.get("translatedContent").asText()
            : question.get("content").asText();
        problem.setDescription(description);

        // 解析标签（优先使用中文标签）
        List<String> tags = new ArrayList<>();
        if (question.has("topicTags")) {
            JsonNode topicTags = question.get("topicTags");
            topicTags.forEach(tag -> {
                String tagName = tag.has("translatedName") && !tag.get("translatedName").isNull()
                    ? tag.get("translatedName").asText()
                    : tag.get("name").asText();
                tags.add(tagName);
            });
        }
        problem.setTags(tags);

        // 解析示例测试用例（如果有）
        if (question.has("exampleTestcases") && !question.get("exampleTestcases").isNull()) {
            String exampleTestcases = question.get("exampleTestcases").asText();
            List<TestCaseDTO> testCases = parseExampleTestcases(exampleTestcases);
            problem.setExampleTestCases(testCases);
        }

        // 解析代码模板（codeSnippets）
        if (question.has("codeSnippets") && !question.get("codeSnippets").isNull()) {
            problem.setCodeSnippets(question.get("codeSnippets").toString());
        }

        // 解析函数签名元数据（metaData）
        if (question.has("metaData") && !question.get("metaData").isNull()) {
            problem.setMetaData(question.get("metaData").asText());
        }

        return problem;
    }

    /**
     * 解析示例测试用例字符串
     * LeetCode 的 exampleTestcases 格式为换行分隔的输入输出对
     */
    private List<TestCaseDTO> parseExampleTestcases(String exampleTestcases) {
        List<TestCaseDTO> testCases = new ArrayList<>();

        if (exampleTestcases == null || exampleTestcases.trim().isEmpty()) {
            return testCases;
        }

        try {
            String[] lines = exampleTestcases.split("\n");
            // 假设输入输出成对出现
            for (int i = 0; i < lines.length - 1; i += 2) {
                TestCaseDTO testCase = new TestCaseDTO();
                testCase.setInput(lines[i].trim());
                testCase.setExpectedOutput(lines[i + 1].trim());
                testCases.add(testCase);
            }
        } catch (Exception e) {
            log.warn("解析示例测试用例失败: {}", e.getMessage());
        }

        return testCases;
    }
}
