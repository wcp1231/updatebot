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

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.utils.PropertiesHelper;
import io.fabric8.utils.Strings;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Enriches spring-boot containers with health checks if the actuator module is present.
 */
public class SpringBootHealthCheckEnricher extends AbstractHealthCheckEnricher {
    private static final transient Logger log = LoggerFactory.getLogger(AbstractHealthCheckEnricher.class);
    private static final String[] REQUIRED_CLASSES = {
            "org.springframework.boot.actuate.health.HealthIndicator",
            "org.springframework.web.context.support.GenericWebApplicationContext"
    };

    private static final int DEFAULT_SERVER_PORT = 8080;
    private static final String SCHEME_HTTPS = "HTTPS";
    private static final String SCHEME_HTTP = "HTTP";


    public SpringBootHealthCheckEnricher(MavenProject project) {
        super(project);
    }

/*    @Override
    protected Probe getReadinessProbe() {
        Integer initialDelay = Configs.asInteger(getConfig(Config.readinessProbeInitialDelaySeconds));
        Integer period = Configs.asInteger(getConfig(Config.readinessProbePeriodSeconds));
        return discoverSpringBootHealthCheck(initialDelay, period);
    }

    @Override
    protected Probe getLivenessProbe() {
        Integer initialDelay = Configs.asInteger(getConfig(Config.livenessProbeInitialDelaySeconds));
        Integer period = Configs.asInteger(getConfig(Config.livenessProbePeriodSeconds));
        return discoverSpringBootHealthCheck(initialDelay, period);
    }*/

    public Probe discoverProbe(Integer initialDelay, Integer period) {
        try {
            if (MavenUtil.hasAllClasses(this.getProject(), REQUIRED_CLASSES)) {
                Properties properties = SpringBootUtil.getSpringBootApplicationProperties(this.getProject());
                return buildProbe(properties, initialDelay, period);
            }
        } catch (Exception ex) {
            log.error("Error while reading the spring-boot configuration", ex);
        }
        return null;
    }

    protected Probe buildProbe(Properties springBootProperties, Integer initialDelay, Integer period) {
        SpringBootConfigurationHelper propertyHelper = new SpringBootConfigurationHelper(SpringBootUtil.getSpringBootVersion(getProject()));
        Integer managementPort = PropertiesHelper.getInteger(springBootProperties, propertyHelper.getManagementPortPropertyKey());
        boolean usingManagementPort = managementPort != null;

        Integer port = managementPort;
        if (port == null) {
            port = PropertiesHelper.getInteger(springBootProperties, propertyHelper.getServerPortPropertyKey(), DEFAULT_SERVER_PORT);
        }

        String scheme;
        String prefix;
        if (usingManagementPort) {
            scheme = Strings.isNotBlank(springBootProperties.getProperty(propertyHelper.getManagementKeystorePropertyKey())) ? SCHEME_HTTPS : SCHEME_HTTP;
            prefix = springBootProperties.getProperty(propertyHelper.getManagementContextPathPropertyKey(), "");
        } else {
            scheme = Strings.isNotBlank(springBootProperties.getProperty(propertyHelper.getServerKeystorePropertyKey())) ? SCHEME_HTTPS : SCHEME_HTTP;
            prefix = springBootProperties.getProperty(propertyHelper.getServerContextPathPropertyKey(), "");
            prefix += springBootProperties.getProperty(propertyHelper.getServletPathPropertyKey(), "");
            prefix += springBootProperties.getProperty(propertyHelper.getManagementContextPathPropertyKey(), "");
        }

        String actuatorBasePathKey = propertyHelper.getActuatorBasePathPropertyKey();
        String actuatorBasePath = propertyHelper.getActuatorDefaultBasePath();
        if (actuatorBasePathKey != null) {
            actuatorBasePath = springBootProperties.getProperty(actuatorBasePathKey, actuatorBasePath);
        }

        // lets default to adding a spring boot actuator health check
        ProbeBuilder probeBuilder = new ProbeBuilder().
                withNewHttpGet().withNewPort(port).withPath(prefix + actuatorBasePath + "/health").withScheme(scheme).endHttpGet();

        if (initialDelay != null) {
            probeBuilder = probeBuilder.withInitialDelaySeconds(initialDelay);
        }
        if (period != null) {
            probeBuilder = probeBuilder.withPeriodSeconds(period);
        }

        return probeBuilder.build();
    }

}
