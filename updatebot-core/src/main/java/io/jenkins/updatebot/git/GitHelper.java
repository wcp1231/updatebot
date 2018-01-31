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
package io.jenkins.updatebot.git;

import io.fabric8.utils.Files;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Strings;
import io.jenkins.updatebot.support.UserPassword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
public class GitHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitHelper.class);

    public static List<String> getGitHubCloneUrls(String gitHubHost, String orgName, String repository) {
        List<String> answer = new ArrayList<>();
        answer.add("https://" + gitHubHost + "/" + orgName + "/" + repository);
        answer.add("git@" + gitHubHost + ":" + orgName + "/" + repository);
        answer.add("git://" + gitHubHost + "/" + orgName + "/" + repository);

        // now lets add the .git versions
        List<String> copy = new ArrayList<>(answer);
        for (String url : copy) {
            if (!url.endsWith(".git")) {
                answer.add(url + ".git");
            }
        }
        return answer;
    }

    /**
     * Parses the git URL string and determines the host and organisation string
     */
    public static GitRepositoryInfo parseGitRepositoryInfo(String gitUrl) {
        if (Strings.isNullOrBlank(gitUrl)) {
            return null;
        }
        try {
            URI url = new URI(gitUrl);
            String host = url.getHost();
            String userInfo = url.getUserInfo();
            String path = url.getPath();
            path = stripSlashesAndGit(path);
            if (Strings.notEmpty(userInfo)) {
                return new GitRepositoryInfo(host, userInfo, path);
            } else {
                return parseRepository(path, host);
            }
        } catch (URISyntaxException e) {
            // ignore
        }
        String prefix = "git@";
        if (gitUrl.startsWith(prefix)) {
            String path = Strings.stripPrefix(gitUrl, prefix);
            path = stripSlashesAndGit(path);
            String[] paths = path.split(":|/", 3);
            if (paths.length == 3) {
                return new GitRepositoryInfo(paths[0], paths[1], paths[2]);
            }
        }
        return null;
    }

    /**
     * Returns the repository string split its organisation and name
     */
    public static GitRepositoryInfo parseRepository(String orgAndRepoName) {
        return parseRepository(orgAndRepoName, null);
    }

    /**
     * Returns the repository string split its organisation and name
     */
    public static GitRepositoryInfo parseRepository(String orgAndRepoName, String host) {
        if (Strings.notEmpty(orgAndRepoName)) {
            String[] paths = orgAndRepoName.split("/", 2);
            if (paths.length > 1) {
                return new GitRepositoryInfo(host, paths[0], paths[1]);
            }
        }
        return null;
    }

    protected static String stripSlashesAndGit(String path) {
        path = Strings.stripPrefix(path, "/");
        path = Strings.stripPrefix(path, "/");
        path = Strings.stripSuffix(path, "/");
        path = Strings.stripSuffix(path, ".git");
        return path;
    }

    /**
     * Returns the clone URL without a user or password
     */
    public static String removeUsernamePassword(String cloneUrl) {
        try {
            URL url = new URL(cloneUrl);
            return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile()).toString();
        } catch (MalformedURLException e) {
            // ignore
            return cloneUrl;
        }
    }

    public static void loadGitCredentials(Map<String,UserPassword> map, File file) {
        if (Files.isFile(file)) {
            List<String> lines;
            try {
                lines = IOHelpers.readLines(file);
            } catch (IOException e) {
                LOG.warn("Failed to load file " + file + " " + e, e);
                return;
            }

            LOG.debug("Loading git credentials file " + file);
            int count = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        URL url = new URL(line);
                        String host = url.getHost();
                        String userInfo = url.getUserInfo();
                        if (userInfo != null) {
                            String[] values = userInfo.split(":", 2);
                            if (values != null && values.length == 2) {
                                String user = values[0];
                                String password = values[1];
                                if (Strings.notEmpty(user) && Strings.notEmpty(password)) {
                                    map.put(host, new UserPassword(user, password));
                                    count++;
                                }
                            }
                        }
                    } catch (MalformedURLException e) {
                        // ignore
                    }
                }
            }
            LOG.info("Loaded " + count + " git credentials from " + file);
        }
    }
}
