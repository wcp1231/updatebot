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

package io.fabric8.updatebot.maven.health;

import io.fabric8.utils.Objects;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author roland
 * @since 31/03/16
 */
public class MavenUtil {

    private static final String DEFAULT_CONFIG_FILE_NAME = "kubernetes.json";

    public static URLClassLoader getCompileClassLoader(MavenProject project) {
        try {
            List<String> classpathElements = project.getCompileClasspathElements();
            return createClassLoader(classpathElements, project.getBuild().getOutputDirectory());
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalArgumentException("Cannot resolve artifact from compile classpath", e);
        }
    }


    // ====================================================

    private static URLClassLoader createClassLoader(List<String> classpathElements, String... paths) {
        List<URL> urls = new ArrayList<>();
        for (String path : paths) {
            URL url = pathToUrl(path);
            urls.add(url);
        }
        for (Object object : classpathElements) {
            if (object != null) {
                String path = object.toString();
                URL url = pathToUrl(path);
                urls.add(url);
            }
        }
        return createURLClassLoader(urls);
    }

    private static URLClassLoader createURLClassLoader(Collection<URL> jars) {
        return new URLClassLoader(jars.toArray(new URL[jars.size()]));
    }

    private static URL pathToUrl(String path) {
        try {
            File file = new File(path);
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(String.format("Cannot convert %s to a an URL: %s", path, e.getMessage()), e);
        }
    }


    /**
     * Returns true if the maven project has a dependency with the given groupId and artifactId (if not null)
     */
    public static boolean hasDependency(MavenProject project, String groupId, String artifactId) {
        return getDependencyVersion(project, groupId, artifactId) != null;
    }

    /**
     * Returns the version associated to the dependency dependency with the given groupId and artifactId (if present)
     */
    public static String getDependencyVersion(MavenProject project, String groupId, String artifactId) {
        Set<Artifact> artifacts = project.getArtifacts();
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                String scope = artifact.getScope();
                if (Objects.equal("test", scope)) {
                    continue;
                }
                if (artifactId != null && !Objects.equal(artifactId, artifact.getArtifactId())) {
                    continue;
                }
                if (Objects.equal(groupId, artifact.getGroupId())) {
                    return artifact.getVersion();
                }
            }
        }
        return null;
    }

    /**
     * Returns true if all the given class names could be found on the given class loader
     */
    public static boolean hasAllClasses(MavenProject project, String... classNames) {
        URLClassLoader compileClassLoader = getCompileClassLoader(project);
        for (String className : classNames) {
            try {
                compileClassLoader.loadClass(className);
            } catch (Throwable e) {
                // ignore message
                return false;
            }
        }
        return true;
    }

}
