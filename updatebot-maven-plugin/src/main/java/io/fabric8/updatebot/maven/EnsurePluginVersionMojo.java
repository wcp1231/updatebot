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
package io.fabric8.updatebot.maven;

import io.jenkins.updatebot.kind.maven.PomUpdateStatus;
import io.jenkins.updatebot.model.MavenArtifactKey;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Ensures the given maven plugin version is of the given minimum version, modifying it if required
 */
@Mojo(name = "plugin", aggregator = true, requiresProject = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class EnsurePluginVersionMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(property = "group", defaultValue = MavenArtifactKey.DEFAULT_MAVEN_PLUGIN_GROUP)
    protected String group;

    @Parameter(property = "artifact", required = true)
    protected String artifact;

    @Parameter(property = "version", required = true)
    protected String version;

    @Parameter(property = "pomFile", defaultValue = "${basedir}/pom.xml")
    protected File pomFile;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        if (project == null) {
            throw new MojoExecutionException("No Project available!");
        }
        Map<String, Plugin> map = project.getBuild().getPluginsAsMap();
        String key = group + ":" + artifact;
        Plugin plugin = map.get(key);
        PomUpdateStatus status;
        try {
            status = PomUpdateStatus.createPomUpdateStatus(pomFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to parse pom.xml: " + e, e);
        }
        status.updatePluginVersion(key, version, true);
        if (status.isUpdated()) {
            try {
                status.saveIfChanged();

                log.info("Modified pom setting plugin " + key + " to version " + version);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to save pom.xml: " + e, e);
            }
        } else {
            log.info("No need to modify the pom.xml as the plugin " + key + " is on version: " + plugin.getVersion());
        }
    }
}
