package io.jenkins.updatebot.commands;

import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.model.PhabRepository;
import io.jenkins.updatebot.model.PhabRevision;
import io.jenkins.updatebot.phab.PhabHelper;
import io.jenkins.updatebot.repository.LocalRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpdatePhabContext extends CommandContext {
    private static Map<String, List<PhabRevision>> CACHED_REVISION;

    private Configuration configuration;

    public UpdatePhabContext(LocalRepository repository, Configuration configuration) {
        super(repository, configuration);
        this.configuration = configuration;
    }

    public List<PhabRevision> retrieveRevisions(PhabRepository repository) throws IOException {
        String repositoryPHID = repository.getPhid();

        if (CACHED_REVISION == null) {
            CACHED_REVISION = PhabHelper.queryOpenRevisions(configuration.getConduitAPIClient());
        }

        return CACHED_REVISION.getOrDefault(repositoryPHID, new ArrayList<>());
    }
}
