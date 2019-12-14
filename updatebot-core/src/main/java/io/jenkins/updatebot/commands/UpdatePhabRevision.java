/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jenkins.updatebot.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.fabric8.utils.Objects;
import io.jenkins.updatebot.CommandNames;
import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.UpdateBot;
import io.jenkins.updatebot.github.GitHubHelpers;
import io.jenkins.updatebot.github.PullRequests;
import io.jenkins.updatebot.model.GitRepository;
import io.jenkins.updatebot.model.PhabRepository;
import io.jenkins.updatebot.model.PhabRevision;
import io.jenkins.updatebot.phab.PhabHelper;
import io.jenkins.updatebot.repository.LocalRepository;
import io.jenkins.updatebot.support.Markdown;
import io.jenkins.updatebot.support.Strings;
import io.jenkins.updatebot.support.Systems;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static io.jenkins.updatebot.EnvironmentVariables.*;
import static io.jenkins.updatebot.github.GitHubHelpers.getLastCommitStatus;
import static io.jenkins.updatebot.github.Issues.getLabels;
import static io.jenkins.updatebot.github.Issues.isOpen;
import static io.jenkins.updatebot.support.Markdown.UPDATEBOT;

/**
 * Updates open Phabricator Revision. Rebases any unmergable PRs or land any diff that are ready.
 */
@Parameters(commandNames = CommandNames.UPDATE_PHAB, commandDescription = "Updates open Phabricator Revision. Rebases any unmergable PRs or land any diff that are ready.")
public class UpdatePhabRevision extends CommandSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(UpdatePhabRevision.class);

    @Override
    protected CommandContext createCommandContext(LocalRepository repository, Configuration configuration) {
        return new UpdatePhabContext(repository, configuration);
    }

    @Override
    public void run(CommandContext context) throws IOException {
        GitRepository repo = context.getRepository().getRepo();

        if (!(repo instanceof PhabRepository)) {
            return;
        }

        UpdatePhabContext phabContext = (UpdatePhabContext) context;
        List<PhabRevision> revisions = phabContext.retrieveRevisions((PhabRepository) repo);

        for (PhabRevision revision : revisions) {
            if (revision.canClose()) {
                context.info(LOG, "Close revision " + revision.getId());
                PhabHelper.closeRevision(context.getConfiguration().getConduitAPIClient(), revision.getId());
                context.info(LOG, "Delete branch " + revision.getBranch());
                context.getGit().deleteBranch(context.getDir(), revision.getBranch());
            } else if (revision.canLand()) {
                context.info(LOG, "Land revision " + revision.getId());
                context.getGit().stashAndCheckoutBranch(context.getDir(), revision.getBranch());
                PhabHelper.landRevision(context.getConfiguration().getConduitAPIClient(), context);
            } else {
                // TODO do command by comment?
            }
        }
    }
}
