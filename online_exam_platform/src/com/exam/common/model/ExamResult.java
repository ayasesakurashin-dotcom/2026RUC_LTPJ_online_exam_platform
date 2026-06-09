package com.exam.common.model;

import java.io.Serializable;
import java.util.Map;

public class ExamResult implements Serializable {
    private static final long serialVersionUID = 2L;

    private String examId;
    private String examTitle;
    private String studentId;
    private int score;
    private int totalScore;
    private Map<String, String> answerMap;
    private long submitTime;

    // 按难度分类统计
    private int basicCorrect,    basicTotal;
    private int mediumCorrect,   mediumTotal;
    private int advancedCorrect, advancedTotal;

    public ExamResult() {}

    public ExamResult(String examId, String examTitle, String studentId, int score, int totalScore,
                      Map<String, String> answerMap, long submitTime) {
        this.examId = examId;
        this.examTitle = examTitle;
        this.studentId = studentId;
        this.score = score;
        this.totalScore = totalScore;
        this.answerMap = answerMap;
        this.submitTime = submitTime;
    }

    public String getExamId() { return examId; }
    public void setExamId(String examId) { this.examId = examId; }

    public String getExamTitle() { return examTitle; }
    public void setExamTitle(String examTitle) { this.examTitle = examTitle; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public Map<String, String> getAnswerMap() { return answerMap; }
    public void setAnswerMap(Map<String, String> answerMap) { this.answerMap = answerMap; }

    public long getSubmitTime() { return submitTime; }
    public void setSubmitTime(long submitTime) { this.submitTime = submitTime; }

    public int getBasicCorrect()    { return basicCorrect; }
    public void setBasicCorrect(int v) { this.basicCorrect = v; }
    public int getBasicTotal()      { return basicTotal; }
    public void setBasicTotal(int v)   { this.basicTotal = v; }

    public int getMediumCorrect()   { return mediumCorrect; }
    public void setMediumCorrect(int v) { this.mediumCorrect = v; }
    public int getMediumTotal()     { return mediumTotal; }
    public void setMediumTotal(int v)   { this.mediumTotal = v; }

    public int getAdvancedCorrect() { return advancedCorrect; }
    public void setAdvancedCorrect(int v) { this.advancedCorrect = v; }
    public int getAdvancedTotal()   { return advancedTotal; }
    public void setAdvancedTotal(int v)   { this.advancedTotal = v; }
}
