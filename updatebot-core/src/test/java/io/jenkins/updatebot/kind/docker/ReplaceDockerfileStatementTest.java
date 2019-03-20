/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jenkins.updatebot.kind.docker;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class ReplaceDockerfileStatementTest {
    protected static String assertReplace(String line, String name, String value, boolean expectedAnswer, String expectedLine) {
        List<String> lines = new ArrayList<>();
        lines.add(line);
        boolean answer = DockerUpdater.replaceDockerfileStatement(lines, name, value);
        assertThat(answer).isEqualTo(expectedAnswer);
        String actualLine = lines.get(0);
        if (answer) {
            assertThat(actualLine).isEqualTo(expectedLine);
        }
        return actualLine;
    }

    @Test
    public void testReplaceDockerStatement() throws Exception {
        assertReplace("FROM jenkinsxio/jx:0.0.1", "jenkinsxio/jx", "1.2.3", true, "FROM jenkinsxio/jx:1.2.3");
        assertReplace("FROM jenkinsxio/jx:0.0.2", "jenkinsxio/jx", "1.2.3", true, "FROM jenkinsxio/jx:1.2.3");
        assertReplace("ENV JX_VERSION 0.0.1", "JX_VERSION", "1.2.3", true, "ENV JX_VERSION 1.2.3");
        assertReplace("ENV JX_VERSION 0.0.2", "JX_VERSION", "1.2.3", true, "ENV JX_VERSION 1.2.3");
        assertReplace("ARG JX_VERSION=0.0.1", "JX_VERSION", "1.2.3", true, "ARG JX_VERSION=1.2.3");
        assertReplace("ARG JX_VERSION=0.0.2", "JX_VERSION", "1.2.3", true, "ARG JX_VERSION=1.2.3");
    }

    @Test
    public void testDosNotChangeOtherStatements() throws Exception {
        assertReplace("FROM wine/cheese:whatnot", "jenkinsxio/jx", "1.2.3", false, "FROM wine/cheese:whatnot");
        assertReplace("ENV CHEESE_N_WINE_VERSION 0.0.1", "JX_VERSION", "1.2.3", false, "ENV JX_VERSION 1.2.3");
        assertReplace("ARG CHEESE_N_WINE_VERSION=0.0.1", "JX_VERSION", "1.2.3", false, "ARG JX_VERSION=1.2.3");
    }

}
