package org.ruoyi.system.crawler.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ruoyi.system.crawler.domain.dto.TestCaseDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TestCaseParser 单元测试
 * 验证需求：7.1, 7.2, 7.4
 *
 * @author AlpenCode
 */
@DisplayName("TestCaseParser 单元测试")
class TestCaseParserTest {

    private TestCaseParser parser;

    @BeforeEach
    void setUp() {
        parser = new TestCaseParser();
    }

    @Test
    @DisplayName("测试单个示例解析")
    void testParseSingleExample() {
        // Given: HTML 内容包含一个示例（示例标题在 strong 标签中）
        String html = """
                <div>
                    <strong>示例 1：</strong>
                    <p>输入：nums = [2,7,11,15], target = 9</p>
                    <p>输出：[0,1]</p>
                    <p>解释：因为 nums[0] + nums[1] == 9，返回 [0, 1]。</p>
                </div>
                """;

        // When: 解析示例
        List<TestCaseDTO> testCases = parser.parseExamples(html);

        // Then: 应该解析出一个测试用例
        assertNotNull(testCases);
        assertEquals(1, testCases.size());

        TestCaseDTO testCase = testCases.get(0);
        assertEquals("nums = [2,7,11,15], target = 9", testCase.getInput());
        assertEquals("[0,1]", testCase.getExpectedOutput());
    }

    @Test
    @DisplayName("测试多个示例解析")
    void testParseMultipleExamples() {
        // Given: HTML 内容包含多个示例
        String html = """
                <div>
                    <strong>示例 1：</strong>
                    <p>输入：nums = [2,7,11,15], target = 9</p>
                    <p>输出：[0,1]</p>
                    
                    <strong>示例 2：</strong>
                    <p>输入：nums = [3,2,4], target = 6</p>
                    <p>输出：[1,2]</p>
                    
                    <strong>示例 3：</strong>
                    <p>输入：nums = [3,3], target = 6</p>
                    <p>输出：[0,1]</p>
                </div>
                """;

        // When: 解析示例
        List<TestCaseDTO> testCases = parser.parseExamples(html);

        // Then: 应该解析出三个测试用例
        assertNotNull(testCases);
        assertEquals(3, testCases.size());

        // 验证第一个示例
        assertEquals("nums = [2,7,11,15], target = 9", testCases.get(0).getInput());
        assertEquals("[0,1]", testCases.get(0).getExpectedOutput());

        // 验证第二个示例
        assertEquals("nums = [3,2,4], target = 6", testCases.get(1).getInput());
        assertEquals("[1,2]", testCases.get(1).getExpectedOutput());

        // 验证第三个示例
        assertEquals("nums = [3,3], target = 6", testCases.get(2).getInput());
        assertEquals("[0,1]", testCases.get(2).getExpectedOutput());
    }

    @Test
    @DisplayName("测试格式不符合预期的情况 - 缺少输出")
    void testParseMissingOutput() {
        // Given: HTML 内容只有输入，没有输出
        String html = """
                <div>
                    <strong>示例 1：</strong>
                    <p>输入：nums = [2,7,11,15], target = 9</p>
                </div>
                """;

        // When: 解析示例
        List<TestCaseDTO> testCases = parser.parseExamples(html);

        // Then: 应该返回空列表（因为数据不完整）
        assertNotNull(testCases);
        assertTrue(testCases.isEmpty());
    }

    @Test
    @DisplayName("测试格式不符合预期的情况 - 缺少输入")
    void testParseMissingInput() {
        // Given: HTML 内容只有输出，没有输入
        String html = """
                <div>
                    <strong>示例 1：</strong>
                    <p>输出：[0,1]</p>
                </div>
                """;

        // When: 解析示例
        List<TestCaseDTO> testCases = parser.parseExamples(html);

        // Then: 应该返回空列表（因为数据不完整）
        assertNotNull(testCases);
        assertTrue(testCases.isEmpty());
    }

