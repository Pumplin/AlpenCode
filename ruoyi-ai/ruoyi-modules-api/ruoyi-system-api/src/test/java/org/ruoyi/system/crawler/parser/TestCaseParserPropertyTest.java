package org.ruoyi.system.crawler.parser;

import net.jqwik.api.*;
import org.ruoyi.system.crawler.domain.dto.TestCaseDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TestCaseParser 属性测试
 * 使用 jqwik 进行基于属性的测试
 * 
 * **验证需求：7.3**
 *
 * @author AlpenCode
 */
public class TestCaseParserPropertyTest {

    private final TestCaseParser parser = new TestCaseParser();

    /**
     * 属性 12：示例解析 Round-Trip
     * 
     * **验证需求：7.3**
     * 
     * 对于任何成功解析的示例测试用例，将其格式化输出后再解析应该得到语义等价的输入输出数据
     */
    @Property(tries = 100)
    @Label("Feature: leetcode-crawler, Property 12: 示例解析 Round-Trip")
    void testExampleParsingRoundTrip(@ForAll("testCases") TestCaseDTO originalTestCase) {
        
        // Given: 一个测试用例
        String input = originalTestCase.getInput();
        String output = originalTestCase.getExpectedOutput();
        
        // When: 格式化为 HTML，然后解析
        String html = formatTestCaseToHtml(originalTestCase);
        List<TestCaseDTO> parsedTestCases = parser.parseExamples(html);
        
        // Then: 应该成功解析出一个测试用例
        assertNotNull(parsedTestCases, "解析结果不应为 null");
        assertFalse(parsedTestCases.isEmpty(), "应该至少解析出一个测试用例");
        assertEquals(1, parsedTestCases.size(), "应该只解析出一个测试用例");
        
        // And: 解析出的测试用例应该与原始测试用例语义等价
        TestCaseDTO parsedTestCase = parsedTestCases.get(0);
        assertEquals(normalizeWhitespace(input), 
                normalizeWhitespace(parsedTestCase.getInput()),
                "输入应该相同");
        assertEquals(normalizeWhitespace(output), 
                normalizeWhitespace(parsedTestCase.getExpectedOutput()),
                "输出应该相同");
    }

    /**
     * 属性：多个示例的 Round-Trip
     * 
     * 验证多个示例的解析和格式化往返一致性
     */
    @Property(tries = 100)
    @Label("Feature: leetcode-crawler, Property: 多个示例 Round-Trip")
    void testMultipleExamplesRoundTrip(@ForAll("testCaseLists") List<TestCaseDTO> originalTestCases) {
        
        // Given: 多个测试用例
        if (originalTestCases.isEmpty()) {
            return; // 跳过空列表
        }
        
        // When: 格式化为 HTML，然后解析
        String html = formatMultipleTestCasesToHtml(originalTestCases);
        List<TestCaseDTO> parsedTestCases = parser.parseExamples(html);
        
        // Then: 解析出的测试用例数量应该相同
        assertEquals(originalTestCases.size(), parsedTestCases.size(),
                "解析出的测试用例数量应该相同");
        
        // And: 每个测试用例都应该语义等价
        for (int i = 0; i < originalTestCases.size(); i++) {
            TestCaseDTO original = originalTestCases.get(i);
            TestCaseDTO parsed = parsedTestCases.get(i);
            
            assertEquals(normalizeWhitespace(original.getInput()),
                    normalizeWhitespace(parsed.getInput()),
                    "第 " + (i + 1) + " 个示例的输入应该相同");
            assertEquals(normalizeWhitespace(original.getExpectedOutput()),
                    normalizeWhitespace(parsed.getExpectedOutput()),
                    "第 " + (i + 1) + " 个示例的输出应该相同");
        }
    }

    /**
     * 属性：解析结果的非空性
     * 
     * 对于任何有效的 HTML 格式，解析结果不应该为 null
     */
    @Property(tries = 100)
    @Label("Feature: leetcode-crawler, Property: 解析结果非空性")
    void testParseResultNotNull(@ForAll("testCases") TestCaseDTO testCase) {
        
        // Given: 一个格式化的 HTML
        String html = formatTestCaseToHtml(testCase);
        
        // When: 解析 HTML
        List<TestCaseDTO> result = parser.parseExamples(html);
        
        // Then: 结果不应该为 null
        assertNotNull(result, "解析结果不应为 null");
    }

