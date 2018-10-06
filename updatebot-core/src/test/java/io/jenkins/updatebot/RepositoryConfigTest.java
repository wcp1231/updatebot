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


import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.updatebot.test.Tests;
import io.jenkins.updatebot.commands.PushSourceChanges;
import io.jenkins.updatebot.model.RepositoryConfig;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 */
public class RepositoryConfigTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(RepositoryConfigTest.class);

    protected PushSourceChanges pushSourceChanges = new PushSourceChanges();
    protected Configuration testSubject;

    @Before
    public void init() throws IOException {
        this.testSubject = new Configuration();

        String workDirPath = Tests.getCleanWorkDir(getClass());
        testSubject.setWorkDir(workDirPath);
    }

    @Test
    public void testBranchConfiguration() throws Exception {
        // given
        String configFile = new File(Tests.getBasedir(), "src/test/resources/maven/branch/updatebot-develop.yml").getPath();
        testSubject.setConfigFile(configFile);

        // when
        RepositoryConfig repositoryConfig = pushSourceChanges.getRepositoryConfig(testSubject);

        // then
        assertThat(repositoryConfig.getGithub().findOrganisation("jstrachan-testing").findRepository("updatebot").getBranch()).isEqualTo("develop");
    }

    @Test
    public void testNoBranchConfigurationDefaultsToNull() throws Exception {
        // given
        String configFile = new File(Tests.getBasedir(), "src/test/resources/maven/branch/updatebot-master.yml").getPath();
        testSubject.setConfigFile(configFile);

        // when
        RepositoryConfig repositoryConfig = pushSourceChanges.getRepositoryConfig(testSubject);

        // then
        assertThat(repositoryConfig.getGithub().findOrganisation("jstrachan-testing").findRepository("updatebot").getBranch()).isNull();
    }

    @Test
    public void testUseSinglePullRequestConfigurations() throws Exception {
        // given
        String configFile = new File(Tests.getBasedir(), "src/test/resources/maven/source/updatebot.yml").getPath();
        testSubject.setConfigFile(configFile);

        // when
        RepositoryConfig repositoryConfig = pushSourceChanges.getRepositoryConfig(testSubject);

        // then
        assertThat(repositoryConfig.getGithub().findOrganisation("jstrachan-testing")
                   .findRepository("fabric8")
                   .isUseSinglePullRequest()
                  ).isFalse();

        assertThat(repositoryConfig.getGithub().findOrganisation("jstrachan-testing")
                   .findRepository("fabric8-maven-plugin")
                   .isUseSinglePullRequest()
                  ).isTrue();

    }
}