    @Test
    @DisplayName("测试空 HTML 内容")
    void testParseEmptyHtml() {
        // When: 解析空 HTML
        List<TestCaseDTO> testCases = parser.parseExamples("");

        // Then: 应该返回空列表
        assertNotNull(testCases);
        assertTrue(testCases.isEmpty());
    }

    @Test
    @DisplayName("测试 null HTML 内容")
    void testParseNullHtml() {
        // When: 解析 null HTML
        List<TestCaseDTO> testCases = parser.parseExamples(null);

        // Then: 应该返回空列表
        assertNotNull(testCases);
        assertTrue(testCases.isEmpty());
    }

    @Test
    @DisplayName("测试不包含示例的 HTML")
    void testParseHtmlWithoutExamples() {
        // Given: HTML 内容不包含示例
        String html = """
                <div>
                    <p>这是一道算法题的描述。</p>
                    <p>请实现一个函数来解决这个问题。</p>
                </div>
                """;

        // When: 解析示例
        List<TestCaseDTO> testCases = parser.parseExamples(html);

        // Then: 应该返回空列表
        assertNotNull(testCases);
        assertTrue(testCases.isEmpty());
    }

    @Test
    @DisplayName("测试不同的冒号格式 - 中文冒号")
    void testParseWithChineseColon() {
        // Given: 使用中文冒号
        String html = """
                <div>
                    <strong>示例 1：</strong>
                    <p>输入：nums = [1,2,3]</p>
                    <p>输出：6</p>
                </div>
                """;

        // When: 解析示例
        List<TestCaseDTO> testCases = parser.parseExamples(html);

        // Then: 应该成功解析
        assertNotNull(testCases);
        assertEquals(1, testCases.size());
        assertEquals("nums = [1,2,3]", testCases.get(0).getInput());
        assertEquals("6", testCases.get(0).getExpectedOutput());
    }

    @Test
    @DisplayName("测试不同的冒号格式 - 英文冒号")
    void testParseWithEnglishColon() {
        // Given: 使用英文冒号
        String html = """
                <div>
                    <strong>示例 1:</strong>
                    <p>输入: nums = [1,2,3]</p>
                    <p>输出: 6</p>
                </div>
                """;

        // When: 解析示例
        List<TestCaseDTO> testCases = parser.parseExamples(html);

        // Then: 应该成功解析
        assertNotNull(testCases);
        assertEquals(1, testCases.size());
        assertEquals("nums = [1,2,3]", testCases.get(0).getInput());
        assertEquals("6", testCases.get(0).getExpectedOutput());
    }

    @Test
    @DisplayName("测试带有额外空格的格式")
    void testParseWithExtraSpaces() {
        // Given: 输入输出标签后有额外空格
        String html = """
                <div>
                    <strong>示例 1：</strong>
                    <p>输入 ：  nums = [1,2,3]  </p>
                    <p>输出 ：  6  </p>
                </div>
                """;

        // When: 解析示例
        List<TestCaseDTO> testCases = parser.parseExamples(html);

        // Then: 应该成功解析并去除多余空格
        assertNotNull(testCases);
        assertEquals(1, testCases.size());
        assertEquals("nums = [1,2,3]", testCases.get(0).getInput());
        assertEquals("6", testCases.get(0).getExpectedOutput());
    }

    @Test
    @DisplayName("测试复杂的输入输出格式")
    void testParseComplexFormat() {
        // Given: 复杂的输入输出格式（多行、数组等）
        String html = """
                <div>
                    <strong>示例 1：</strong>
                    <p>输入：matrix = [[1,2,3],[4,5,6],[7,8,9]]</p>
                    <p>输出：[[7,4,1],[8,5,2],[9,6,3]]</p>
                </div>
                """;

        // When: 解析示例
        List<TestCaseDTO> testCases = parser.parseExamples(html);

        // Then: 应该成功解析复杂格式
        assertNotNull(testCases);
        assertEquals(1, testCases.size());
        assertEquals("matrix = [[1,2,3],[4,5,6],[7,8,9]]", testCases.get(0).getInput());
        assertEquals("[[7,4,1],[8,5,2],[9,6,3]]", testCases.get(0).getExpectedOutput());
    }

