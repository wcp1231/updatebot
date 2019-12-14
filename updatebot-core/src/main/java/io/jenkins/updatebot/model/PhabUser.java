package io.jenkins.updatebot.model;

public class PhabUser {
    private String phid;
    private String username;
    private String email;

    public PhabUser(String phid, String username, String email) {
        this.phid = phid;
        this.username = username;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}
