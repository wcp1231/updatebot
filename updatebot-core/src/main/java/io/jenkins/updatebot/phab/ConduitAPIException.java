package io.jenkins.updatebot.phab;

import java.io.IOException;

public class ConduitAPIException extends IOException {

    public final int code;

    public ConduitAPIException(String message) {
        super(message);
        this.code = 0;
    }

    public ConduitAPIException(String message, int code) {
        super(message);
        this.code = code;
    }
}