package com.exam.server.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 对话服务 — 封装 LLM API 调用 (OpenAI 兼容格式)
 * <p>
 * 配置文件: ai.properties (位于工作目录)
 * 支持任意兼容 OpenAI Chat Completions 格式的 API。
 */
public class AIService {

    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final int timeout;

    /** 会话存储: sessionKey -> 消息历史 */
    private final Map<String, List<Map<String, String>>> sessions = new ConcurrentHashMap<>();

    public AIService() {
        Properties props = loadProperties();

        this.endpoint   = props.getProperty("ai.api.endpoint", "https://api.openai.com/v1/chat/completions");
        this.apiKey     = props.getProperty("ai.api.key", "");
        this.model      = props.getProperty("ai.api.model", "gpt-4o");
        this.maxTokens  = Integer.parseInt(props.getProperty("ai.api.max_tokens", "1024"));
        this.temperature = Double.parseDouble(props.getProperty("ai.api.temperature", "0.7"));
        this.timeout    = Integer.parseInt(props.getProperty("ai.api.timeout", "30000"));

        System.out.println("[AIService] Initialized, endpoint=" + endpoint + ", model=" + model);
    }

    // ==================== 公开方法 ====================

    /**
     * 初始化对话：使用考试复盘数据构建 System Prompt，存储会话，返回欢迎语。
     * 不调用 API，欢迎语由本地计算生成。
     *
     * @param sessionKey      会话唯一标识
     * @param examReviewData  考试复盘数据 (来自 handleGetExamReview 的 result map)
     * @return AI 欢迎语
     */
    public String initChat(String sessionKey, Map<String, Object> examReviewData) {
        String systemPrompt = buildSystemPrompt(examReviewData);
        String welcome = buildWelcomeMessage(examReviewData);

        List<Map<String, String>> history = new ArrayList<>();
        Map<String, String> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        history.add(sysMsg);

        Map<String, String> assistMsg = new HashMap<>();
        assistMsg.put("role", "assistant");
        assistMsg.put("content", welcome);
        history.add(assistMsg);

        sessions.put(sessionKey, history);
        return welcome;
    }

    /**
     * 发送用户消息，调用 LLM API，返回 AI 回复。
     *
     * @param sessionKey 会话唯一标识
     * @param userMessage 用户消息
     * @return AI 回复内容；API 调用失败返回错误信息
     */
    public String chat(String sessionKey, String userMessage) {
        List<Map<String, String>> history = sessions.get(sessionKey);
        if (history == null) {
            return "⚠️ 会话已过期，请重新打开复盘界面。";
        }

        // 添加用户消息
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        history.add(userMsg);

        // 调用 API
        String reply;
        try {
            reply = callAPI(history);
        } catch (Exception e) {
            reply = "⚠️ AI 服务暂时不可用: " + e.getMessage();
            System.err.println("[AIService] API call failed: " + e.getMessage());
        }

        // 添加 AI 回复到历史
        Map<String, String> assistMsg = new HashMap<>();
        assistMsg.put("role", "assistant");
        assistMsg.put("content", reply);
        history.add(assistMsg);

        // 限制历史长度，保留 system + 最近 20 轮
        if (history.size() > 41) { // 1 system + 20*2
            Map<String, String> sys = history.get(0);
            List<Map<String, String>> trimmed = new ArrayList<>();
            trimmed.add(sys);
            trimmed.addAll(history.subList(history.size() - 40, history.size()));
            sessions.put(sessionKey, trimmed);
        }

        return reply;
    }

    /**
     * 清除会话
     */
    public void clearSession(String sessionKey) {
        sessions.remove(sessionKey);
    }

    // ==================== System Prompt 构建 ====================

