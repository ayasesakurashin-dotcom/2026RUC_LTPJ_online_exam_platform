package com.exam.common.util;

import com.exam.common.model.BlankQuestion;
import com.exam.common.model.ChoiceQuestion;
import com.exam.common.model.EssayQuestion;
import com.exam.common.model.Question;

import java.util.List;
import java.util.UUID;

public class QuestionFactory {

    public static Question createChoice(String content, int score, String difficulty,
                                         List<String> options, int correctIndex) {
        Question q = new ChoiceQuestion(UUID.randomUUID().toString().substring(0, 8),
                content, score, options, correctIndex);
        q.setDifficulty(difficulty);
        return q;
    }

    public static Question createBlank(String content, int score, String difficulty,
                                        String correctAnswer) {
        Question q = new BlankQuestion(UUID.randomUUID().toString().substring(0, 8),
                content, score, correctAnswer);
        q.setDifficulty(difficulty);
        return q;
    }

    public static Question createEssay(String content, int score, String difficulty,
                                        String referenceAnswer) {
        Question q = new EssayQuestion(UUID.randomUUID().toString().substring(0, 8),
                content, score, referenceAnswer);
        q.setDifficulty(difficulty);
        return q;
    }
}
