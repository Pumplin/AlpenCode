package org.ruoyi.system.crawler.mapper;

import org.springframework.stereotype.Component;

/**
 * 难度映射器
 * <p>
 * 将 LeetCode 的难度标识（Easy/Medium/Hard）转换为 AlpenCode 的难度值（1/2/3）
 * </p>
 *
 * @author AlpenCode
 */
@Component
public class DifficultyMapper {

    /**
     * 将 LeetCode 难度映射为 AlpenCode 难度值
     * <p>
     * 映射规则：
     * <ul>
     *   <li>Easy -> 1</li>
     *   <li>Medium -> 2</li>
     *   <li>Hard -> 3</li>
     * </ul>
     * </p>
     *
     * @param leetcodeDifficulty LeetCode 难度值（Easy/Medium/Hard，不区分大小写）
     * @return AlpenCode 难度值（1/2/3）
     * @throws IllegalArgumentException 如果难度值未知
     */
    public Integer mapDifficulty(String leetcodeDifficulty) {
        if (leetcodeDifficulty == null || leetcodeDifficulty.trim().isEmpty()) {
            throw new IllegalArgumentException("难度值不能为空");
        }

        return switch (leetcodeDifficulty.toLowerCase().trim()) {
            case "easy" -> 1;
            case "medium" -> 2;
            case "hard" -> 3;
            default -> throw new IllegalArgumentException(
                "未知的难度值: " + leetcodeDifficulty + "，有效值为: Easy, Medium, Hard");
        };
    }

    /**
     * 将 AlpenCode 难度值反向映射为 LeetCode 难度标识
     * <p>
     * 此方法用于属性测试的双射验证，确保映射关系的正确性
     * </p>
     * <p>
     * 映射规则：
     * <ul>
     *   <li>1 -> Easy</li>
     *   <li>2 -> Medium</li>
     *   <li>3 -> Hard</li>
     * </ul>
     * </p>
     *
     * @param difficulty AlpenCode 难度值（1/2/3）
     * @return LeetCode 难度标识（Easy/Medium/Hard）
     * @throws IllegalArgumentException 如果难度值未知
     */
    public String reverseDifficulty(Integer difficulty) {
        if (difficulty == null) {
            throw new IllegalArgumentException("难度值不能为空");
        }

        return switch (difficulty) {
            case 1 -> "Easy";
            case 2 -> "Medium";
            case 3 -> "Hard";
            default -> throw new IllegalArgumentException(
                "未知的难度值: " + difficulty + "，有效值为: 1, 2, 3");
        };
    }
}
