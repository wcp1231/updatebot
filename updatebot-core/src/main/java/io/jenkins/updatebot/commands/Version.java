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

import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import io.jenkins.updatebot.CommandNames;
import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.support.VersionHelper;

/**
 * Displays version
 */
@Parameters(commandNames = CommandNames.VERSION, commandDescription = "Displays updatebot version")
public class Version extends CommandSupport {
    @Parameter()
    private String command;

    private JCommander commander;


    @Override
    public ParentContext run(Configuration configuration) throws IOException {
        showVersion();
        return new ParentContext();
    }

    @Override
    public void run(CommandContext context) throws IOException {
        showVersion();
    }

    public void showVersion() {
        System.out.println(VersionHelper.updateBotVersion());
    }

    public JCommander getCommander() {
        return commander;
    }

    public void setCommander(JCommander commander) {
        this.commander = commander;
    }
}
