#!/usr/bin/env bash

# ensure we're not on a detached head
git checkout master

# until we switch to the new kubernetes / jenkins credential implementation use git credentials store
git config credential.helper store

# so we can retrieve the version in later steps
echo $(jx-release-version) > target/VERSION
mvn versions:set -DnewVersion=$(cat target/VERSION)

mvn clean -B
mvn -V -B -e -U install org.sonatype.plugins:nexus-staging-maven-plugin:1.6.7:deploy -P release -P openshift -DnexusUrl=https://oss.sonatype.org -DserverId=oss-sonatype-staging

# now the sonatype repo ids will be on disk

#jx step nexus_release

#git commit -a -m 'release $(cat target/VERSION)'
#git push origin release-$(cat target/VERSION)