    /**
     * 属性：输入输出的完整性
     * 
     * 对于任何成功解析的测试用例，输入和输出都不应该为空
     */
    @Property(tries = 100)
    @Label("Feature: leetcode-crawler, Property: 输入输出完整性")
    void testParsedTestCaseCompleteness(@ForAll("testCases") TestCaseDTO testCase) {
        
        // Given: 一个格式化的 HTML
        String html = formatTestCaseToHtml(testCase);
        
        // When: 解析 HTML
        List<TestCaseDTO> parsedTestCases = parser.parseExamples(html);
        
        // Then: 所有解析出的测试用例都应该有非空的输入和输出
        for (TestCaseDTO parsed : parsedTestCases) {
            assertNotNull(parsed.getInput(), "输入不应为 null");
            assertNotNull(parsed.getExpectedOutput(), "输出不应为 null");
            assertFalse(parsed.getInput().trim().isEmpty(), "输入不应为空");
            assertFalse(parsed.getExpectedOutput().trim().isEmpty(), "输出不应为空");
        }
    }

    /**
     * 将测试用例格式化为 HTML
     */
    private String formatTestCaseToHtml(TestCaseDTO testCase) {
        return String.format("""
                <div>
                    <strong>示例 1：</strong>
                    <p>输入：%s</p>
                    <p>输出：%s</p>
                </div>
                """, testCase.getInput(), testCase.getExpectedOutput());
    }

    /**
     * 将多个测试用例格式化为 HTML
     */
    private String formatMultipleTestCasesToHtml(List<TestCaseDTO> testCases) {
        StringBuilder html = new StringBuilder("<div>\n");
        for (int i = 0; i < testCases.size(); i++) {
            TestCaseDTO testCase = testCases.get(i);
            html.append(String.format("""
                        <strong>示例 %d：</strong>
                        <p>输入：%s</p>
                        <p>输出：%s</p>
                        
                    """, i + 1, testCase.getInput(), testCase.getExpectedOutput()));
        }
        html.append("</div>");
        return html.toString();
    }

    /**
     * 规范化空白字符（用于比较）
     */
    private String normalizeWhitespace(String text) {
        if (text == null) {
            return "";
        }
        // 移除首尾空白，并将多个空白字符替换为单个空格
        return text.trim().replaceAll("\\s+", " ");
    }

    /**
     * 测试用例生成器
     */
    @Provide
    Arbitrary<TestCaseDTO> testCases() {
        Arbitrary<String> inputs = Arbitraries.oneOf(
                Arbitraries.integers().between(-100, 100).list().ofSize(3)
                        .map(list -> String.format("nums = [%d,%d,%d]", list.get(0), list.get(1), list.get(2))),
                Arbitraries.integers().between(1, 100)
                        .map(n -> "target = " + n),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10)
                        .map(s -> "s = \"" + s + "\""),
                Arbitraries.integers().between(-10, 10).list().ofSize(4)
                        .map(list -> String.format("matrix = [[%d,%d],[%d,%d]]", 
                                list.get(0), list.get(1), list.get(2), list.get(3))),
                Arbitraries.integers().between(1, 100).tuple2()
                        .map(tuple -> String.format("x = %d, y = %d", tuple.get1(), tuple.get2())),
                Arbitraries.integers().between(-50, 50).list().ofSize(4)
                        .map(list -> String.format("arr = [%d,%d,%d,%d]", 
                                list.get(0), list.get(1), list.get(2), list.get(3))),
                Arbitraries.integers().between(1, 1000)
                        .map(n -> "n = " + n)
        );

        Arbitrary<String> outputs = Arbitraries.oneOf(
                Arbitraries.integers().between(0, 100).tuple2()
                        .map(tuple -> String.format("[%d,%d]", tuple.get1(), tuple.get2())),
                Arbitraries.integers().between(-100, 100)
                        .map(String::valueOf),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10)
                        .map(s -> "\"" + s + "\""),
                Arbitraries.integers().between(-10, 10).list().ofSize(4)
                        .map(list -> String.format("[[%d,%d],[%d,%d]]", 
                                list.get(0), list.get(1), list.get(2), list.get(3))),
                Arbitraries.of("true", "false", "null"),
                Arbitraries.integers().between(0, 100).list().ofSize(3)
                        .map(list -> String.format("[%d,%d,%d]", list.get(0), list.get(1), list.get(2)))
        );

        return Combinators.combine(inputs, outputs).as((input, output) -> {
            TestCaseDTO testCase = new TestCaseDTO();
            testCase.setInput(input);
            testCase.setExpectedOutput(output);
            return testCase;
        });
    }

    /**
     * 测试用例列表生成器
     */
    @Provide
    Arbitrary<List<TestCaseDTO>> testCaseLists() {
        return testCases().list().ofMinSize(1).ofMaxSize(5);
    }
}
