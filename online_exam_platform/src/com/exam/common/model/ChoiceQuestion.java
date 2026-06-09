package com.exam.common.model;

import java.util.List;

public class ChoiceQuestion implements Question {
    private static final long serialVersionUID = 1L;

    private String id;
    private String content;
    private int score;
    private String difficulty = BASIC;
    private List<String> options;
    private int correctIndex;

    public ChoiceQuestion() {}

    public ChoiceQuestion(String id, String content, int score, List<String> options, int correctIndex) {
        this.id = id;
        this.content = content;
        this.score = score;
        this.options = options;
        this.correctIndex = correctIndex;
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

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public int getCorrectIndex() { return correctIndex; }
    public void setCorrectIndex(int correctIndex) { this.correctIndex = correctIndex; }

    @Override
    public boolean checkAnswer(String answer) {
        if (answer == null) return false;
        try {
            return Integer.parseInt(answer.trim()) == correctIndex;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
