package com.exam.server.core;

import com.exam.common.model.*;
import com.exam.common.protocol.MessageType;
import com.exam.common.protocol.Request;
import com.exam.common.protocol.Response;
import com.exam.server.manager.ExamManager;
import com.exam.server.manager.ScoreManager;
import com.exam.server.manager.UserManager;
import com.exam.server.service.AIService;
import com.exam.server.service.ExamObserver;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable, ExamObserver {

    private final Socket socket;
    private final UserManager userManager;
    private final ExamManager examManager;
    private final ScoreManager scoreManager;
    private final AIService aiService;

    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private volatile boolean running = true;
    private String currentExamId; // 当前所在的考试ID(用于接收推送)

    public ClientHandler(Socket socket, UserManager userManager,
                         ExamManager examManager, ScoreManager scoreManager,
                         AIService aiService) {
        this.socket = socket;
        this.userManager = userManager;
        this.examManager = examManager;
        this.scoreManager = scoreManager;
        this.aiService = aiService;
    }

    @Override
    public void run() {
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());

            while (running) {
                Request request = (Request) ois.readObject();
                Response response = dispatch(request);
                sendResponse(response);
            }
        } catch (EOFException e) {
            // 客户端正常断开
        } catch (Exception e) {
            System.err.println("ClientHandler error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private Response dispatch(Request request) {
        MessageType type = request.getType();
        try {
            switch (type) {
                case REGISTER:   return handleRegister(request);
                case LOGIN:      return handleLogin(request);
                case ADD_USER:   return handleAddUser(request);
                case DELETE_USER:return handleDeleteUser(request);
                case GET_USERS:  return handleGetUsers(request);
                case CREATE_EXAM:return handleCreateExam(request);
                case GET_EXAMS:  return handleGetExams(request);
                case JOIN_EXAM:  return handleJoinExam(request);
                case SUBMIT_ANSWER:return handleSubmitAnswer(request);
                case SUBMIT_EXAM:return handleSubmitExam(request);
                case GET_SCORES: return handleGetScores(request);
                case GET_SUBMISSIONS: return handleGetSubmissions(request);
                case GRADE_ESSAY: return handleGradeEssay(request);
                case PUBLISH_SCORES: return handlePublishScores(request);
                case GET_EXAM_REVIEW: return handleGetExamReview(request);
                case AI_CHAT: return handleAIChat(request);
                default:
                    return Response.error(type, "Unknown message type");
            }
        } catch (Exception e) {
            return Response.error(type, "Server error: " + e.getMessage());
        }
    }

    private Response handleRegister(Request request) {
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) request.getData();
        String username = data.get("username");
        String password = data.get("password");
        String role = data.get("role");

        User user = userManager.register(username, password, role);
        if (user == null) {
            return Response.error(MessageType.REGISTER, "用户名已存在或角色无效");
        }
        Map<String, String> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        return Response.ok(MessageType.REGISTER, (Serializable) result);
    }

    private Response handleLogin(Request request) {
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) request.getData();
        String username = data.get("username");
        String password = data.get("password");

        User user = userManager.login(username, password);
        if (user == null) {
            return Response.error(MessageType.LOGIN, "用户名或密码错误");
        }
        Map<String, String> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        return Response.ok(MessageType.LOGIN, (Serializable) result);
    }

    private Response handleAddUser(Request request) {
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) request.getData();
        User user = userManager.addUser(data.get("username"), data.get("password"), data.get("role"));
        if (user == null) {
            return Response.error(MessageType.ADD_USER, "添加失败，用户名可能已存在");
        }
        return Response.ok(MessageType.ADD_USER, null);
    }

    private Response handleDeleteUser(Request request) {
        String username = (String) request.getData();
        boolean ok = userManager.deleteUser(username);
        if (!ok) {
            return Response.error(MessageType.DELETE_USER, "删除失败，不能删除管理员或用户不存在");
        }
        return Response.ok(MessageType.DELETE_USER, null);
    }

    private Response handleGetUsers(Request request) {
        List<User> userList = userManager.getAllUsers();
        // 转为Map列表用于序列化
        List<Map<String, String>> result = userList.stream().map(u -> {
            Map<String, String> m = new HashMap<>();
            m.put("username", u.getUsername());
            m.put("role", u.getRole());
            return m;
        }).collect(Collectors.toList());
        return Response.ok(MessageType.GET_USERS, (Serializable) result);
    }

    private Response handleCreateExam(Request request) {
        Exam exam = (Exam) request.getData();
        examManager.createExam(exam);
        return Response.ok(MessageType.CREATE_EXAM, null);
    }

    private Response handleGetExams(Request request) {
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) request.getData();
        String role = data.get("role");
        String username = data.get("username");

        List<Exam> allExams = examManager.getExams();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Exam e : allExams) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("title", e.getTitle());
            m.put("teacherId", e.getTeacherId());
            m.put("duration", e.getDuration());
            m.put("scheduledStartTime", e.getScheduledStartTime());
            m.put("status", e.getStatus());
            m.put("questionCount", e.getQuestions().size());
            m.put("totalScore", e.getTotalScore());
            m.put("joinedCount", e.getJoinedCount());

            m.put("hasEssayQuestions", e.hasEssayQuestions());
            m.put("scoresPublished", e.isScoresPublished());

            if ("STUDENT".equals(role) && examManager.hasSubmitted(e.getId(), username)) {
                m.put("submitted", true);
            } else {
                m.put("submitted", false);
            }

            result.add(m);
        }
        return Response.ok(MessageType.GET_EXAMS, (Serializable) result);
    }

    private Response handleJoinExam(Request request) {
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) request.getData();
        String examId = data.get("examId");
        String studentId = data.get("studentId");

        String status = examManager.joinExam(examId, studentId);
        if ("JOINED_PROGRESS".equals(status)) {
            // 考试正在进行中，注册观察者以接收推送
            currentExamId = examId;
            examManager.addObserver(examId, this);
            Exam exam = examManager.getExam(examId);
            Map<String, Object> examData = buildExamDataForStudent(exam);
            return Response.ok(MessageType.JOIN_EXAM, "考试开始", (Serializable) examData);
        } else if ("ALREADY_SUBMITTED".equals(status)) {
            return Response.error(MessageType.JOIN_EXAM, "您已交卷，不能重复参加");
        } else if ("ALREADY_JOINED".equals(status)) {
            // 已加入但未交卷，重新注册观察者
            currentExamId = examId;
            examManager.addObserver(examId, this);
            Exam exam = examManager.getExam(examId);
            Map<String, Object> examData = buildExamDataForStudent(exam);
            return Response.ok(MessageType.JOIN_EXAM, "考试开始", (Serializable) examData);
        } else {
            return Response.error(MessageType.JOIN_EXAM, status);
        }
    }

    private Response handleSubmitAnswer(Request request) {
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) request.getData();
        String examId = data.get("examId");
        String studentId = data.get("studentId");
        String questionId = data.get("questionId");
        String answer = data.get("answer");

        examManager.saveAnswer(examId, studentId, questionId, answer);
        return Response.ok(MessageType.SUBMIT_ANSWER, null);
    }

    private Response handleSubmitExam(Request request) {
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) request.getData();
        String examId = data.get("examId");
        String studentId = data.get("studentId");

        ExamResult result = examManager.submitExam(examId, studentId);
        if (result == null) {
            return Response.error(MessageType.SUBMIT_EXAM, "提交失败");
        }
        // 保留观察者，计时结束后由 EXAM_FINISHED 推送通知考试结束
        return Response.ok(MessageType.SUBMIT_EXAM, "交卷成功，等待考试结束", (Serializable) result);
    }

    private Response handleGetScores(Request request) {
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) request.getData();
        String role = data.get("role");
        String username = data.get("username");

        List<ExamResult> resultList;
        if ("TEACHER".equals(role)) {
            resultList = new ScoreManager().getAllResults();
        } else {
            List<ExamResult> source = "STUDENT".equals(role)
                    ? new ScoreManager().getResultsByStudent(username)
                    : new ScoreManager().getAllResults();
            resultList = new ArrayList<>();
            for (ExamResult r : source) {
                Exam exam = examManager.getExam(r.getExamId());
                if (exam == null) continue;
                if (!exam.hasEssayQuestions() || exam.isScoresPublished()) {
                    resultList.add(r);
                }
            }
        }
        return Response.ok(MessageType.GET_SCORES, (Serializable) resultList);
    }

    private Response handleGetSubmissions(Request request) {
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) request.getData();
        String examId = data.get("examId");

        Map<String, Object> submissions = examManager.getSubmissions(examId);
        if (submissions == null) {
            return Response.error(MessageType.GET_SUBMISSIONS, "考试不存在");
        }
        return Response.ok(MessageType.GET_SUBMISSIONS, (Serializable) submissions);
    }

    private Response handleGradeEssay(Request request) {
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) request.getData();
        String examId = data.get("examId");
        String studentId = data.get("studentId");
        String questionId = data.get("questionId");
        int score = Integer.parseInt(data.get("score"));

        String result = examManager.gradeEssay(examId, studentId, questionId, score);
        if ("OK".equals(result)) {
            return Response.ok(MessageType.GRADE_ESSAY, "评分成功", null);
        }
        return Response.error(MessageType.GRADE_ESSAY, result);
    }

    private Response handlePublishScores(Request request) {
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) request.getData();
        String examId = data.get("examId");

        String result = examManager.publishScores(examId);
        if ("OK".equals(result)) {
            return Response.ok(MessageType.PUBLISH_SCORES, "成绩已发布", null);
        }
        return Response.error(MessageType.PUBLISH_SCORES, result);
    }

    // ==================== 考试复盘 ====================

    private Response handleGetExamReview(Request request) {
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) request.getData();
        String examId = data.get("examId");
        String username = data.get("username");
        String role = data.get("role");

        Exam exam = examManager.getExam(examId);
        if (exam == null) {
            return Response.error(MessageType.GET_EXAM_REVIEW, "考试不存在");
        }
        if (!Exam.FINISHED.equals(exam.getStatus())) {
            return Response.error(MessageType.GET_EXAM_REVIEW, "考试尚未结束，暂不支持复盘");
        }
        if (!exam.isScoresPublished()) {
            return Response.error(MessageType.GET_EXAM_REVIEW, "成绩尚未发布，暂不支持复盘");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("examId", exam.getId());
        result.put("examTitle", exam.getTitle());

        // 构建题目信息列表（含正确答案和解析）
        List<Map<String, Object>> questionInfos = new ArrayList<>();
        for (Question q : exam.getQuestions()) {
            Map<String, Object> qInfo = new HashMap<>();
            qInfo.put("id", q.getId());
            qInfo.put("content", q.getContent());
            qInfo.put("score", q.getScore());
            qInfo.put("difficulty", q.getDifficulty());
            qInfo.put("type", q instanceof ChoiceQuestion ? "CHOICE" :
                              q instanceof EssayQuestion ? "ESSAY" : "BLANK");
            qInfo.put("explanation", q.getExplanation() != null ? q.getExplanation() : "");

            if (q instanceof ChoiceQuestion) {
                ChoiceQuestion cq = (ChoiceQuestion) q;
                qInfo.put("options", (Serializable) new ArrayList<>(cq.getOptions()));
                qInfo.put("correctAnswer", String.valueOf(cq.getCorrectIndex()));
                qInfo.put("correctAnswerText", cq.getOptions().get(cq.getCorrectIndex()));
            } else if (q instanceof BlankQuestion) {
                BlankQuestion bq = (BlankQuestion) q;
                qInfo.put("correctAnswer", bq.getCorrectAnswer());
            } else if (q instanceof EssayQuestion) {
                EssayQuestion eq = (EssayQuestion) q;
                qInfo.put("correctAnswer", eq.getReferenceAnswer());
            }
            questionInfos.add(qInfo);
        }
        result.put("questions", (Serializable) questionInfos);

        // 加载成绩数据（学生和教师都需要）
        ScoreManager sm = new ScoreManager();
        List<ExamResult> allResults = sm.getResultsByExam(examId);

        if ("STUDENT".equals(role)) {
            // 学生复盘：返回该学生的答案和得分
            Map<String, String> studentAnswerMap = exam.getStudentAnswerMap(username);
            result.put("studentAnswers", (Serializable) new HashMap<>(studentAnswerMap));

            // 获取该学生的成绩结果
            ExamResult examResult = null;
            for (ExamResult r : allResults) {
                if (r.getStudentId().equals(username)) {
                    examResult = r;
                    break;
                }
            }

            // 构建每题得分
            Map<String, Integer> perQuestionScores = new HashMap<>();
            if (examResult != null) {
                for (Question q : exam.getQuestions()) {
                    if (q instanceof EssayQuestion) {
                        Map<String, Integer> essayScores = examResult.getEssayScores();
                        if (essayScores != null && essayScores.containsKey(q.getId())) {
                            int s = essayScores.get(q.getId());
                            perQuestionScores.put(q.getId(), s >= 0 ? s : -1); // -1 表示未批改
                        } else {
                            perQuestionScores.put(q.getId(), -1);
                        }
                    } else {
                        boolean correct = q.checkAnswer(studentAnswerMap.get(q.getId()));
                        perQuestionScores.put(q.getId(), correct ? q.getScore() : 0);
                    }
                }
            }
            result.put("questionScores", (Serializable) perQuestionScores);

        } else if ("TEACHER".equals(role)) {
            // 教师复盘：返回所有学生答案用于统计
            Map<String, Map<String, String>> submittedAnswers = new HashMap<>();
            for (Map.Entry<String, Map<String, String>> entry : exam.getStudentAnswers().entrySet()) {
                if (entry.getValue().containsKey("__SUBMITTED__")) {
                    submittedAnswers.put(entry.getKey(), new HashMap<>(entry.getValue()));
                }
            }
            result.put("allStudentAnswers", (Serializable) submittedAnswers);

            // 返回所有学生成绩（含简答题分数），供教师端计算平均分
            result.put("allResults", (Serializable) new ArrayList<>(allResults));
        }

        return Response.ok(MessageType.GET_EXAM_REVIEW, (Serializable) result);
    }

    // ==================== AI 对话 ====================

    @SuppressWarnings("unchecked")
    private Response handleAIChat(Request request) {
        Map<String, String> data = (Map<String, String>) request.getData();
        String examId = data.get("examId");
        String username = data.get("username");
        String action = data.get("action");   // "INIT" or "CHAT"
        String message = data.get("message");

        // Session key: 每个学生的每场考试一个独立会话
        String sessionKey = username + "@" + examId;

        try {
            if ("INIT".equals(action)) {
                // 先获取复盘数据用于构建 System Prompt
                Map<String, String> reviewReq = new HashMap<>();
                reviewReq.put("examId", examId);
                reviewReq.put("username", username);
                reviewReq.put("role", "STUDENT");

                // 直接调用复盘逻辑获取数据
                Response reviewResp = handleGetExamReview(
                        new Request(MessageType.GET_EXAM_REVIEW, (java.io.Serializable) reviewReq));
                if (!"OK".equals(reviewResp.getStatus())) {
                    return Response.error(MessageType.AI_CHAT, reviewResp.getMessage());
                }

                Map<String, Object> reviewData = (Map<String, Object>) reviewResp.getData();
                String welcome = aiService.initChat(sessionKey, reviewData);

                Map<String, String> result = new HashMap<>();
                result.put("reply", welcome);
                return Response.ok(MessageType.AI_CHAT, (java.io.Serializable) result);

            } else if ("CHAT".equals(action)) {
                if (message == null || message.trim().isEmpty()) {
                    return Response.error(MessageType.AI_CHAT, "消息不能为空");
                }
                String reply = aiService.chat(sessionKey, message.trim());

                Map<String, String> result = new HashMap<>();
                result.put("reply", reply);
                return Response.ok(MessageType.AI_CHAT, (java.io.Serializable) result);

            } else {
                return Response.error(MessageType.AI_CHAT, "Unknown action: " + action);
            }
        } catch (Exception e) {
            return Response.error(MessageType.AI_CHAT, "AI service error: " + e.getMessage());
        }
    }

    // ==================== 构建学生端考试数据(不含答案) ====================

    private Map<String, Object> buildExamDataForStudent(Exam exam) {
        Map<String, Object> data = new HashMap<>();
        data.put("examId", exam.getId());
        data.put("title", exam.getTitle());
        data.put("duration", exam.getDuration());
        data.put("scheduledStartTime", exam.getScheduledStartTime());
        data.put("actualStartTime", exam.getActualStartTime());
        data.put("remainingSeconds", exam.getRemainingSeconds());
        data.put("questions", (Serializable) new ArrayList<>(exam.getQuestions()));
        data.put("totalScore", exam.getTotalScore());
        return data;
    }

    // ==================== ExamObserver 推送实现 ====================

    @Override
    public void onExamStarted(Exam exam) {
        Map<String, Object> examData = buildExamDataForStudent(exam);
        Response resp = Response.ok(MessageType.EXAM_STARTED, (Serializable) examData);
        sendResponse(resp);
    }

    @Override
    public void onTimeSync(String examId, int remainingSeconds) {
        Map<String, Object> data = new HashMap<>();
        data.put("examId", examId);
        data.put("remaining", remainingSeconds);
        Response resp = Response.ok(MessageType.TIME_SYNC, (Serializable) data);
        sendResponse(resp);
    }

    @Override
    public void onExamFinished(String examId) {
        // 获取该学生在本次考试的成绩
        Response resp = Response.ok(MessageType.EXAM_FINISHED, "考试结束");
        sendResponse(resp);
        examManager.removeObserver(examId, this);
        currentExamId = null;
    }

    // ==================== 辅助方法 ====================

    private synchronized void sendResponse(Response resp) {
        try {
            oos.writeObject(resp);
            oos.flush();
        } catch (Exception e) {
            running = false;
        }
    }

    private void cleanup() {
        running = false;
        if (currentExamId != null) {
            examManager.removeObserver(currentExamId, this);
        }
        try { socket.close(); } catch (Exception ignored) {}
    }
}
