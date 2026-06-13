package com.exam.common.model;

import java.io.Serializable;
import java.util.*;

public class Exam implements Serializable {
    private static final long serialVersionUID = 2L;

    public static final String NOT_STARTED  = "NOT_STARTED";
    public static final String IN_PROGRESS  = "IN_PROGRESS";
    public static final String FINISHED     = "FINISHED";

    private String id;
    private String title;
    private String teacherId;
    private List<Question> questions;
    private int duration;               // 考试时长(秒)
    private long scheduledStartTime;    // 预约开始时间(epoch millis)
    private long actualStartTime;       // 实际开始时间(epoch millis)
    private String status;
    private List<String> joinedStudentIds;
    // studentId -> (questionId -> answer)
    private Map<String, Map<String, String>> studentAnswers;
    private boolean scoresPublished;

    public Exam() {
        this.questions = new ArrayList<>();
        this.joinedStudentIds = new ArrayList<>();
        this.studentAnswers = new HashMap<>();
        this.status = NOT_STARTED;
    }

    public Exam(String id, String title, String teacherId, int duration, long scheduledStartTime) {
        this();
        this.id = id;
        this.title = title;
        this.teacherId = teacherId;
        this.duration = duration;
        this.scheduledStartTime = scheduledStartTime;
    }

    // ---- getters / setters ----
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public long getScheduledStartTime() { return scheduledStartTime; }
    public void setScheduledStartTime(long t) { this.scheduledStartTime = t; }

    public long getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(long t) { this.actualStartTime = t; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getJoinedStudentIds() { return joinedStudentIds; }
    public void setJoinedStudentIds(List<String> v) { this.joinedStudentIds = v; }

    public Map<String, Map<String, String>> getStudentAnswers() { return studentAnswers; }
    public void setStudentAnswers(Map<String, Map<String, String>> v) { this.studentAnswers = v; }

    // ---- 业务方法 ----
    public void addQuestion(Question q) { this.questions.add(q); }

    public int getTotalScore() {
        int total = 0;
        for (Question q : questions) total += q.getScore();
        return total;
    }

    public int getJoinedCount() { return joinedStudentIds.size(); }

    /** 距离考试结束还剩多少秒(用实际开始时间计算) */
    public int getRemainingSeconds() {
        if (NOT_STARTED.equals(status)) return duration;
        if (FINISHED.equals(status)) return 0;
        long elapsed = (System.currentTimeMillis() - actualStartTime) / 1000;
        return (int) Math.max(0, duration - elapsed);
    }

    public void saveAnswer(String studentId, String questionId, String answer) {
        studentAnswers.computeIfAbsent(studentId, k -> new HashMap<>()).put(questionId, answer);
    }

    public Map<String, String> getStudentAnswerMap(String studentId) {
        return studentAnswers.getOrDefault(studentId, new HashMap<>());
    }

    public boolean isScoresPublished() { return scoresPublished; }
    public void setScoresPublished(boolean v) { this.scoresPublished = v; }

    public boolean hasEssayQuestions() {
        for (Question q : questions) {
            if (q instanceof EssayQuestion) return true;
        }
        return false;
    }
}
