package com.exam.common.model;

import java.io.Serializable;

public interface Question extends Serializable {
    String BASIC   = "BASIC";
    String MEDIUM  = "MEDIUM";
    String ADVANCED = "ADVANCED";

    String getId();
    String getContent();
    int getScore();
    String getDifficulty();
    void setDifficulty(String d);
    boolean checkAnswer(String answer);
    String getExplanation();
    void setExplanation(String explanation);
}
