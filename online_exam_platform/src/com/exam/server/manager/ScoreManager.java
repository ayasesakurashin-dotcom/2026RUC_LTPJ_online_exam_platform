package com.exam.server.manager;

import com.exam.common.model.ExamResult;
import com.exam.server.storage.FileStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScoreManager {

    private List<ExamResult> results;

    public ScoreManager() {
        results = FileStorage.load(FileStorage.SCORES_FILE);
    }

    public synchronized void saveResult(ExamResult result) {
        // 覆盖同一学生对同一考试的已有成绩
        results.removeIf(r -> r.getExamId().equals(result.getExamId())
                && r.getStudentId().equals(result.getStudentId()));
        results.add(result);
        save();
    }

    public synchronized List<ExamResult> getResultsByStudent(String studentId) {
        return results.stream()
                .filter(r -> r.getStudentId().equals(studentId))
                .collect(Collectors.toList());
    }

    public synchronized List<ExamResult> getResultsByExam(String examId) {
        return results.stream()
                .filter(r -> r.getExamId().equals(examId))
                .collect(Collectors.toList());
    }

    public synchronized List<ExamResult> getAllResults() {
        return new ArrayList<>(results);
    }

    private void save() {
        FileStorage.save(FileStorage.SCORES_FILE, results);
    }
}
