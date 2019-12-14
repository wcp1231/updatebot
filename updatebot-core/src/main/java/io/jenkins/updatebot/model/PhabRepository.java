package io.jenkins.updatebot.model;

public class PhabRepository extends GitRepository {
    private String phid;
    private String callsign;

    public PhabRepository(String phid, String callsign, String phabHost) {
        this.phid = phid;
        this.callsign = callsign;
        setHtmlUrl("https://" + phabHost + "/diffusion/" + callsign);
        setName(callsign);
        setCloneUrl("ssh://git@" + phabHost + "/diffusion/" + callsign);
    }

    public String getPhid() {
        return phid;
    }

    @Override
    public String toString() {
        return "PhabRepository{" +
                "callsign='" + callsign + '\'' +
                '}';
    }
}
