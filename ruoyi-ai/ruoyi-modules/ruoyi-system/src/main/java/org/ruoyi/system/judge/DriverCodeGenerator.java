package org.ruoyi.system.judge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Driver 代码生成器（纯 JDK，不依赖 Gson）
 * Driver 从 stdin 循环读取多组测试用例，每组输出一行结果后打印 "---CASE_END---"
 */
@Slf4j
public class DriverCodeGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String INPUT_END = "---INPUT_END---";
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
        List<String> parseExprs = new ArrayList<>();
        for (JsonNode param : params) {
            paramNames.add(param.get("name").asText());
            String lcType = param.get("type").asText();
            paramJavaTypes.add(toJavaType(lcType));
            parseExprs.add(toParseExpr(lcType));
        }

        String retJavaType = toJavaType(returnType.get("type").asText());
        String retLcType = returnType.get("type").asText();

        StringBuilder sb = new StringBuilder();
        sb.append("import java.util.*;\n\n");
        sb.append("public class Main {\n\n");

        // 内嵌 JSON 工具方法
        appendJsonUtils(sb);

        // main 方法
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        Scanner sc = new Scanner(System.in);\n");
        sb.append("        Solution solution = new Solution();\n\n");
        sb.append("        while (sc.hasNextLine()) {\n");
        sb.append("            String firstLine = sc.nextLine();\n");
        sb.append("            if (\"" + INPUT_END + "\".equals(firstLine)) break;\n\n");

        if (paramCount > 0) {
            sb.append("            " + paramJavaTypes.get(0) + " " + paramNames.get(0)
                    + " = " + parseExprs.get(0).replace("$L", "firstLine") + ";\n");
            for (int i = 1; i < paramCount; i++) {
                sb.append("            " + paramJavaTypes.get(i) + " " + paramNames.get(i)
                        + " = " + parseExprs.get(i).replace("$L", "sc.nextLine()") + ";\n");
            }
        }

        sb.append("\n            try {\n");
        if ("void".equals(retLcType)) {
            sb.append("                solution." + funcName + "("
                    + String.join(", ", paramNames) + ");\n");
            sb.append("                System.out.println(\"null\");\n");
        } else {
            sb.append("                " + retJavaType + " result = solution." + funcName
                    + "(" + String.join(", ", paramNames) + ");\n");
            sb.append("                System.out.println(toJson(result));\n");
        }
        sb.append("            } catch (Exception e) {\n");
        sb.append("                System.err.println(e.getMessage());\n");
        sb.append("            }\n");
        sb.append("            System.out.println(\"" + CASE_END + "\");\n");
        sb.append("            if (sc.hasNextLine()) sc.nextLine();\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * 向生成的 Main.java 中追加纯 JDK 的 JSON 解析/序列化工具方法
     */
    private static void appendJsonUtils(StringBuilder sb) {
        // splitJsonArray: 按顶层逗号拆分 JSON 数组
        sb.append("    static List<String> splitArr(String s) {\n");
        sb.append("        s = s.trim();\n");
        sb.append("        if (s.startsWith(\"[\")) s = s.substring(1, s.length()-1);\n");
        sb.append("        List<String> p = new ArrayList<>();\n");
        sb.append("        if (s.trim().isEmpty()) return p;\n");
        sb.append("        int d=0, st=0; boolean q=false;\n");
        sb.append("        for (int i=0; i<s.length(); i++) {\n");
        sb.append("            char c = s.charAt(i);\n");
        sb.append("            if (c=='\"' && (i==0||s.charAt(i-1)!='\\\\')) q=!q;\n");
        sb.append("            if (!q) {\n");
        sb.append("                if (c=='['||c=='{') d++;\n");
        sb.append("                else if (c==']'||c=='}') d--;\n");
        sb.append("                else if (c==',' && d==0) { p.add(s.substring(st,i).trim()); st=i+1; }\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        p.add(s.substring(st).trim());\n");
        sb.append("        return p;\n");
        sb.append("    }\n\n");

        // parseStr
        sb.append("    static String parseStr(String s) {\n");
        sb.append("        s = s.trim();\n");
        sb.append("        if (s.startsWith(\"\\\"\") && s.endsWith(\"\\\"\")) return s.substring(1,s.length()-1);\n");
        sb.append("        return s;\n");
        sb.append("    }\n\n");

        // parseInt1D
        sb.append("    static int[] parseInt1D(String s) {\n");
        sb.append("        List<String> p = splitArr(s);\n");
        sb.append("        if (p.isEmpty()||(p.size()==1&&p.get(0).isEmpty())) return new int[0];\n");
        sb.append("        int[] a = new int[p.size()];\n");
        sb.append("        for (int i=0;i<p.size();i++) a[i]=Integer.parseInt(p.get(i).trim());\n");
        sb.append("        return a;\n");
        sb.append("    }\n\n");

        // parseLong1D
        sb.append("    static long[] parseLong1D(String s) {\n");
        sb.append("        List<String> p = splitArr(s);\n");
        sb.append("        if (p.isEmpty()||(p.size()==1&&p.get(0).isEmpty())) return new long[0];\n");
        sb.append("        long[] a = new long[p.size()];\n");
        sb.append("        for (int i=0;i<p.size();i++) a[i]=Long.parseLong(p.get(i).trim());\n");
        sb.append("        return a;\n");
        sb.append("    }\n\n");

        // parseDouble1D
        sb.append("    static double[] parseDouble1D(String s) {\n");
        sb.append("        List<String> p = splitArr(s);\n");
        sb.append("        if (p.isEmpty()||(p.size()==1&&p.get(0).isEmpty())) return new double[0];\n");
        sb.append("        double[] a = new double[p.size()];\n");
        sb.append("        for (int i=0;i<p.size();i++) a[i]=Double.parseDouble(p.get(i).trim());\n");
        sb.append("        return a;\n");
        sb.append("    }\n\n");

        // parseStr1D
        sb.append("    static String[] parseStr1D(String s) {\n");
        sb.append("        List<String> p = splitArr(s);\n");
        sb.append("        if (p.isEmpty()||(p.size()==1&&p.get(0).isEmpty())) return new String[0];\n");
        sb.append("        String[] a = new String[p.size()];\n");
        sb.append("        for (int i=0;i<p.size();i++) a[i]=parseStr(p.get(i));\n");
        sb.append("        return a;\n");
        sb.append("    }\n\n");

        // parseChar1D
        sb.append("    static char[] parseChar1D(String s) {\n");
        sb.append("        List<String> p = splitArr(s);\n");
        sb.append("        if (p.isEmpty()||(p.size()==1&&p.get(0).isEmpty())) return new char[0];\n");
        sb.append("        char[] a = new char[p.size()];\n");
        sb.append("        for (int i=0;i<p.size();i++) a[i]=parseStr(p.get(i)).charAt(0);\n");
        sb.append("        return a;\n");
        sb.append("    }\n\n");

        // parseInt2D
        sb.append("    static int[][] parseInt2D(String s) {\n");
        sb.append("        List<String> p = splitArr(s);\n");
        sb.append("        if (p.isEmpty()||(p.size()==1&&p.get(0).isEmpty())) return new int[0][];\n");
        sb.append("        int[][] a = new int[p.size()][];\n");
        sb.append("        for (int i=0;i<p.size();i++) a[i]=parseInt1D(p.get(i));\n");
        sb.append("        return a;\n");
        sb.append("    }\n\n");

        // parseStr2D
        sb.append("    static String[][] parseStr2D(String s) {\n");
        sb.append("        List<String> p = splitArr(s);\n");
        sb.append("        if (p.isEmpty()||(p.size()==1&&p.get(0).isEmpty())) return new String[0][];\n");
        sb.append("        String[][] a = new String[p.size()][];\n");
        sb.append("        for (int i=0;i<p.size();i++) a[i]=parseStr1D(p.get(i));\n");
        sb.append("        return a;\n");
        sb.append("    }\n\n");

        // toJson 序列化
        sb.append("    static String toJson(Object o) {\n");
        sb.append("        if (o==null) return \"null\";\n");
        sb.append("        if (o instanceof String) return \"\\\"\"+(String)o+\"\\\"\";\n");
        sb.append("        if (o instanceof Character) return \"\\\"\"+(Character)o+\"\\\"\";\n");
        sb.append("        if (o instanceof Boolean||o instanceof Number) return o.toString();\n");
        sb.append("        if (o instanceof int[]) {\n");
        sb.append("            int[] a=(int[])o; StringBuilder r=new StringBuilder(\"[\");\n");
        sb.append("            for(int i=0;i<a.length;i++){if(i>0)r.append(\",\");r.append(a[i]);}\n");
        sb.append("            return r.append(\"]\").toString();\n");
        sb.append("        }\n");
        sb.append("        if (o instanceof long[]) {\n");
        sb.append("            long[] a=(long[])o; StringBuilder r=new StringBuilder(\"[\");\n");
        sb.append("            for(int i=0;i<a.length;i++){if(i>0)r.append(\",\");r.append(a[i]);}\n");
        sb.append("            return r.append(\"]\").toString();\n");
        sb.append("        }\n");
        sb.append("        if (o instanceof double[]) {\n");
        sb.append("            double[] a=(double[])o; StringBuilder r=new StringBuilder(\"[\");\n");
        sb.append("            for(int i=0;i<a.length;i++){if(i>0)r.append(\",\");r.append(a[i]);}\n");
        sb.append("            return r.append(\"]\").toString();\n");
        sb.append("        }\n");
        sb.append("        if (o instanceof char[]) {\n");
        sb.append("            char[] a=(char[])o; StringBuilder r=new StringBuilder(\"[\");\n");
        sb.append("            for(int i=0;i<a.length;i++){if(i>0)r.append(\",\");r.append(\"\\\"\").append(a[i]).append(\"\\\"\");}\n");
        sb.append("            return r.append(\"]\").toString();\n");
        sb.append("        }\n");
        sb.append("        if (o instanceof String[]) {\n");
        sb.append("            String[] a=(String[])o; StringBuilder r=new StringBuilder(\"[\");\n");
        sb.append("            for(int i=0;i<a.length;i++){if(i>0)r.append(\",\");r.append(\"\\\"\").append(a[i]).append(\"\\\"\");}\n");
        sb.append("            return r.append(\"]\").toString();\n");
        sb.append("        }\n");
        sb.append("        if (o instanceof int[][]) {\n");
        sb.append("            int[][] a=(int[][])o; StringBuilder r=new StringBuilder(\"[\");\n");
        sb.append("            for(int i=0;i<a.length;i++){if(i>0)r.append(\",\");r.append(toJson(a[i]));}\n");
        sb.append("            return r.append(\"]\").toString();\n");
        sb.append("        }\n");
        sb.append("        if (o instanceof String[][]) {\n");
        sb.append("            String[][] a=(String[][])o; StringBuilder r=new StringBuilder(\"[\");\n");
        sb.append("            for(int i=0;i<a.length;i++){if(i>0)r.append(\",\");r.append(toJson(a[i]));}\n");
        sb.append("            return r.append(\"]\").toString();\n");
        sb.append("        }\n");
        sb.append("        if (o instanceof List) {\n");
        sb.append("            List<?> l=(List<?>)o; StringBuilder r=new StringBuilder(\"[\");\n");
        sb.append("            for(int i=0;i<l.size();i++){if(i>0)r.append(\",\");r.append(toJson(l.get(i)));}\n");
        sb.append("            return r.append(\"]\").toString();\n");
        sb.append("        }\n");
        sb.append("        return o.toString();\n");
        sb.append("    }\n\n");
    }

    /**
     * 根据 LeetCode 类型返回解析表达式，$L 占位符代表读取的行
     */
    private static String toParseExpr(String lcType) {
        return switch (lcType) {
            case "integer" -> "Integer.parseInt(($L).trim())";
            case "long" -> "Long.parseLong(($L).trim())";
            case "double" -> "Double.parseDouble(($L).trim())";
            case "boolean" -> "Boolean.parseBoolean(($L).trim())";
            case "string" -> "parseStr($L)";
            case "character" -> "parseStr($L).charAt(0)";
            case "integer[]" -> "parseInt1D($L)";
            case "long[]" -> "parseLong1D($L)";
            case "double[]" -> "parseDouble1D($L)";
            case "string[]" -> "parseStr1D($L)";
            case "character[]" -> "parseChar1D($L)";
            case "integer[][]" -> "parseInt2D($L)";
            case "string[][]" -> "parseStr2D($L)";
            default -> "($L)";
        };
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
        sb.append("        if lines[i] == '" + INPUT_END + "':\n");
        sb.append("            break\n");
        for (int p = 0; p < paramCount; p++) {
            sb.append("        " + paramNames.get(p) + " = json.loads(lines[i + " + p + "])\n");
        }
        sb.append("        try:\n");
        sb.append("            result = solution." + funcName + "("
                + String.join(", ", paramNames) + ")\n");
        sb.append("            print(json.dumps(result))\n");
        sb.append("        except Exception as e:\n");
        sb.append("            print(str(e), file=sys.stderr)\n");
        sb.append("        print('" + CASE_END + "')\n");
        sb.append("        i += " + (paramCount + 1) + "\n");
        sb.append("\n");
        sb.append("if __name__ == '__main__':\n");
        sb.append("    main()\n");

        return sb.toString();
    }

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
