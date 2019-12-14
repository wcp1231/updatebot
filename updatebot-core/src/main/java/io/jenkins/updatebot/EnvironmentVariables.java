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
package io.jenkins.updatebot;

/**
 */
public class EnvironmentVariables {
    public static final String CONFIG_FILE = "UPDATEBOT_CONFIG_FILE";
    public static final String WORK_DIR = "UPDATEBOT_WORK_DIR";

    public static final String GITHUB_USER = "UPDATEBOT_GITHUB_USER";
    public static final String GITHUB_PASSWORD = "UPDATEBOT_GITHUB_PASSWORD";
    public static final String GITHUB_TOKEN = "UPDATEBOT_GITHUB_TOKEN";
    public static final String GITHUB_PR_LABEL = "UPDATEBOT_GITHUB_PR_LABEL";

    public static final String POLL_PERIOD = "UPDATEBOT_POLL_PERIOD";
    public static final String POLL_TIMEOUT = "UPDATEBOT_POLL_TIMEOUT";

    public static final String MERGE = "UPDATEBOT_MERGE";
    public static final String CHECK_PR_STATUS = "UPDATEBOT_CHECK_PR_STATUS";
    public static final String DELETE_MERGED_BRANCHES = "UPDATEBOT_DELETE_MERGED_BRANCHES";
    public static final String MERGE_METHOD = "UPDATEBOT_MERGE_METHOD";

    public static final String DRY_RUN = "UPDATEBOT_DRY_RUN";

    public static final String MVN_COMMAND = "UPDATEBOT_MVN_COMMAND";
    public static final String NPM_COMMAND = "UPDATEBOT_NPM_COMMAND";

    public static final String PROW_PR_COMMAND = "UPDATEBOT_PROW_PR_COMMAND";

    public static final String JENKINSFILE_GIT_REPO = "UPDATEBOT_JENKINSFILE_GIT_REPO";

    public static final String PHAB_HOST = "PHAB_HOST";
    public static final String CONDUIT_TOKEN = "CONDUIT_TOKEN";
}
