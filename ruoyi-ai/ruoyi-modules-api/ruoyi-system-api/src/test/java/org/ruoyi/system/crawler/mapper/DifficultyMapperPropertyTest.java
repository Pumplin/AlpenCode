package org.ruoyi.system.crawler.mapper;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * DifficultyMapper 属性测试
 * <p>
 * 使用 jqwik 进行属性测试，验证难度映射的双射性（bijection）
 * </p>
 *
 * @author AlpenCode
 */
class DifficultyMapperPropertyTest {

    private final DifficultyMapper difficultyMapper = new DifficultyMapper();

    /**
     * 属性 6：难度映射双射性
     * <p>
     * **Validates: Requirements 3.1**
     * </p>
     * <p>
     * 验证难度映射是双射的：对于任意有效的难度值，映射后再反向映射应该得到原始值。
     * 这确保了 DifficultyMapper 的正确性和一致性。
     * </p>
     * <p>
     * 测试策略：
     * <ul>
     *   <li>生成随机的 LeetCode 难度字符串（Easy/Medium/Hard，包含不同大小写和空格）</li>
     *   <li>将难度字符串映射为 AlpenCode 难度值（1/2/3）</li>
     *   <li>将难度值反向映射回 LeetCode 难度字符串</li>
     *   <li>验证反向映射的结果与原始输入语义等价（忽略大小写和空格）</li>
     * </ul>
     * </p>
     */
    @Property(tries = 100)
    void difficultyMappingBijection(@ForAll("validDifficulties") String leetcodeDifficulty) {
        // Given: 一个有效的 LeetCode 难度字符串
        
        // When: 映射为 AlpenCode 难度值
        Integer alpencodeDifficulty = difficultyMapper.mapDifficulty(leetcodeDifficulty);
        
        // Then: 难度值应该在有效范围内（1/2/3）
        assertThat(alpencodeDifficulty)
            .isNotNull()
            .isBetween(1, 3);
        
        // When: 反向映射回 LeetCode 难度字符串
        String reversedDifficulty = difficultyMapper.reverseDifficulty(alpencodeDifficulty);
        
        // Then: 反向映射的结果应该与原始输入语义等价（忽略大小写和空格）
        assertThat(reversedDifficulty.toLowerCase())
            .isEqualTo(leetcodeDifficulty.trim().toLowerCase());
    }

    /**
     * 生成有效的 LeetCode 难度字符串
     * <p>
     * 生成策略：
     * <ul>
     *   <li>从 ["Easy", "Medium", "Hard"] 中随机选择一个</li>
     *   <li>随机应用大小写变换（全小写、全大写、首字母大写）</li>
     *   <li>随机添加前后空格</li>
     * </ul>
     * </p>
     */
    @Provide
    Arbitrary<String> validDifficulties() {
        Arbitrary<String> baseDifficulty = Arbitraries.of("Easy", "Medium", "Hard");
        
        Arbitrary<String> withCaseVariation = baseDifficulty.flatMap(difficulty ->
            Arbitraries.of(
                difficulty.toLowerCase(),           // 全小写：easy, medium, hard
                difficulty.toUpperCase(),           // 全大写：EASY, MEDIUM, HARD
                difficulty                          // 首字母大写：Easy, Medium, Hard
            )
        );
        
        Arbitrary<String> withWhitespace = withCaseVariation.flatMap(difficulty ->
            Arbitraries.of(
                difficulty,                         // 无空格
                " " + difficulty,                   // 前置空格
                difficulty + " ",                   // 后置空格
                " " + difficulty + " "              // 前后空格
            )
        );
        
        return withWhitespace;
    }
}
