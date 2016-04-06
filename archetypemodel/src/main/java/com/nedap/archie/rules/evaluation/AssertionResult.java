package com.nedap.archie.rules.evaluation;

import com.nedap.archie.rules.Expression;

/**
 * Created by pieter.bos on 04/04/16.
 */
public class AssertionResult {

    private String tag;
    private Expression assertion;
    /**
     * The raw result: Did all the separate checks for this assertion pass?
     */
    private Value rawResult;
    /**
     * The result: did this assertion pass?
     */
    private boolean result;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Expression getAssertion() {
        return assertion;
    }

    public void setAssertion(Expression assertion) {
        this.assertion = assertion;
    }

    public Value getRawResult() {
        return rawResult;
    }

    public void setRawResult(Value rawResult) {
        this.rawResult = rawResult;
    }

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }
}
