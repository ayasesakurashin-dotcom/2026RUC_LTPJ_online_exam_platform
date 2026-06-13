package com.exam.server.manager;

import com.exam.common.model.*;
import com.exam.server.service.ExamObserver;
import com.exam.server.storage.FileStorage;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ExamManager {
    private static ExamManager instance;

    private List<Exam> exams;
    private final Map<String, List<ExamObserver>> observers = new HashMap<>();

    private ExamManager() {
        exams = FileStorage.load(FileStorage.EXAMS_FILE);
        // 重启后重调度未开始或进行中的考试
        for (Exam e : exams) {
            if (Exam.NOT_STARTED.equals(e.getStatus())) {
                scheduleExamStart(e);
            } else if (Exam.IN_PROGRESS.equals(e.getStatus())) {
                long endTime = e.getActualStartTime() + e.getDuration() * 1000L;
                TimerManager.getInstance().scheduleEnd(e.getId(), endTime,
                        () -> timeoutExam(e.getId()));
            }
        }
    }

    public static synchronized ExamManager getInstance() {
        if (instance == null) instance = new ExamManager();
        return instance;
    }

    // ==================== Observer 管理 ====================

    public synchronized void addObserver(String examId, ExamObserver observer) {
        observers.computeIfAbsent(examId, k -> new CopyOnWriteArrayList<>()).add(observer);
    }

    public synchronized void removeObserver(String examId, ExamObserver observer) {
        List<ExamObserver> list = observers.get(examId);
        if (list != null) list.remove(observer);
    }

    // ==================== 考试 CRUD ====================

    public synchronized Exam createExam(Exam exam) {
        exams.add(exam);
        save();
        // 预约开始
        scheduleExamStart(exam);
        return exam;
    }

    public synchronized List<Exam> getExams() {
        return new ArrayList<>(exams);
    }

    public synchronized Exam getExam(String examId) {
        return exams.stream().filter(e -> e.getId().equals(examId)).findFirst().orElse(null);
    }

    // ==================== 考试流程 ====================

    /** 学生加入考试：仅进行中可加入 */
    public synchronized String joinExam(String examId, String studentId) {
        Exam exam = getExam(examId);
        if (exam == null) return "考试不存在";

        if (Exam.FINISHED.equals(exam.getStatus())) return "考试已结束";
        if (Exam.NOT_STARTED.equals(exam.getStatus())) return "考试尚未开始";

        if (exam.getJoinedStudentIds().contains(studentId)) {
            if (hasSubmitted(examId, studentId)) return "ALREADY_SUBMITTED";
            return "ALREADY_JOINED";
        }

        exam.getJoinedStudentIds().add(studentId);
        exam.getStudentAnswers().putIfAbsent(studentId, new HashMap<>());
        save();
        return "JOINED_PROGRESS";
    }

    /** 保存单题答案 */
    public synchronized void saveAnswer(String examId, String studentId,
                                         String questionId, String answer) {
        Exam exam = getExam(examId);
        if (exam != null) exam.saveAnswer(studentId, questionId, answer);
    }

    /** 学生交卷 */
    public synchronized ExamResult submitExam(String examId, String studentId) {
        Exam exam = getExam(examId);
        if (exam == null) return null;

        ExamResult result = gradeStudent(exam, studentId);
        new ScoreManager().saveResult(result);
        return result;
    }

    public synchronized boolean hasSubmitted(String examId, String studentId) {
        Exam exam = getExam(examId);
        if (exam == null) return true;
        Map<String, String> a = exam.getStudentAnswers().get(studentId);
        return a != null && a.containsKey("__SUBMITTED__");
    }

    // ==================== 定时器驱动 ====================

    /** 预约考试开始（服务端系统时间到达 scheduledStartTime 后触发） */
    private void scheduleExamStart(Exam exam) {
        TimerManager.getInstance().scheduleStart(exam.getId(), exam.getScheduledStartTime(), () -> {
            startExam(exam.getId());
        });
    }

    /** 定时器回调：开始考试 */
    private synchronized void startExam(String examId) {
        Exam exam = getExam(examId);
        if (exam == null || !Exam.NOT_STARTED.equals(exam.getStatus())) return;

        exam.setStatus(Exam.IN_PROGRESS);
        exam.setActualStartTime(System.currentTimeMillis());
        save();

        // 通知已注册观察者（如果有等待的学生）
        List<ExamObserver> obsList = observers.get(examId);
        if (obsList != null) {
            for (ExamObserver obs : obsList) obs.onExamStarted(exam);
        }

        // 预约结束
        long endTime = System.currentTimeMillis() + exam.getDuration() * 1000L;
        TimerManager.getInstance().scheduleEnd(examId, endTime, () -> timeoutExam(examId));
    }

    /** 定时器回调：超时收卷 */
    private synchronized void timeoutExam(String examId) {
        Exam exam = getExam(examId);
        if (exam == null || Exam.FINISHED.equals(exam.getStatus())) return;

        ScoreManager sm = new ScoreManager();
        for (String sid : exam.getJoinedStudentIds()) {
            Map<String, String> a = exam.getStudentAnswers().get(sid);
            if (a == null || !a.containsKey("__SUBMITTED__")) {
                sm.saveResult(gradeStudent(exam, sid));
            }
        }
        finishExam(exam);
    }

    private void finishExam(Exam exam) {
        exam.setStatus(Exam.FINISHED);
        if (!exam.hasEssayQuestions()) {
            exam.setScoresPublished(true);
        }
        save();
        TimerManager.getInstance().cancel(exam.getId());

        List<ExamObserver> obsList = observers.get(exam.getId());
        if (obsList != null) {
            for (ExamObserver obs : obsList) obs.onExamFinished(exam.getId());
        }
    }

    // ==================== 判卷 ====================

    private ExamResult gradeStudent(Exam exam, String studentId) {
        Map<String, String> answerMap = exam.getStudentAnswerMap(studentId);
        int autoScore = 0;
        int bCorr = 0, bTot = 0, mCorr = 0, mTot = 0, aCorr = 0, aTot = 0;
        Map<String, Integer> essayScores = new HashMap<>();

        for (Question q : exam.getQuestions()) {
            if (q instanceof EssayQuestion) {
                essayScores.put(q.getId(), -1);
            } else {
                boolean correct = q.checkAnswer(answerMap.get(q.getId()));
                if (correct) autoScore += q.getScore();
                String diff = q.getDifficulty();
                if (Question.BASIC.equals(diff))      { bTot++; if (correct) bCorr++; }
                else if (Question.MEDIUM.equals(diff)) { mTot++; if (correct) mCorr++; }
                else                                   { aTot++; if (correct) aCorr++; }
            }
        }

        answerMap.put("__SUBMITTED__", "true");
        exam.getStudentAnswers().put(studentId, answerMap);

        ExamResult result = new ExamResult(exam.getId(), exam.getTitle(), studentId,
                autoScore, exam.getTotalScore(), new HashMap<>(answerMap), System.currentTimeMillis());
        result.setAutoScore(autoScore);
        result.setEssayScores(essayScores.isEmpty() ? null : new HashMap<>(essayScores));
        result.setBasicCorrect(bCorr);    result.setBasicTotal(bTot);
        result.setMediumCorrect(mCorr);   result.setMediumTotal(mTot);
        result.setAdvancedCorrect(aCorr); result.setAdvancedTotal(aTot);
        return result;
    }

    // ==================== 简答题批改 ====================

    public synchronized String gradeEssay(String examId, String studentId,
                                           String questionId, int awardedScore) {
        Exam exam = getExam(examId);
        if (exam == null) return "考试不存在";
        if (!Exam.FINISHED.equals(exam.getStatus())) return "考试尚未结束，无法批改";

        Question target = null;
        for (Question q : exam.getQuestions()) {
            if (q.getId().equals(questionId)) { target = q; break; }
        }
        if (target == null || !(target instanceof EssayQuestion))
            return "题目不存在或不是简答题";
        if (awardedScore < 0 || awardedScore > target.getScore())
            return "分数超出范围 (0~" + target.getScore() + ")";

        ScoreManager sm = new ScoreManager();
        ExamResult result = null;
        for (ExamResult r : sm.getResultsByExam(examId)) {
            if (r.getStudentId().equals(studentId)) { result = r; break; }
        }
        if (result == null) return "该学生未提交试卷";

        Map<String, Integer> es = result.getEssayScores();
        if (es == null) es = new HashMap<>();
        es.put(questionId, awardedScore);
        result.setEssayScores(es);

        int total = result.getAutoScore();
        for (int s : es.values()) {
            if (s >= 0) total += s;
        }
        result.setScore(total);
        sm.saveResult(result);
        return "OK";
    }

    public synchronized String publishScores(String examId) {
        Exam exam = getExam(examId);
        if (exam == null) return "考试不存在";
        if (!Exam.FINISHED.equals(exam.getStatus())) return "考试尚未结束，无法发布成绩";

        ScoreManager sm = new ScoreManager();
        List<ExamResult> results = sm.getResultsByExam(examId);
        for (ExamResult r : results) {
            Map<String, Integer> es = r.getEssayScores();
            if (es != null) {
                for (Map.Entry<String, Integer> entry : es.entrySet()) {
                    if (entry.getValue() < 0) {
                        return "尚有未批改的简答题（学生: " + r.getStudentId() + "），请批改完毕后再发布";
                    }
                }
            }
        }

        exam.setScoresPublished(true);
        save();
        return "OK";
    }

    public synchronized Map<String, Object> getSubmissions(String examId) {
        Exam exam = getExam(examId);
        if (exam == null) return null;

        Map<String, Object> data = new HashMap<>();
        data.put("examId", exam.getId());
        data.put("examTitle", exam.getTitle());
        data.put("questions", (java.io.Serializable) new ArrayList<>(exam.getQuestions()));
        data.put("scoresPublished", exam.isScoresPublished());

        Map<String, Map<String, String>> submittedAnswers = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : exam.getStudentAnswers().entrySet()) {
            if (entry.getValue().containsKey("__SUBMITTED__")) {
                submittedAnswers.put(entry.getKey(), new HashMap<>(entry.getValue()));
            }
        }
        data.put("studentAnswers", (java.io.Serializable) submittedAnswers);

        ScoreManager sm = new ScoreManager();
        List<ExamResult> results = sm.getResultsByExam(examId);
        data.put("results", (java.io.Serializable) new ArrayList<>(results));

        return data;
    }

    // ==================== 成绩查询 ====================

    public List<ExamResult> getResultsByExam(String examId) {
        return new ScoreManager().getResultsByExam(examId);
    }

    // ==================== 持久化 ====================

    private void save() {
        FileStorage.save(FileStorage.EXAMS_FILE, exams);
    }
}
