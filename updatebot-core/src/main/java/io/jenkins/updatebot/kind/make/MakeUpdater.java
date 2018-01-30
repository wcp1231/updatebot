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
package io.jenkins.updatebot.kind.make;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class MakeUpdater extends UpdaterSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(MakeUpdater.class);

    @Override
    public boolean isApplicable(CommandContext context) {
        return FileHelper.isFile(context.file("Makefile"));
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


    private boolean doPushVersionChange(PushVersionChangesContext context, String name, String value) throws IOException {
        boolean answer = false;
        File dir = context.getDir();
        if (Files.isDirectory(dir)) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (Files.isFile(file) && fileName.equals("Makefile") || fileName.startsWith("Makefile.")) {
                        if (updateMakefile(context, file, name, value)) {
                            answer = true;
                        }
                    }
                }
            }
        }
        return answer;
    }

    private boolean updateMakefile(PushVersionChangesContext context, File file, String name, String value) throws IOException {
        List<String> lines = IOHelpers.readLines(file);
        boolean answer = replaceMakefileStatement(lines, name, value);
        if (answer) {
            IOHelpers.writeLines(file, lines);
        }
        return answer;
    }

    public static boolean replaceMakefileStatement(List<String> lines, String name, String value) {
        boolean answer = false;
        String regex = name + "\\s*:=\\s*(.+)";
        Pattern pattern = Pattern.compile(regex);
        for (int i = 0, size = lines.size(); i < size; i++) {
            String line = lines.get(i);
            Matcher m = pattern.matcher(line);
            if (m.matches()) {
                int start = m.start(1);
                int end = m.end(1);
                String newLine = line.substring(0, start) + value;
                if (end + 1 < line.length()) {
                    newLine += line.substring(end);
                }
                if (!line.equals(newLine)) {
                    lines.set(i, newLine);
                    answer = true;
                }
            }
        }
        return answer;
    }
}