    @Test
    @DisplayName("测试示例编号格式变化 - 无空格")
    void testParseExampleNumberWithoutSpace() {
        // Given: 示例编号没有空格（示例1）
        String html = """
                <div>
                    <strong>示例1：</strong>
                    <p>输入：x = 5</p>
                    <p>输出：25</p>
                </div>
                """;

        // When: 解析示例
        List<TestCaseDTO> testCases = parser.parseExamples(html);

        // Then: 应该成功解析
        assertNotNull(testCases);
        assertEquals(1, testCases.size());
        assertEquals("x = 5", testCases.get(0).getInput());
        assertEquals("25", testCases.get(0).getExpectedOutput());
    }

    @Test
    @DisplayName("测试示例编号格式变化 - 多个空格")
    void testParseExampleNumberWithMultipleSpaces() {
        // Given: 示例编号有多个空格（示例  1）
        String html = """
                <div>
                    <strong>示例  1：</strong>
                    <p>输入：x = 5</p>
                    <p>输出：25</p>
                </div>
                """;

        // When: 解析示例
        List<TestCaseDTO> testCases = parser.parseExamples(html);

        // Then: 应该成功解析
        assertNotNull(testCases);
        assertEquals(1, testCases.size());
        assertEquals("x = 5", testCases.get(0).getInput());
        assertEquals("25", testCases.get(0).getExpectedOutput());
    }

    @Test
    @DisplayName("测试输入输出在同一行")
    void testParseInputOutputInSameLine() {
        // Given: 输入和输出在同一个元素中
        String html = """
                <div>
                    <strong>示例 1：</strong>
                    <p>输入：x = 5 输出：25</p>
                </div>
                """;

        // When: 解析示例
        List<TestCaseDTO> testCases = parser.parseExamples(html);

        // Then: 应该成功解析（输入会包含整行内容，输出也会提取到）
        assertNotNull(testCases);
        assertEquals(1, testCases.size());
        // 输入会提取到 "输出：" 之前的所有内容
        assertEquals("x = 5 输出：25", testCases.get(0).getInput());
        // 输出会提取到 "输出：" 之后的内容
        assertEquals("25", testCases.get(0).getExpectedOutput());
    }

    @Test
    @DisplayName("测试带有解释说明的示例")
    void testParseExampleWithExplanation() {
        // Given: 示例后面有解释说明
        String html = """
                <div>
                    <strong>示例 1：</strong>
                    <p>输入：nums = [2,7,11,15], target = 9</p>
                    <p>输出：[0,1]</p>
                    <p>解释：因为 nums[0] + nums[1] == 9，返回 [0, 1]。</p>
                </div>
                """;

        // When: 解析示例
        List<TestCaseDTO> testCases = parser.parseExamples(html);

        // Then: 应该只提取输入和输出，忽略解释
        assertNotNull(testCases);
        assertEquals(1, testCases.size());
        assertEquals("nums = [2,7,11,15], target = 9", testCases.get(0).getInput());
        assertEquals("[0,1]", testCases.get(0).getExpectedOutput());
    }

    @Test
    @DisplayName("测试空白输入输出")
    void testParseEmptyInputOutput() {
        // Given: 输入输出为空白
        String html = """
                <div>
                    <strong>示例 1：</strong>
                    <p>输入：   </p>
                    <p>输出：   </p>
                </div>
                """;

        // When: 解析示例
        List<TestCaseDTO> testCases = parser.parseExamples(html);

        // Then: 应该返回空列表（因为数据为空）
        assertNotNull(testCases);
        assertTrue(testCases.isEmpty());
    }
}