    @SuppressWarnings("unchecked")
    private String buildSystemPrompt(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的考试复盘辅导老师。学生刚完成了一场考试，请你帮助学生分析复盘。\n\n");

        // 考试信息
        String examTitle = (String) data.getOrDefault("examTitle", "未知");
        sb.append("=== 考试信息 ===\n");
        sb.append("考试：").append(examTitle).append("\n");

        List<Map<String, Object>> questions = (List<Map<String, Object>>) data.get("questions");
        Map<String, String> studentAnswers = (Map<String, String>) data.get("studentAnswers");
        Map<String, Integer> questionScores = (Map<String, Integer>) data.get("questionScores");

        int totalScore = 0, earnedScore = 0, correctCount = 0;
        if (questions != null && questionScores != null) {
            for (Map<String, Object> q : questions) {
                int qs = ((Number) q.get("score")).intValue();
                totalScore += qs;
                Integer es = questionScores.get(q.get("id"));
                if (es != null && es >= 0) {
                    earnedScore += es;
                    if (es == qs) correctCount++;
                }
            }
        }
        sb.append("总分：").append(totalScore).append(" 分\n");
        sb.append("学生得分：").append(earnedScore).append(" 分\n");
        double rate = totalScore > 0 ? 100.0 * earnedScore / totalScore : 0;
        sb.append("正确率：").append(String.format("%.1f%%", rate)).append("\n\n");

        // 题目详情
        sb.append("=== 题目详情 ===\n");
        if (questions != null) {
            for (int i = 0; i < questions.size(); i++) {
                Map<String, Object> q = questions.get(i);
                String type = (String) q.get("type");
                String diff = (String) q.get("difficulty");
                String typeTag = "CHOICE".equals(type) ? "选择题" : "ESSAY".equals(type) ? "简答题" : "填空题";
                String diffTag = "BASIC".equals(diff) ? "基础题" : "MEDIUM".equals(diff) ? "中等题" : "提高题";

                sb.append("\n--- 第").append(i + 1).append("题 [").append(typeTag)
                  .append(" · ").append(diffTag).append(" · ").append(q.get("score")).append("分] ---\n");
                sb.append("题目：").append(q.get("content")).append("\n");

                if ("CHOICE".equals(type)) {
                    List<String> options = (List<String>) q.get("options");
                    if (options != null) {
                        for (int j = 0; j < options.size(); j++) {
                            sb.append("  ").append((char) ('A' + j)).append(". ").append(options.get(j)).append("\n");
                        }
                    }
                }

                String correctAnswer = (String) q.get("correctAnswer");
                if ("CHOICE".equals(type) && q.containsKey("correctAnswerText")) {
                    sb.append("正确答案：").append(correctAnswer)
                      .append(" (").append(q.get("correctAnswerText")).append(")\n");
                } else {
                    sb.append("正确答案：").append(correctAnswer).append("\n");
                }

                if (studentAnswers != null) {
                    String myAns = studentAnswers.getOrDefault(q.get("id"), "（未作答）");
                    if (myAns == null || myAns.isEmpty() || "__SUBMITTED__".equals(myAns)) {
                        myAns = "（未作答）";
                    }
                    sb.append("学生答案：").append(myAns).append("\n");
                }

                if (questionScores != null) {
                    Integer es = questionScores.get(q.get("id"));
                    if ("ESSAY".equals(type) && (es == null || es < 0)) {
                        sb.append("得分：待批改 / ").append(q.get("score")).append(" 分\n");
                    } else {
                        sb.append("得分：").append(es).append(" / ").append(q.get("score")).append(" 分\n");
                    }
                }

                String explanation = (String) q.getOrDefault("explanation", "");
                if (explanation != null && !explanation.isEmpty()) {
                    sb.append("教师解析：").append(explanation).append("\n");
                }
            }
        }

        // 辅导规则
        sb.append("\n=== 辅导规则 ===\n");
        sb.append("1. 当学生问某道题的解法时，请结合题目内容详细解答，说明解题思路\n");
        sb.append("2. 当学生要求总结或分析时，请根据得分数据指出薄弱环节（按难度和题型分类），给出针对性学习建议\n");
        sb.append("3. 鼓励学生，语气亲切专业\n");
        sb.append("4. 使用中文回答\n");
        sb.append("5. 回答要简洁有条理，不要过长\n");

        return sb.toString();
    }

    private String buildWelcomeMessage(Map<String, Object> data) {
        List<Map<String, Object>> questions = (List<Map<String, Object>>) data.get("questions");
        Map<String, Integer> questionScores = (Map<String, Integer>) data.get("questionScores");

        int totalScore = 0, earnedScore = 0, correctCount = 0, essayCount = 0;
        if (questions != null && questionScores != null) {
            for (Map<String, Object> q : questions) {
                int qs = ((Number) q.get("score")).intValue();
                totalScore += qs;
                Integer es = questionScores.get(q.get("id"));
                if (es != null && es >= 0) {
                    earnedScore += es;
                    if (es == qs) correctCount++;
                }
                if ("ESSAY".equals(q.get("type"))) {
                    essayCount++;
                }
            }
        }

        double rate = totalScore > 0 ? 100.0 * earnedScore / totalScore : 0;
        int objCount = (questions != null ? questions.size() : 0) - essayCount;

        StringBuilder sb = new StringBuilder();
        sb.append("你好！我已经读取了你的考试数据。\n\n");
        sb.append("* 本次考试共 ").append(questions != null ? questions.size() : 0).append(" 题，");
        sb.append("总分 ").append(totalScore).append(" 分。\n");
        sb.append("* 你答对了 ").append(correctCount).append(" 题，");
        sb.append("得分 ").append(earnedScore).append(" 分，");
        sb.append("正确率 ").append(String.format("%.1f%%", rate)).append("。\n\n");
        sb.append("你可以问我：\n");
        sb.append("- 某道题的解题思路（如'第3题为什么选这个？'）\n");
        sb.append("- 本次考试的薄弱环节分析\n");
        sb.append("- 针对错题的提升建议\n");
        sb.append("- 某类题型的答题技巧\n\n");
        sb.append("有什么想了解的，尽管问我吧！");
        return sb.toString();
    }

