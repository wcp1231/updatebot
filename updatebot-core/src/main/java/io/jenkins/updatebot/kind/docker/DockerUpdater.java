/*
 * Copyright 2018 Original Authors
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
package io.jenkins.updatebot.kind.docker;

import io.fabric8.utils.Files;
import io.fabric8.utils.IOHelpers;
import io.jenkins.updatebot.commands.CommandContext;
import io.jenkins.updatebot.commands.PushVersionChangesContext;
import io.jenkins.updatebot.kind.UpdaterSupport;
import io.jenkins.updatebot.model.Dependencies;
import io.jenkins.updatebot.model.DependencyVersionChange;
import io.jenkins.updatebot.support.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 */
public class DockerUpdater extends UpdaterSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(DockerUpdater.class);

    @Override
    public boolean isApplicable(CommandContext context) {
        return FileHelper.isFile(context.file("Dockerfile"));
    }

    @Override
    public void addVersionChangesFromSource(CommandContext context, Dependencies dependencyConfig, List<DependencyVersionChange> list) throws IOException {
    }

    @Override
    public boolean pushVersions(CommandContext parentContext, List<DependencyVersionChange> changes) throws IOException {
        boolean answer = false;
        if (isApplicable(parentContext)) {
            for (DependencyVersionChange step : changes) {
                PushVersionChangesContext context = new PushVersionChangesContext(parentContext, step);
                boolean updated = pushVersions(context);
                if (updated) {
                    answer = true;
                } else {
                    parentContext.removeChild(context);
                }
            }
        }
        return answer;
    }

    protected boolean pushVersions(PushVersionChangesContext context) throws IOException {
        boolean answer = false;
        List<PushVersionChangesContext.Change> changes = context.getChanges();
        for (PushVersionChangesContext.Change change : changes) {
            if (doPushVersionChange(context, context.getName(), context.getValue())) {
                answer = true;
            }
        }
        DependencyVersionChange step = context.getStep();
        if (step != null) {
            if (doPushVersionChange(context, step.getDependency(), context.getValue())) {
                answer = true;
            }
        }
        return answer;
    }

    private boolean checkDirectory(File dir, PushVersionChangesContext context, String name, String value) throws IOException {
        boolean answer = false;
        File[] files = dir.listFiles();
        for (File file : files) {
                if (file.isDirectory()) {
                        boolean newAnswer = checkDirectory(file, context, name, value);
                        if (!answer) {
                                answer = newAnswer;
                        }
                } else {
                        String fileName = file.getName();
                        if (Files.isFile(file) && fileName.equals("Dockerfile") || fileName.startsWith("Dockerfile.")) {
                                if (updateDockerfile(context, file, name, value)) {
                                        answer = true;
                                }
                        }
                }
        }
        return answer;
        }

    private boolean doPushVersionChange(PushVersionChangesContext context, String name, String value) throws IOException {
        boolean answer = false;
        File dir = context.getDir();
        if (Files.isDirectory(dir)) {
            answer = checkDirectory(dir, context, name, value);
        }
        return answer;
    }

    private boolean updateDockerfile(PushVersionChangesContext context, File file, String name, String value) throws IOException {
        List<String> lines = IOHelpers.readLines(file);
        boolean answer = replaceDockerfileStatement(lines, name, value);
        if (answer) {
            IOHelpers.writeLines(file, lines);
        }
        return answer;
    }
    public static boolean replaceDockerfileStatement(List<String> lines, String name, String value) {
        String[] linePrefixes = {
                "FROM " + name + ":",
                "ENV " + name + " ",
                "ARG " + name + "="
        };
        boolean answer = false;
        for (int i = 0, size = lines.size(); i < size; i++) {
            String line = lines.get(i);
            for (String linePrefix : linePrefixes) {
                if (line.startsWith(linePrefix)) {
                    String remaining = line.substring(linePrefix.length());
                    if (!remaining.trim().equals(value)) {
                        answer = true;
                        lines.set(i, linePrefix + value);
                    }
                }
            }
        }
        return answer;
    }
}
