package com.exam.common.model;

public class BlankQuestion implements Question {
    private static final long serialVersionUID = 1L;

    private String id;
    private String content;
    private int score;
    private String difficulty = BASIC;
    private String correctAnswer;

    public BlankQuestion() {}

    public BlankQuestion(String id, String content, int score, String correctAnswer) {
        this.id = id;
        this.content = content;
        this.score = score;
        this.correctAnswer = correctAnswer;
    }

    @Override public String getDifficulty() { return difficulty; }
    @Override public void setDifficulty(String d) { this.difficulty = d; }

    @Override
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @Override
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    @Override
    public boolean checkAnswer(String answer) {
        if (answer == null) return false;
        return answer.trim().equalsIgnoreCase(correctAnswer.trim());
    }
}
