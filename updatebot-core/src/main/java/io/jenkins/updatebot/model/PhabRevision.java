package io.jenkins.updatebot.model;

import java.util.Arrays;
import java.util.List;

public class PhabRevision {
    private static List<String> STATUS_CLOSE = Arrays.asList(
            "5", // Changes Planned
            "1" // Needs Revision
    );
    private static String STATUS_ACCEPT = "2"; // Accepted

    private String id;
    private String uri;
    private String status;
    private String statusName;
    private String branch;
    private String repositoryPHID;

    public PhabRevision(String id, String uri, String status, String statusName, String branch, String repositoryPHID) {
        this.id = id;
        this.uri = uri;
        this.status = status;
        this.statusName = statusName;
        this.branch = branch;
        this.repositoryPHID = repositoryPHID;
    }

    public String getId() {
        return id;
    }

    public String getBranch() {
        return branch;
    }

    public String getRepositoryPHID() {
        return repositoryPHID;
    }

    public boolean canClose() {
        return STATUS_CLOSE.contains(status);
    }

    public boolean canLand() {
        return STATUS_ACCEPT.equals(status);
    }
}
