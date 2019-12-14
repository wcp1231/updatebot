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
package io.jenkins.updatebot.repository;

import io.fabric8.utils.Filter;
import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.github.GitHubHelpers;
import io.jenkins.updatebot.model.*;
import io.jenkins.updatebot.phab.ConduitAPIClient;
import io.jenkins.updatebot.phab.PhabHelper;
import io.jenkins.updatebot.support.FileHelper;
import io.jenkins.updatebot.support.Strings;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 */
public class Repositories {
    private static final transient Logger LOG = LoggerFactory.getLogger(Repositories.class);


    public static List<LocalRepository> cloneOrPullRepositories(Configuration configuration, RepositoryConfig repositoryConfig) throws IOException {
        List<LocalRepository> repositories = findRepositories(configuration, repositoryConfig);
        for (LocalRepository repository : repositories) {
            cloneOrPullRepository(configuration, repository);
        }
        return repositories;
    }

    public static void cloneOrPullRepository(Configuration configuration, LocalRepository repository) {
        File dir = repository.getDir();
        String secureCloneUrl = repository.getRepo().secureCloneUrl(configuration);
        File gitDir = new File(dir, ".git");

        if (gitDir.exists()) {
            // Let's resolve clone branch from local repository
            String branch = repository.resolveRemoteBranch();

            configuration.info(LOG, "Checkout branch: " + branch + " from " + repository.getFullName() + " in " + FileHelper.getRelativePathToCurrentDir(dir));
            if (configuration.getGit().stashAndCheckoutBranch(dir, branch)) {
                if (!configuration.isPullDisabled()) {
                    configuration.info(LOG, "Pull branch: " + branch + " from " + repository.getFullName() + " in " + FileHelper.getRelativePathToCurrentDir(dir));
                    configuration.getGit().pull(dir, repository.getCloneUrl());
                }
            }
            configuration.getGit().configUserNameAndEmail(dir);
        } else {
            File parentDir = dir.getParentFile();
            parentDir.mkdirs();

            configuration.info(LOG, "Cloning: " + repository.getFullName() + " to " + FileHelper.getRelativePathToCurrentDir(dir));
            configuration.getGit().clone(parentDir, secureCloneUrl, dir.getName());

            configuration.getGit().configUserNameAndEmail(dir);
        }
    }

    protected static List<LocalRepository> findRepositories(Configuration configuration, RepositoryConfig repositoryConfig) throws IOException {
        String workDirPath = configuration.getWorkDir();
        File workDir = new File(workDirPath);
        if (!workDir.isAbsolute()) {
            workDir = new File(workDir.getCanonicalPath());
        }
        workDir.mkdirs();

        Map<String, LocalRepository> map = new LinkedHashMap<>();
        File gitHubDir = new File(workDir, "github");
        File gitDir = new File(workDir, "git");

        GitHubProjects githubProjects = repositoryConfig.getGithub();
        if (githubProjects != null) {
            List<GithubOrganisation> organisations = githubProjects.getOrganisations();
            if (organisations != null && !organisations.isEmpty()) {
                GitHub github = configuration.getGithub();
                for (GithubOrganisation organisation : organisations) {
                    if (organisation != null) {
                        String name = organisation.getName();
                        if (name != null) {
                            addGitHubRepositories(configuration, map, github, organisation, new File(gitHubDir, name));
                        } else {
                            LOG.warn("Organisation has no name! " + organisation);
                        }
                    }
                }
            }
        }
        List<GitRepository> gitRepositories = repositoryConfig.getGit();
        if (gitRepositories != null) {
            for (GitRepository gitRepository : gitRepositories) {
                addRepository(configuration, map, gitDir, gitRepository);
            }
        }
        List<String> projectTags = repositoryConfig.getPhabTags();
        if (projectTags != null) {
            ConduitAPIClient client = configuration.getConduitAPIClient();
            String phabHost = configuration.getPhabHost();
            List<PhabRepository> repositories = PhabHelper.repositorySearch(client, phabHost, projectTags);
            for (PhabRepository repository : repositories) {
                LocalRepository localRepository = new LocalRepository(repository, new File(gitDir, repository.getName()));
                map.putIfAbsent(localRepository.getCloneUrl(), localRepository);
            }
        }
        return new ArrayList<>(map.values());
    }

    protected static void addRepository(Configuration configuration, Map<String, LocalRepository> map, File gitDir, GitRepository gitRepository) {
        if (configuration.isIgnoreExcludeUpdateLoopRepositories() && gitRepository.isExcludeUpdateLoop()) {
            LOG.info("Ignoring repository " + gitRepository.getFullName() + " as it configured to be excluded from the update-loop");
            return;
        }
        LOG.info("repository " + gitRepository.getFullName() + " has excludeUpdateLoop: " + gitRepository.getExcludeUpdateLoop());
        LocalRepository localRepository = new LocalRepository(gitRepository, new File(gitDir, gitRepository.getName()));
        map.putIfAbsent(localRepository.getCloneUrl(), localRepository);
    }

    protected static void addGitHubRepositories(Configuration configuration, Map<String, LocalRepository> map, GitHub github, GithubOrganisation organisation, File file) {
        String orgName = organisation.getName();
        Filter<String> filter = organisation.createFilter();

        GHPerson person = GitHubHelpers.getOrganisationOrUser(github, orgName);
        if (person != null) {
            try {
                Set<String> foundNames = new TreeSet<>();
                List<GitRepositoryConfig> namedRepositories = organisation.getRepositories();
                if (namedRepositories != null) {
                    for (GitRepositoryConfig namedRepository : namedRepositories) {
                        String name = namedRepository.getName();
                        if (Strings.notEmpty(name) && foundNames.add(name)) {
                            GHRepository ghRepository = null;
                            try {
                                ghRepository = person.getRepository(name);
                            } catch (IOException e) {
                                LOG.warn("Github repository " + orgName + "/" + name + " not found: " + e);
                                continue;
                            }
                            if (ghRepository != null) {
                                GitRepository gitRepository = new GithubRepository(ghRepository, namedRepository);
                                addRepository(configuration, map, file, gitRepository);
                            } else {
                                LOG.warn("Github repository " + orgName + "/" + name + " not found!");
                            }
                        }
                    }
                }
                Map<String, GHRepository> repositories = person.getRepositories();
                for (Map.Entry<String, GHRepository> entry : repositories.entrySet()) {
                    String repoName = entry.getKey();
                    if (filter.matches(repoName) && foundNames.add(repoName)) {
                        GitRepository gitRepository = new GithubRepository(entry.getValue());
                        addRepository(configuration, map, file, gitRepository);
                    }
                }
            } catch (IOException e) {
                LOG.warn("Failed to load organisation: " + orgName + ". " + e, e);
            }
        }
    }


}