    // ==================== LLM API 调用 ====================

    private String callAPI(List<Map<String, String>> messages) throws Exception {
        String requestBody = buildRequestBody(messages);

        HttpURLConnection conn = null;
        try {
            URI uri = new URI(endpoint);
            URL url = uri.toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);

            // 发送请求体
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // 读取响应
            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String responseBody;
            if (is != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder respBuilder = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        respBuilder.append(line);
                    }
                    responseBody = respBuilder.toString();
                }
            } else {
                responseBody = "(empty response body)";
            }

            if (code != 200) {
                System.err.println("[AIService] API error " + code + ": " + truncate(responseBody, 300));
                throw new IOException("HTTP " + code + " - " + truncate(responseBody, 200));
            }

            String content = extractContent(responseBody);
            System.out.println("[AIService] Got reply (" + content.length() + " chars)");
            return content;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("[AIService] Connection failed: " + e.getMessage());
            throw new IOException("Connection failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * 从 OpenAI 格式响应中提取 message.content
     */
    private String extractContent(String responseBody) throws IOException {
        // 简单 JSON 解析：寻找 "content": "..."
        // OpenAI 格式: {"choices":[{"message":{"content":"..."}}]}
        String key = "\"content\":\"";
        int idx = responseBody.indexOf(key);
        if (idx < 0) {
            // 可能 content 在别的格式中，尝试另一种
            key = "\"content\": \"";
            idx = responseBody.indexOf(key);
        }
        if (idx < 0) {
            throw new IOException("Unexpected response format: " + truncate(responseBody, 100));
        }

        idx += key.length();
        StringBuilder content = new StringBuilder();
        boolean escaped = false;
        for (; idx < responseBody.length(); idx++) {
            char c = responseBody.charAt(idx);
            if (escaped) {
                escaped = false;
                switch (c) {
                    case 'n': content.append('\n'); break;
                    case 't': content.append('\t'); break;
                    case 'r': content.append('\r'); break;
                    case '\\': content.append('\\'); break;
                    case '"': content.append('"'); break;
                    case 'u':
                        // unicode escape sequence
                        if (idx + 4 < responseBody.length()) {
                            try {
                                int codePoint = Integer.parseInt(responseBody.substring(idx + 1, idx + 5), 16);
                                content.append((char) codePoint);
                                idx += 4;
                            } catch (NumberFormatException e) {
                                content.append('u');
                            }
                        }
                        break;
                    default:
                        content.append('\\').append(c);
                        break;
                }
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break; // end of content string
            } else {
                content.append(c);
            }
        }
        return content.toString().trim();
    }

    /**
     * 构建 OpenAI Chat Completions 请求 JSON
     */
    private String buildRequestBody(List<Map<String, String>> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(escapeJson(model)).append("\",");
        sb.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) sb.append(",");
            Map<String, String> msg = messages.get(i);
            sb.append("{\"role\":\"").append(escapeJson(msg.get("role"))).append("\",");
            sb.append("\"content\":\"").append(escapeJson(msg.get("content"))).append("\"}");
        }
        sb.append("],");
        sb.append("\"max_tokens\":").append(maxTokens).append(",");
        sb.append("\"temperature\":").append(temperature);
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // ==================== 辅助方法 ====================

    private Properties loadProperties() {
        Properties props = new Properties();
        File propFile = new File("ai.properties");
        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile);
                 InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) {
                System.err.println("[AIService] Failed to load ai.properties: " + e.getMessage());
            }
        } else {
            System.err.println("[AIService] ai.properties not found at " + propFile.getAbsolutePath());
        }
        return props;
    }

    private String truncate(String s, int maxLen) {
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
