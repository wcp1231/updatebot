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
import io.jenkins.updatebot.kind.Kind;
import io.jenkins.updatebot.kind.regex.RegexUpdater;
import io.jenkins.updatebot.repository.LocalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Push changes from a specific release pipeline into downstream projects
 */
@Parameters(commandNames = CommandNames.PUSH_VERSION, commandDescription = "Pushes regex changes into your projects. " +
        "You usually invoke this command after a release has been performed")
public class PushRegexChanges extends ModifyFilesCommandSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(PushRegexChanges.class);

    @Parameter(order = 0, names = {"--regex", "-r"}, description = "The regular expression to replace")
    private String regex;

    @Parameter(order = 1, names = {"--value", "-v"}, description = "The value to replace the regex group with")
    private String value;

    @Parameter(order = 2, names = {"--exclude", "-x"}, description = "The file patterns to exclude")
    private List<String> excludeFiles;

    @Parameter(description = "The file patterns to replace", required = true)
    private List<String> files;

    public PushRegexChanges() {
    }

    public String getRegex() {
        return regex;
    }

    public String getValue() {
        return value;
    }

    public List<String> getFiles() {
        return files;
    }

    public List<String> getExcludeFiles() {
        return excludeFiles;
    }

    @Override
    protected CommandContext createCommandContext(LocalRepository repository, Configuration configuration) {
        return new PushRegexChangesContext(repository, configuration, this);
    }

    @Override
    protected boolean doProcess(CommandContext context) throws IOException {
        LocalRepository repository = context.getRepository();
        File dir = repository.getDir();
        LOG.info("Updating regex: " + getRegex() + " with value " + getValue() + " in: " + dir + " repo: " + repository.getCloneUrl());

        RegexUpdater updater = (RegexUpdater) Kind.REGEX.getUpdater();
        return updater.pushRegex(this, context);
    }
}
