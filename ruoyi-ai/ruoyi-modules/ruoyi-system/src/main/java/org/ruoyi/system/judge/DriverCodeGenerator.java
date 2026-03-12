package org.ruoyi.system.judge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Driver 代码生成器
 * Driver 从 stdin 循环读取多组测试用例，每组输出一行结果后打印 "---CASE_END---"
 * 这样一个容器可以跑完所有测试用例，避免每个用例单独起容器的开销。
 *
 * stdin 格式（每组用例之间用空行分隔，最后一行为 "---INPUT_END---"）：
 *   param1_json\nparam2_json\n\nparam1_json\nparam2_json\n\n---INPUT_END---
 */
@Slf4j
public class DriverCodeGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 用例输入结束标记 */
    public static final String INPUT_END = "---INPUT_END---";
    /** 单个用例输出结束标记 */
    public static final String CASE_END = "---CASE_END---";

    public static String generate(String metaData, String language) {
        try {
            JsonNode meta = MAPPER.readTree(metaData);
            return switch (language.toLowerCase()) {
                case "java" -> generateJavaDriver(meta);
                case "python3", "python" -> generatePythonDriver(meta);
                default -> throw new IllegalArgumentException("不支持的语言: " + language);
            };
        } catch (Exception e) {
            log.error("Driver 代码生成失败", e);
            throw new RuntimeException("Driver 代码生成失败: " + e.getMessage());
        }
    }

    private static String generateJavaDriver(JsonNode meta) {
        String funcName = meta.get("name").asText();
        JsonNode params = meta.get("params");
        JsonNode returnType = meta.get("return");
        int paramCount = params.size();

        List<String> paramNames = new ArrayList<>();
        List<String> paramJavaTypes = new ArrayList<>();
        List<String> paramGsonTypes = new ArrayList<>();
        for (JsonNode param : params) {
            paramNames.add(param.get("name").asText());
            paramJavaTypes.add(toJavaType(param.get("type").asText()));
            paramGsonTypes.add(toGsonDeserializeType(param.get("type").asText()));
        }

        String retJavaType = toJavaType(returnType.get("type").asText());

        StringBuilder sb = new StringBuilder();
        sb.append("import java.util.*;\n");
        sb.append("import com.google.gson.*;\n\n");
        sb.append("public class Main {\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        Scanner sc = new Scanner(System.in);\n");
        sb.append("        Gson gson = new Gson();\n");
        sb.append("        Solution solution = new Solution();\n\n");
        sb.append("        while (sc.hasNextLine()) {\n");
        sb.append("            String firstLine = sc.nextLine();\n");
        sb.append("            if (\"").append(INPUT_END).append("\".equals(firstLine)) break;\n\n");

        // 读取第一个参数（已读了 firstLine）
        if (paramCount > 0) {
            sb.append("            ").append(paramJavaTypes.get(0)).append(" ").append(paramNames.get(0))
              .append(" = gson.fromJson(firstLine, ").append(paramGsonTypes.get(0)).append(");\n");
            // 读取剩余参数
            for (int i = 1; i < paramCount; i++) {
                sb.append("            ").append(paramJavaTypes.get(i)).append(" ").append(paramNames.get(i))
                  .append(" = gson.fromJson(sc.nextLine(), ").append(paramGsonTypes.get(i)).append(");\n");
            }
        }

        sb.append("\n            try {\n");
        if ("void".equals(returnType.get("type").asText())) {
            sb.append("                solution.").append(funcName).append("(")
              .append(String.join(", ", paramNames)).append(");\n");
            sb.append("                System.out.println(\"null\");\n");
        } else {
            sb.append("                ").append(retJavaType).append(" result = solution.").append(funcName)
              .append("(").append(String.join(", ", paramNames)).append(");\n");
            sb.append("                System.out.println(gson.toJson(result));\n");
        }
        sb.append("            } catch (Exception e) {\n");
        sb.append("                System.err.println(e.getMessage());\n");
        sb.append("            }\n");
        sb.append("            System.out.println(\"").append(CASE_END).append("\");\n");
        // 消耗用例之间的空行
        sb.append("            if (sc.hasNextLine()) sc.nextLine();\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    private static String generatePythonDriver(JsonNode meta) {
        String funcName = meta.get("name").asText();
        JsonNode params = meta.get("params");
        int paramCount = params.size();

        List<String> paramNames = new ArrayList<>();
        for (JsonNode param : params) {
            paramNames.add(param.get("name").asText());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("import json, sys\n\n");
        sb.append("def main():\n");
        sb.append("    solution = Solution()\n");
        sb.append("    lines = sys.stdin.read().splitlines()\n");
        sb.append("    i = 0\n");
        sb.append("    while i < len(lines):\n");
        sb.append("        if lines[i] == '").append(INPUT_END).append("':\n");
        sb.append("            break\n");
        // 读取各参数
        for (int p = 0; p < paramCount; p++) {
            sb.append("        ").append(paramNames.get(p)).append(" = json.loads(lines[i + ").append(p).append("])\n");
        }
        sb.append("        try:\n");
        sb.append("            result = solution.").append(funcName).append("(")
          .append(String.join(", ", paramNames)).append(")\n");
        sb.append("            print(json.dumps(result))\n");
        sb.append("        except Exception as e:\n");
        sb.append("            print(str(e), file=sys.stderr)\n");
        sb.append("        print('").append(CASE_END).append("')\n");
        // 跳过参数行 + 空行
        sb.append("        i += ").append(paramCount + 1).append("\n");
        sb.append("\n");
        sb.append("if __name__ == '__main__':\n");
        sb.append("    main()\n");

        return sb.toString();
    }

    private static String toJavaType(String lcType) {
        return switch (lcType) {
            case "integer" -> "int";
            case "long" -> "long";
            case "double" -> "double";
            case "boolean" -> "boolean";
            case "string" -> "String";
            case "integer[]" -> "int[]";
            case "long[]" -> "long[]";
            case "double[]" -> "double[]";
            case "string[]" -> "String[]";
            case "integer[][]" -> "int[][]";
            case "string[][]" -> "String[][]";
            case "character" -> "char";
            case "character[]" -> "char[]";
            case "void" -> "void";
            default -> "Object";
        };
    }

    private static String toGsonDeserializeType(String lcType) {
        return switch (lcType) {
            case "integer" -> "int.class";
            case "long" -> "long.class";
            case "double" -> "double.class";
            case "boolean" -> "boolean.class";
            case "string" -> "String.class";
            case "integer[]" -> "int[].class";
            case "long[]" -> "long[].class";
            case "double[]" -> "double[].class";
            case "string[]" -> "String[].class";
            case "integer[][]" -> "int[][].class";
            case "string[][]" -> "String[][].class";
            case "character" -> "char.class";
            case "character[]" -> "char[].class";
            default -> "Object.class";
        };
    }

    /**
     * 生成用户代码模板（只含 Solution 类的函数签名，供前端编辑器初始化）
     */
    public static String generateTemplate(String metaData, String language) {
        try {
            JsonNode meta = MAPPER.readTree(metaData);
            return switch (language.toLowerCase()) {
                case "java" -> generateJavaTemplate(meta);
                case "python3", "python" -> generatePythonTemplate(meta);
                default -> "";
            };
        } catch (Exception e) {
            log.warn("模板生成失败，使用空模板: {}", e.getMessage());
            return "";
        }
    }

    private static String generateJavaTemplate(JsonNode meta) {
        String funcName = meta.get("name").asText();
        JsonNode params = meta.get("params");
        String retType = toJavaType(meta.get("return").get("type").asText());

        List<String> paramDecls = new ArrayList<>();
        for (JsonNode param : params) {
            paramDecls.add(toJavaType(param.get("type").asText()) + " " + param.get("name").asText());
        }

        return "class Solution {\n" +
               "    public " + retType + " " + funcName + "(" + String.join(", ", paramDecls) + ") {\n" +
               "        // 在这里写你的代码\n" +
               "    }\n" +
               "}\n";
    }

    private static String generatePythonTemplate(JsonNode meta) {
        String funcName = meta.get("name").asText();
        JsonNode params = meta.get("params");

        List<String> paramNames = new ArrayList<>();
        paramNames.add("self");
        for (JsonNode param : params) {
            paramNames.add(param.get("name").asText());
        }

        return "class Solution:\n" +
               "    def " + funcName + "(" + String.join(", ", paramNames) + "):\n" +
               "        # 在这里写你的代码\n" +
               "        pass\n";
    }
}
