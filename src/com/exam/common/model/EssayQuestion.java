package com.exam.common.model;

public class EssayQuestion implements Question {
    private static final long serialVersionUID = 1L;

    private String id;
    private String content;
    private int score;
    private String difficulty = BASIC;
    private String referenceAnswer;
    private String explanation;

    public EssayQuestion() {}

    public EssayQuestion(String id, String content, int score, String referenceAnswer) {
        this.id = id;
        this.content = content;
        this.score = score;
        this.referenceAnswer = referenceAnswer;
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

    public String getReferenceAnswer() { return referenceAnswer; }
    public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }

    @Override public String getExplanation() { return explanation; }
    @Override public void setExplanation(String explanation) { this.explanation = explanation; }

    @Override
    public boolean checkAnswer(String answer) {
        return false;
    }
}
