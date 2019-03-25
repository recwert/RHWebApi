package com.zhourh.rhwebapi;

import java.io.Serializable;

/**
 * @author hugu
 */
public class CommonQuestionDO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long questionId;

    private String question;

    private String answer;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(long questionId) {
        this.questionId = questionId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
