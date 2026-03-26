# For Next Maintainer

This document is for the next maintainer if the current maintainer can no longer
operate this repository.

## Taking over this repository

1. Fork this repository to your own GitHub account or organization.
2. Decide the coordinates you will publish under.

If you publish through a GitHub-backed Central namespace, `io.github.<your-id>`
is the usual choice. If you control your own domain, use that reverse domain
instead.

3. Rename the package names and `groupId` to match your own reverse-domain
   convention.

Keep package names and publishing coordinates aligned to avoid confusion for the
next maintainer and users of the library.

4. Preserve readable git history when you rename packages.

Use `git mv` or an IDE refactor/rename so Git can detect file renames. Git does
not track directories directly, but it can keep file-level rename history when
the move happens as one coherent change.

5. Update all coordinates and references.

That includes package declarations, imports, Gradle group, README dependency
examples, badges, publishing metadata, sample applications, and IntelliJ run
configurations.

6. Register and verify your namespace in Sonatype Central Portal.

If you want to publish SNAPSHOT builds, enable `SNAPSHOTs` for that namespace in
Central Portal as well.

## Build

Build everything locally before publishing:

```bash
./gradlew clean build
```

Publish the starter to your local Maven repository:

```bash
./gradlew :ssh-shell-spring-boot-starter:publishToMavenLocal
```

## Sending test coverage to SonarCloud

If you want to send test results and JaCoCo coverage to SonarCloud through
Gradle, add the Sonar token to `~/.gradle/gradle.properties`:

```properties
systemProp.sonar.token=<sonarcloud-token>
```

Keep this in your local Gradle home only. Do not commit it to the repository.

If you send analysis through Gradle, disable SonarCloud Automatic Analysis for
the project first. Manual Gradle analysis and SonarCloud Automatic Analysis
cannot be enabled at the same time, and JaCoCo coverage requires the
manual/CI-based analysis path.

This repository does not commit `gradle/verification-metadata.xml`. The file is
gitignored and should be treated as a temporary local artifact that is created
only for SonarCloud analysis. SonarCloud reports a security hotspot when the
file is missing, but keeping it around permanently can make IntelliJ Gradle
sync noisy because the IDE may resolve extra source and javadoc artifacts.

Then run:

```bash
./gradlew --no-daemon --write-verification-metadata sha256 sonar
rm -f gradle/verification-metadata.xml
```

The `help` task is only there to give Gradle a lightweight task to execute
while it writes verification metadata. If IntelliJ IDEA or another IDE reports
dependency verification failures during Gradle sync, check the generated report
under `build/reports/dependency-verification/`. In most cases the simplest
recovery is to remove `gradle/verification-metadata.xml` and reload the
project.

## SNAPSHOT publishing

For SNAPSHOT publishing, keep the version in `gradle.properties` with the
`-SNAPSHOT` suffix, for example:

```properties
version=1.0.0-SNAPSHOT
```

Add the following to `~/.gradle/gradle.properties`:

```properties
sonatypeUsername=<central-portal-token-username>
sonatypePassword=<central-portal-token-password>
```

Use a Central Portal user token here, not your account login password.

Then publish the SNAPSHOT:

```bash
./gradlew :ssh-shell-spring-boot-starter:publishToSonatype
```

## Release publishing

Create a GPG key first:

```bash
gpg --full-generate-key
gpg --list-secret-keys --keyid-format SHORT
gpg --keyring secring.gpg --export-secret-keys <KEY_ID> > ~/.gnupg/secring.gpg
```

Publishing the public key to a public keyserver is recommended before the first
release, for example:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
```

Add the following to `~/.gradle/gradle.properties`:

```properties
sonatypeUsername=<central-portal-token-username>
sonatypePassword=<central-portal-token-password>
signing.keyId=<last-8-characters-of-key-id>
signing.password=<gpg-passphrase>
signing.secretKeyRingFile=/home/<your-user>/.gnupg/secring.gpg
```

Then:

1. Remove the `-SNAPSHOT` suffix from the version in `gradle.properties`.
2. Run a full local build.
3. Commit the release version.
4. Publish the release.
5. Verify the published artifact in Maven Central / Sonatype Central.
6. Tag the release commit and write the GitHub release notes.
7. Bump the version to the next `-SNAPSHOT` and commit again.

Release publish command:

```bash
./gradlew clean build
```

```bash
./gradlew :ssh-shell-spring-boot-starter:publishToSonatype closeAndReleaseSonatypeStagingRepository
```
