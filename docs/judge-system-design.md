# AlpenCode 判题系统设计 — 函数调用模式

## 1. 背景

AlpenCode 采用 LeetCode 风格的函数调用模式进行判题，用户只需编写核心算法函数，不需要处理输入输出。

### 两种 OJ 判题模式对比

| | 标准 IO 模式（POJ/洛谷） | 函数调用模式（LeetCode） |
|---|---|---|
| 用户写什么 | 完整程序，自己读 stdin、写 stdout | 只写一个函数体 |
| IO 处理 | 用户负责 | 系统负责（Driver 程序） |
| 用户体验 | 差，要写大量 IO 代码 | 好，专注算法 |
| 实现复杂度 | 简单 | 需要 Driver 代码生成 |

AlpenCode 选择函数调用模式，因为：
- 用户体验更好，和 LeetCode 一致
- 从 LeetCode 爬取的 `metaData` 和 `codeSnippets` 可以直接复用
- 作为 AI 辅助编程训练平台，降低用户门槛很重要

## 2. 核心概念

### 2.1 Code Snippets（代码模板）

每道题在每种语言下都有一个代码模板，用户在编辑器中看到的就是这个模板。

Java 示例（Two Sum）：
```java
class Solution {
    public int[] twoSum(int[] nums, int target) {

    }
}
```

Python 示例：
```python
class Solution:
    def twoSum(self, nums: List[int], target: int) -> List[int]:

```

这些模板从 LeetCode GraphQL API 的 `codeSnippets` 字段获取，存储在 `ac_problem.code_snippets` 中，格式为 JSON：
```json
[
  {"lang": "Java", "langSlug": "java", "code": "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n\n    }\n}"},
  {"lang": "Python3", "langSlug": "python3", "code": "class Solution:\n    def twoSum(self, nums: List[int], target: int) -> List[int]:\n"}
]
```

### 2.2 MetaData（函数签名元数据）

`metaData` 是 LeetCode 提供的 JSON，描述了函数的签名信息，是 Driver 代码生成的关键依据。

示例（Two Sum）：
```json
{
  "name": "twoSum",
  "params": [
    {"name": "nums", "type": "integer[]"},
    {"name": "target", "type": "integer"}
  ],
  "return": {"type": "integer[]"}
}
```

存储在 `ac_problem.meta_data` 字段中。

### 2.3 Driver 程序

Driver 是系统自动生成的包装代码，负责：
1. 从 stdin 读取测试用例输入（JSON 格式）
2. 将 JSON 反序列化为函数参数
3. 调用用户编写的 Solution 类的方法
4. 将返回值序列化为 JSON 输出到 stdout

判题时，系统将 Driver 代码和用户代码拼接在一起编译运行。

## 3. 判题流程

```
用户提交代码（只有 Solution 类）
        ↓
后端根据 metaData + 语言 生成 Driver 代码
        ↓
拼接：用户代码 + Driver 代码 → 完整可执行程序
        ↓
Docker 沙箱中编译运行
        ↓
对每个测试用例：
  stdin 输入 JSON 格式的参数 → 程序执行 → stdout 输出结果
        ↓
对比 stdout 和 expected_output → 判定 AC/WA
```

## 4. 测试用例格式

测试用例的 input 和 expected_output 都使用 JSON 格式，每行一个参数值。

以 Two Sum 为例：
```
input:
[2,7,11,15]
9

expected_output:
[0,1]
```

input 中每行对应 metaData.params 中的一个参数（按顺序），expected_output 是函数返回值的 JSON 表示。

这和 LeetCode 的 `exampleTestcases` 格式完全一致。

## 5. Driver 代码生成

### 5.1 支持的类型映射

| LeetCode metaData 类型 | Java 类型 | Python 类型 |
|---|---|---|
| integer | int | int |
| long | long | int |
| double | double | float |
| boolean | boolean | bool |
| string | String | str |
| integer[] | int[] | List[int] |
| string[] | String[] | List[str] |
| integer[][] | int[][] | List[List[int]] |
| character | char | str |
| character[] | char[] | List[str] |
| ListNode | ListNode | ListNode |
| TreeNode | TreeNode | TreeNode |

### 5.2 Java Driver 示例

以 Two Sum 为例，系统生成的 Driver：

```java
import java.util.*;
import com.google.gson.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Gson gson = new Gson();

        // 读取参数（每行一个，JSON 格式）
        int[] nums = gson.fromJson(sc.nextLine(), int[].class);
        int target = gson.fromJson(sc.nextLine(), int.class);

        // 调用用户的 Solution
        Solution solution = new Solution();
        int[] result = solution.twoSum(nums, target);

        // 输出结果（JSON 格式）
        System.out.println(gson.toJson(result));
    }
}
```

### 5.3 Python Driver 示例

```python
import json, sys

def main():
    # 读取参数
    nums = json.loads(input())
    target = json.loads(input())

    # 调用用户的 Solution
    solution = Solution()
    result = solution.twoSum(nums, target)

    # 输出结果
    print(json.dumps(result))

if __name__ == "__main__":
    main()
```

### 5.4 特殊数据结构处理

对于 ListNode 和 TreeNode，需要在 Driver 中提供辅助类和序列化/反序列化方法：

ListNode：JSON 数组 `[1,2,3]` ↔ 链表 `1->2->3`
TreeNode：JSON 数组 `[1,2,3,null,null,4,5]` ↔ 二叉树（层序遍历）

这些辅助代码作为固定模板，在生成 Driver 时按需拼接。

## 6. 数据库变更

### ac_problem 表新增字段

```sql
ALTER TABLE ac_problem
  ADD COLUMN code_snippets TEXT COMMENT '各语言代码模板（JSON）' AFTER description,
  ADD COLUMN meta_data TEXT COMMENT '函数签名元数据（JSON）' AFTER code_snippets;
```

### 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| code_snippets | TEXT | LeetCode codeSnippets JSON，包含各语言的代码模板 |
| meta_data | TEXT | LeetCode metaData JSON，包含函数名、参数类型、返回类型 |

## 7. 爬虫变更

GraphQL 详情查询新增 `codeSnippets` 和 `metaData` 字段：

```graphql
query ($titleSlug: String!) {
  question(titleSlug: $titleSlug) {
    # ... 原有字段 ...
    codeSnippets {
      lang
      langSlug
      code
    }
    metaData
  }
}
```

## 8. 前端展示

1. 用户打开题目页面时，根据选择的语言从 `code_snippets` 中取出对应模板
2. Monaco Editor 预填代码模板
3. 用户在模板基础上编写算法
4. 提交时只提交用户代码（包含 Solution 类）

## 9. 实现优先级

1. **P0**：爬虫获取 codeSnippets + metaData 并存储
2. **P0**：Java/Python 的基础类型 Driver 生成（integer, string, array）
3. **P1**：ListNode/TreeNode 等特殊类型支持
4. **P1**：已有题目的 metaData 补充（管理端批量操作）
5. **P2**：AI 生成测试用例（依赖 AI 模块完成）
