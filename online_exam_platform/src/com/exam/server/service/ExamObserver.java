package com.exam.server.service;

import com.exam.common.model.Exam;

public interface ExamObserver {
    void onExamStarted(Exam exam);
    void onTimeSync(String examId, int remainingSeconds);
    void onExamFinished(String examId);
}
