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

import static io.jenkins.updatebot.Configuration.DEFAULT_CONFIG_FILE;

import io.fabric8.utils.Files;
import io.fabric8.utils.Objects;
import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.github.GitHubHelpers;
import io.jenkins.updatebot.model.GitRepository;
import io.jenkins.updatebot.model.GitRepositoryConfig;
import io.jenkins.updatebot.model.GithubRepository;
import io.jenkins.updatebot.model.RepositoryConfig;
import io.jenkins.updatebot.model.RepositoryConfigs;
import io.jenkins.updatebot.support.Strings;

import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 */
public class LocalRepository {
    private static final transient Logger LOG = LoggerFactory.getLogger(LocalRepository.class);

    private GitRepository repo;
    private File dir;

    public LocalRepository(GitRepository repo, File dir) {
        this.repo = repo;
        this.dir = dir;
    }

    /**
     * Returns a local repository from a directory.
     */
    public static LocalRepository fromDirectory(Configuration configuration, File dir) throws IOException {
        LocalRepository localRepository = new LocalRepository(new GitRepository(dir.getName()), dir);
        File configFile = new File(dir, DEFAULT_CONFIG_FILE);
        if (Files.isFile(configFile)) {
            RepositoryConfig config = RepositoryConfigs.loadRepositoryConfig(configuration, DEFAULT_CONFIG_FILE, dir);
            if (config != null) {
                GitRepositoryConfig local = config.getLocal();
                if (local != null) {
                    localRepository.getRepo().setRepositoryDetails(local);
                }
            }
        }
        return localRepository;
    }

    /**
     * Returns the repository for the given name or null if it could not be found
     */
    public static LocalRepository findRepository(List<LocalRepository> localRepositories, String name) {
        if (localRepositories != null) {
            for (LocalRepository repository : localRepositories) {
                GitRepository repo = repository.getRepo();
                if (repo != null) {
                    if (Objects.equal(name, repo.getName())) {
                        return repository;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the repository for the given repo
     */
    public static LocalRepository findRepository(List<LocalRepository> localRepositories, GitRepository gitRepository) {
        if (localRepositories != null) {
            for (LocalRepository repository : localRepositories) {
                GitRepository repo = repository.getRepo();
                if (repo != null) {
                    if (Objects.equal(repo.getCloneUrl(), gitRepository.getCloneUrl())) {
                        return repository;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the local repository for the given GHRepository with matching transport url
     */
    public static LocalRepository findRepository(List<LocalRepository> localRepositories, GHRepository ghRepository) {
        if (localRepositories != null) {
            for (LocalRepository repository : localRepositories) {
                GitRepository repo = repository.getRepo();
                if (repo != null) {
                    if (Objects.equal(repo.getCloneUrl(), ghRepository.getGitTransportUrl())) {
                        return repository;
                    }
                }
            }
        }
        return null;
    }


    /**
     * Returns the link to the repository
     */
    public static String getRepositoryLink(LocalRepository repository) {
        return getRepositoryLink(repository, repository.getFullName());
    }

    /**
     * Returns the link to the repository
     */
    public static String getRepositoryLink(LocalRepository repository, String label) {
        return getRepositoryLink(repository, label, "`" + label + "`");
    }

    /**
     * Returns the link to the repository
     */
    public static String getRepositoryLink(LocalRepository repository, String label, String defaultValue) {
        if (repository != null) {
            String htmlUrl = repository.getRepo().getHtmlUrl();
            if (Strings.notEmpty(htmlUrl)) {
                return "[" + label + "](" + htmlUrl + ")";
            }
        }
        if (Strings.notEmpty(defaultValue)) {
            return defaultValue;
        }
        return label;
    }

    @Override
    public String toString() {
        return "LocalRepository{" +
                "repo=" + repo +
                ", dir=" + dir +
                '}';
    }

    public String getFullName() {
        return repo.getFullName();
    }

    public GitRepository getRepo() {
        return repo;
    }

    public File getDir() {
        return dir;
    }

    public String getCloneUrl() {
        return repo.getCloneUrl();
    }

    /**
     * Returns true if this repository can be cloned using the given URL
     */
    public boolean hasCloneUrl(String cloneUrl) {
        // sometimes folks miss off the ".git" from URLs so lets check for that too
        return repo.hasCloneUrl(cloneUrl) || repo.hasCloneUrl(cloneUrl + ".git");
    }

    /**
     * Resolves remote branch name at runtime using a combination of .updatebot.yml setting, or Github default branch,
     * or master as fallback. If branch is not set, tries to resolve default branch name from remote Github repository
     * in order to create PRs with updates using the same branch.
     *
     * Returns remote branch name to use
     *
     */
    public String resolveRemoteBranch() {
        // Let's try use repository branch from .updatebot.yml first
        GitRepositoryConfig config = repo.getRepositoryDetails();
        if (config == null) {
            LOG.warn("The repo has no config!");
            return "master";
        }
        if(!Strings.empty(config.getBranch())) {
            return config.getBranch();
        } // Try detect Github repository and use its default branch
        else if(repo instanceof GithubRepository) {
            GHRepository ghRepository = GitHubHelpers.getGitHubRepository(this);
            if (ghRepository != null) {
                config.setBranch(ghRepository.getDefaultBranch());
            }
        }

        // Fallback to master branch for Git repositories
        if(Strings.empty(config.getBranch())) {
            config.setBranch("master");
        }

        return config.getBranch();
    }


    /**
     * Returns configured remote branch from repository configuration details
     */
    public String getRemoteBranch() {
        GitRepositoryConfig config = repo.getRepositoryDetails();
        if (config == null) {
            LOG.warn("The repo has no config!");
            return "master";
        }
        return config.getBranch();
    }

    /**
     * Returns true if the repository is configured to use single pull request to add a new version push commit
     */
    public boolean isUseSinglePullRequest() {
        GitRepositoryConfig config = repo.getRepositoryDetails();
        if (config == null) {
            return false;
        }

        return config.isUseSinglePullRequest();
    }
}
