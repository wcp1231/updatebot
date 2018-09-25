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
import io.jenkins.updatebot.CommandNames;
import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.support.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.jenkins.updatebot.EnvironmentVariables.POLL_PERIOD;
import static io.jenkins.updatebot.EnvironmentVariables.POLL_TIMEOUT;
import static io.jenkins.updatebot.commands.StatusInfo.isPending;

/**
 * A loop to keep updating Pull Requests until they all merge
 */
@Parameters(commandNames = CommandNames.UPDATE_LOOP, commandDescription = "A loop which waits for the updatebot PRs to be merged")
public class UpdatePullRequestLoop extends CommandSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(UpdatePullRequestLoop.class);

    @Parameter(names = "--merge", description = "Whether we should merge Pull Requests that are Open and have a successful last commit status", arity = 1)
    private boolean mergeOnSuccess = true;

    @Parameter(names = "--check-pr-status", description = "Whether we should check the status of Pull Requests before merging them", arity = 1)
    private boolean checkPrStatus = true;

    @Parameter(names = "--poll-time-ms", description = "The poll period", arity = 1)
    private long pollTimeMillis = Systems.getConfigLongValue(POLL_PERIOD, 2 * 60 * 1000);

    @Parameter(names = "--loop-time-ms", description = "The maximum amount of time to wait for the Pull Requests to be ready to merge before terminating.", arity = 1)
    private long loopTime = Systems.getConfigLongValue(POLL_TIMEOUT, 60 * 60 * 1000);


    @Override
    public ParentContext run(Configuration configuration) throws IOException {
        validateConfiguration(configuration);

        ParentContext parentContext = new ParentContext();

        UpdatePullRequests updatePullRequests = createUpdatePullRequestsCommand();

        Map<String, StatusInfo> lastStatusMap = new LinkedHashMap();
        long start = System.currentTimeMillis();
        long end = start + loopTime;
        while (true) {
            Map<String, StatusInfo> currentStatusMap = new LinkedHashMap();

            ParentContext context = updatePullRequests.run(configuration);
            List<CommandContext> children = context.getChildren();
            for (CommandContext child : children) {
                StatusInfo status = child.createStatusInfo();
                currentStatusMap.put(status.getCloneUrl(), status);
            }

            // lets get the previous state and compare them then log the differences
            Collection<StatusInfo> changes;
            boolean logBlankLineAfter = false;
            if (lastStatusMap.isEmpty() && !currentStatusMap.isEmpty()) {
                changes = currentStatusMap.values();
                configuration.info(LOG, "");
                configuration.info(LOG, "");
                logBlankLineAfter = true;
            } else {
                changes = StatusInfo.changedStatuses(configuration, lastStatusMap, currentStatusMap).values();
            }
            for (StatusInfo change : changes) {
                configuration.info(LOG, change.description(configuration));
            }
            if (logBlankLineAfter) {
                configuration.info(LOG, "");
            }
            lastStatusMap = currentStatusMap;

            if (!isPending(lastStatusMap)) {
                LOG.info("UpdateBot update-loop is complete!");
                return parentContext;
            }

            if (loopTime > 0 && System.currentTimeMillis() > end) {
                LOG.info("UpdateBot has reached the end of its loop time and is terminating with pending Pull Requests");
                for (StatusInfo statusInfo : currentStatusMap.values()) {
                    if (statusInfo.isPending()) {
                        configuration.info(LOG, statusInfo.description(configuration));
                    }
                }
                return parentContext;
            }
            try {
                Thread.sleep(pollTimeMillis);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    @Override
    public void run(CommandContext context) throws IOException {
        throw new IllegalArgumentException("This method should never be invoked!");
    }

    protected UpdatePullRequests createUpdatePullRequestsCommand() {
        UpdatePullRequests answer = new UpdatePullRequests();
        answer.setMergeOnSuccess(mergeOnSuccess);
        return answer;
    }

}
