package com.github.uchan_nos.c_helper.questionnaire;

public class Question {

    private String question;
    private String[] options;

    public Question(String question, String... options) {
        this.question = question;
        this.options = options;
    }

    public String getQuestion() {
        return this.question;
    }

    public String[] getOptions() {
        return this.options;
    }
}
