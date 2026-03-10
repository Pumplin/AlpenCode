package org.ruoyi.system.crawler.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.ruoyi.system.crawler.domain.dto.TestCaseDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 测试用例解析器
 * 从 LeetCode 题目描述的 HTML 内容中提取示例测试用例
 *
 * @author AlpenCode
 */
@Component
@Slf4j
public class TestCaseParser {

    /**
     * 匹配示例标题的正则表达式（示例 1、示例 2 等）
     */
    private static final Pattern EXAMPLE_PATTERN = Pattern.compile("示例\\s*\\d+");

    /**
     * 从 HTML 内容中解析示例测试用例
     * <p>
     * 解析规则：
     * 1. 查找 "示例 1"、"示例 2" 等标题
     * 2. 提取 "输入：" 后的内容作为 input
     * 3. 提取 "输出：" 后的内容作为 expectedOutput
     * 4. 支持多组示例
     * </p>
     *
     * @param htmlContent HTML 格式的题目描述
     * @return 解析出的测试用例列表
     */
    public List<TestCaseDTO> parseExamples(String htmlContent) {
        List<TestCaseDTO> testCases = new ArrayList<>();

        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            log.warn("HTML 内容为空，无法解析示例测试用例");
            return testCases;
        }

        try {
            // 使用 Jsoup 解析 HTML
            Document doc = Jsoup.parse(htmlContent);

            // 查找所有包含"示例"的段落或标题元素
            Elements allElements = doc.getAllElements();

            for (Element element : allElements) {
                String text = element.ownText();

                // 检查是否是示例标题
                if (EXAMPLE_PATTERN.matcher(text).find()) {
                    try {
                        TestCaseDTO testCase = extractTestCase(element);
                        if (testCase != null) {
                            testCases.add(testCase);
                            log.debug("成功解析示例: 输入={}, 输出={}",
                                    testCase.getInput(), testCase.getExpectedOutput());
                        }
                    } catch (Exception e) {
                        log.warn("解析示例失败: {}", e.getMessage());
                    }
                }
            }

            log.info("成功解析 {} 个示例测试用例", testCases.size());

        } catch (Exception e) {
            log.error("解析 HTML 内容失败", e);
        }

        return testCases;
    }

    /**
     * 从示例元素中提取测试用例
     *
     * @param exampleElement 示例标题元素
     * @return 测试用例 DTO，如果提取失败则返回 null
     */
    private TestCaseDTO extractTestCase(Element exampleElement) {
        // 获取示例标题后的所有兄弟元素
        Elements siblings = exampleElement.parent().children();
        int startIndex = siblings.indexOf(exampleElement);

        String input = null;
        String output = null;

        // 遍历后续元素，查找输入和输出
        for (int i = startIndex + 1; i < siblings.size(); i++) {
            Element sibling = siblings.get(i);
            String text = sibling.text();

            // 如果遇到下一个示例标题，停止查找
            if (EXAMPLE_PATTERN.matcher(text).find()) {
                break;
            }

            // 提取输入
            if (input == null && text.contains("输入")) {
                input = extractContent(text, "输入");
            }

            // 提取输出
            if (output == null && text.contains("输出")) {
                output = extractContent(text, "输出");
            }

            // 如果已经找到输入和输出，可以提前结束
            if (input != null && output != null) {
                break;
            }
        }

        // 验证是否成功提取输入和输出
        if (input == null || output == null || input.trim().isEmpty() || output.trim().isEmpty()) {
            log.warn("示例数据不完整，跳过");
            return null;
        }

        TestCaseDTO testCase = new TestCaseDTO();
        testCase.setInput(input.trim());
        testCase.setExpectedOutput(output.trim());
        return testCase;
    }

    /**
     * 从文本中提取指定标签后的内容
     *
     * @param text  包含标签的文本
     * @param label 标签名称（如"输入"、"输出"）
     * @return 提取的内容，如果未找到则返回 null
     */
    private String extractContent(String text, String label) {
        try {
            // 查找标签位置（支持"输入："、"输入:"、"输入 ："等格式）
            Pattern pattern = Pattern.compile(label + "\\s*[：:]\\s*");
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                // 提取标签后的内容
                String content = text.substring(matcher.end()).trim();

                // 移除可能的代码块标记
                content = content.replaceAll("^```.*?\\n", "");
                content = content.replaceAll("\\n```$", "");

                return content;
            }
        } catch (Exception e) {
            log.warn("提取内容失败: label={}, error={}", label, e.getMessage());
        }

        return null;
    }
}
