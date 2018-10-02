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
package io.jenkins.updatebot.github;

import io.fabric8.utils.Objects;
import io.jenkins.updatebot.model.GitRepository;
import io.jenkins.updatebot.model.GithubRepository;
import io.jenkins.updatebot.repository.LocalRepository;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;
import org.kohsuke.github.PagedIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 */
public class GitHubHelpers {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitHubHelpers.class);

    public static void closeOpenUpdateBotIssuesAndPullRequests(String prLabel, List<LocalRepository> repositories) {
        for (LocalRepository repository : repositories) {
            GHRepository ghRepo = GitHubHelpers.getGitHubRepository(repository);
            if (ghRepo != null) {
                try {
                    closePullRequests(PullRequests.getOpenPullRequests(ghRepo, prLabel));
                    closeIssues(Issues.getOpenIssues(ghRepo, prLabel));
                } catch (IOException e) {
                    LOG.warn("Failed to close pending open Pull Requests on " + repository.getCloneUrl());
                }
            }
        }
    }

    public static void closeIssues(List<GHIssue> issues) throws IOException {
        for (GHIssue issue : issues) {
            issue.close();
        }
    }

    public static void closePullRequests(List<GHPullRequest> pullRequests) throws IOException {
        for (GHPullRequest pullRequest : pullRequests) {
            pullRequest.close();
        }
    }

    /**
     * Returns the underlying GitHub repository if this repository is on github
     */
    public static GHRepository getGitHubRepository(LocalRepository repository) {
        GitRepository repo = repository.getRepo();
        if (repo instanceof GithubRepository) {
            GithubRepository githubRepository = (GithubRepository) repo;
            return githubRepository.getRepository();
        }
        return null;
    }

    public static boolean hasLabel(Collection<GHLabel> labels, String label) {
        if (labels != null) {
            for (GHLabel ghLabel : labels) {
                if (Objects.equal(label, ghLabel.getName())) {
                    return true;
                }
            }
        }
        return false;
    }


    public static boolean isMergeable(GHPullRequest pullRequest) throws IOException {
        boolean canMerge = false;
        Boolean mergeable = pullRequest.getMergeable();
        GHPullRequest single = null;
        if (mergeable == null) {
            single = pullRequest.getRepository().getPullRequest(pullRequest.getNumber());
            mergeable = single.getMergeable();
        }
        if (mergeable == null) {
            LOG.warn("Mergeable flag is still null on pull request " + pullRequest.getHtmlUrl() + " assuming its still mergable. Probably a caching issue and this flag may appear again later");
            return true;
        }
        if (mergeable != null && mergeable.booleanValue()) {
            canMerge = true;
        }
        return canMerge;
    }

    public static void deleteUpdateBotBranches(List<LocalRepository> localRepositories) throws IOException {

        for (LocalRepository localRepository : localRepositories) {
            GHRepository ghRepository = getGitHubRepository(localRepository);
            deleteUpdateBotBranches(ghRepository);
        }

    }

    public static void deleteUpdateBotBranches(GHRepository ghRepository) throws IOException {
        if (ghRepository != null) {
            Map<String, GHBranch> branches = ghRepository.getBranches();
            for (GHBranch ghBranch : branches.values()) {
                deleteUpdateBotBranch(ghRepository, ghBranch);
            }
        }
    }

    public static void deleteUpdateBotBranches(GHRepository ghRepository, List<String> branchNames) throws IOException {
        for (String branchName : branchNames) {
            deleteUpdateBotBranch(ghRepository, branchName);
        }
    }

    public static void deleteUpdateBotBranch(GHRepository ghRepository, GHBranch ghBranch) throws IOException {
        deleteUpdateBotBranch(ghRepository, ghBranch.getName());

    }

    public static void deleteUpdateBotBranch(GHRepository ghRepository, String branchName) throws IOException {
        if (branchName.startsWith("updatebot-")) {
            //delete as per https://github.com/kohsuke/github-api/pull/164#issuecomment-78391771
            //heads needed as per https://developer.github.com/v3/git/refs/#get-a-reference
            ghRepository.getRef("heads/" + branchName).delete();
            LOG.info("deleted branch " + branchName + " for " + ghRepository.getFullName());
        }

    }

    public static GHPerson getOrganisationOrUser(GitHub github, String orgName) {
        GHPerson person = null;
        try {
            person = github.getOrganization(orgName);
        } catch (IOException e) {
        }
        if (person == null) {
            try {
                person = github.getUser(orgName);
            } catch (IOException e) {
                LOG.warn("Could not find organisation or user for " + orgName + ". " + e, e);
            }
        }
        return person;
    }

    /**
     * Make sure all the statuses reported on the commit match the expected, at least for first page
     */
    public static boolean checkCommitStatus(GHRepository repository, GHPullRequest pullRequest, GHCommitState expectedStatus) throws IOException {
        boolean statusesMatch = true;
        PagedIterable<GHCommitStatus> statuses = repository.getCommit(pullRequest.getHead().getSha()).listStatuses();

        // note lets assume that the first status for a given target URL is the current value
        // as we get a Success followed by a number of Pending statuses for a given URL
        Map<String, GHCommitState> targetUrlToState = new HashMap<>();

        int count = 0;
        if (statuses != null) {
            Iterator<GHCommitStatus> iterator = statuses.iterator();

            while (iterator != null && iterator.hasNext()) {
                GHCommitStatus status = iterator.next();
                GHCommitState state = status.getState();
                String key = status.getTargetUrl();
                if (key == null) {
                    continue;
                }
                count++;
                GHCommitState previous = targetUrlToState.get(key);
                if (previous != null) {
                    LOG.info("Ignoring subsequent state " + state + " for targetUrl " + key + " as we first got a state of " + previous);
                    continue;
                }
                if (state != null) {
                    targetUrlToState.put(key, state);
                }
                if (status == null || !state.equals(expectedStatus)) {
                    statusesMatch = false;
                }
            }
        }
        return statusesMatch && count > 0;
    }

    public static GHCommitStatus getLastCommitStatus(GHRepository repository, GHPullRequest pullRequest) throws IOException {
        String commitSha = pullRequest.getHead().getRef();
        return repository.getLastCommitStatus(commitSha);
    }

    public static <T> T retryGithub(Callable<T> callable) throws IOException {
        return retryGithub(callable, 5, 1000);
    }

    /**
     * Allow automatic retries when timeout exceptions happen
     */
    public static <T> T retryGithub(Callable<T> callable, int retries, long timeout) throws IOException {
        for (int i = 0; i < retries; i++) {
            if (i > 0) {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                return callable.call();
            } catch (HttpException e) {
                int code = e.getResponseCode();
                LOG.warn("GitHub Operation returned response " + code + " so retrying. Exception " + e, e);
                if (code >= 100) {
                    throw e;
                }
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        return null;
    }

    public static Boolean waitForPullRequestToHaveMergable(GHPullRequest pullRequest, long sleepMS, long maximumTimeMS) throws IOException {
        long end = System.currentTimeMillis() + maximumTimeMS;
        while (true) {
            Boolean mergeable = pullRequest.getMergeable();
            if (mergeable == null) {
                GHRepository repository = pullRequest.getRepository();
                int number = pullRequest.getNumber();
                pullRequest = repository.getPullRequest(number);
                mergeable = pullRequest.getMergeable();
            }
            if (mergeable != null) {
                return mergeable;
            }
            if (System.currentTimeMillis() > end) {
                return null;
            }
            try {
                Thread.sleep(sleepMS);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
