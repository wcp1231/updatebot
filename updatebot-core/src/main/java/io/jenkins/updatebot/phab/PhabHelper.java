package io.jenkins.updatebot.phab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jenkins.updatebot.commands.CommandContext;
import io.jenkins.updatebot.model.PhabRepository;
import io.jenkins.updatebot.model.PhabUser;
import io.jenkins.updatebot.support.ProcessHelper;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhabHelper {
    public static List<PhabRepository> repositorySearch(ConduitAPIClient client, String phabHost, List<String> tags) throws IOException {
        ObjectNode params = ConduitAPIClient.OBJECT_MAPPER.createObjectNode();
        ArrayNode projects = params.with("constraints").withArray("projects");
        tags.forEach(projects::add);

        List<PhabRepository> repositories = new ArrayList<>();

        JsonNode response = client.perform("diffusion.repository.search", params);
        repositories.addAll(parseRepositories(response, phabHost));
        int after = getNextRepositoryCursor(response);
        while (after != 0) {
            params.put("after", after);
            response = client.perform("diffusion.repository.search", params);
            repositories.addAll(parseRepositories(response, phabHost));
            after = getNextRepositoryCursor(response);
        }
        return repositories;
    }

    public static PhabUser whoami(ConduitAPIClient client) throws IOException {
        ObjectNode params = ConduitAPIClient.OBJECT_MAPPER.createObjectNode();
        JsonNode response = client.perform("user.whoami", params);
        JsonNode result = response.with("result");
        String phid = result.get("phid").asText();
        String username = result.get("userName").asText();
        String email = result.get("primaryEmail").asText();
        return new PhabUser(phid, username, email);
    }

    public static String createRevision(ConduitAPIClient client, CommandContext context) throws IOException {
        //ProcessHelper.runCommandAndLogOutput(context.getConfiguration(), LOG, context.getDir(), "");
        String output = ProcessHelper.runCommandCaptureOutput(context.getDir(),
                "arc", "diff", "master", "--nolint", "--nounit",
                "--verbatim", "--excuse", "autofix", "--conduit-token", client.getConduitToken());
        Matcher matcher = Pattern.compile("Revision URI: (.*)", Pattern.MULTILINE).matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "NULL";
    }

    private static int getNextRepositoryCursor(JsonNode response) {
        String after = response.with("result").with("cursor").get("after").asText();
        if (StringUtils.isEmpty(after) || StringUtils.equals(after, "null")) {
            return 0;
        }
        return Integer.valueOf(after);
    }

    private static List<PhabRepository> parseRepositories(JsonNode response, String phabHost) {
        List<PhabRepository> repositories = new ArrayList<>();
        Iterator<JsonNode> projects = response.with("result").withArray("data").elements();
        while (projects.hasNext()) {
            JsonNode project = projects.next();
            String phid = project.get("phid").asText();
            String callsign = project.with("fields").get("callsign").asText();
            repositories.add(new PhabRepository(phid, callsign, phabHost));
        }
        return repositories;
    }
}
