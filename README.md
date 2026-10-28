# naic

A small Jakarta EE 10 web application packaged as a **WildFly 42 bootable JAR**: one executable jar that contains a
trimmed WildFly server plus the app. No separate WildFly installation is needed.

## Prerequisites: Java and Maven via SDKMAN

The required tool versions are pinned in [`.sdkmanrc`](.sdkmanrc):

| Tool  | Version                           |
|-------|-----------------------------------|
| Java  | Azul Zulu 21 (`21.0.13.0+9-zulu`) |
| Maven | 3.10.0                            |

1. Install [SDKMAN](https://sdkman.io/install) if you don't have it:
   ```sh
   curl -s "https://get.sdkman.io" | bash
   source "$HOME/.sdkman/bin/sdkman-init.sh"
   ```
2. From the repository root, install the pinned versions:
   ```sh
   sdk env install
   ```
3. Switch the current shell to them (do this in each new shell):
   ```sh
   sdk env
   java -version   # should report Zulu 21.0.13
   mvn -v          # should report Maven 3.10.0
   ```

To have SDKMAN switch versions automatically whenever you `cd` into the repo, set `sdkman_auto_env=true` in
`~/.sdkman/etc/config`.

## Build

```sh
mvn clean package
```

This produces:

- `target/naic.war`: the plain web application
- `target/naic-bootable.jar`: the WAR plus an embedded WildFly 42 server

The first build downloads the WildFly feature pack, so it takes a while. During the build the plugin briefly boots the
server to configure it; the WildFly startup and shutdown messages in the build output are expected.

## Run the application standalone

```sh
java -jar target/naic-bootable.jar
```

When the log shows `WFLYSRV0025: WildFly 42.0.0.Final ... started`, the app is available at:

| URL                                    | Description                                           |
|----------------------------------------|-------------------------------------------------------|
| http://localhost:8080/                 | JSP welcome page ("Hello World!")                     |
| http://localhost:8080/api/hello/{user} | REST endpoint, returns `Hello {user}` as `text/plain` |

```sh
curl http://localhost:8080/api/hello/gary
# Hello gary
```

Stop the server with `Ctrl+C`.

## Run the tests

```sh
mvn test
```

Run a single test class or method:

```sh
mvn test -Dtest=HelloEndpointTest
mvn test -Dtest=HelloEndpointTest#getHelloGreetsUser
```

The REST endpoint tests use RESTEasy's in-memory mock dispatcher. Requests go through real
JAX-RS routing, parameter binding and content negotiation without starting a server, so the suite runs in well under a
second. Because no container runs, CDI injection isn't available in these tests.

## Code coverage report

Coverage is collected by [JaCoCo](https://www.jacoco.org/jacoco/) on every test run (including `mvn package`):

```sh
mvn test
open target/site/jacoco/index.html   # macOS; use xdg-open on Linux
```

Machine-readable versions are written alongside: `target/site/jacoco/jacoco.xml` (for CI and IDE integration) and
`jacoco.csv`.

## Project layout

```
src/main/java/estate/myers/naic/
  RestApplication.java          JAX-RS activation, mounts REST endpoints under /api
  endpoint/HelloEndpoint.java   GET /api/hello/{user}
src/main/webapp/                index.jsp and WEB-INF/web.xml
src/test/java/                  JUnit 4 tests
```

## How the bootable JAR is configured

The server is built by the `wildfly-maven-plugin` (see `pom.xml`):

- **Feature pack:** `org.wildfly:wildfly-galleon-pack` at `version.wildfly` (42.0.0.Final).
- **Layers:** `jaxrs-server` (servlet, JSP, Jakarta REST). Add Galleon layers here if the app needs more subsystems, for
  example `jpa` or `ejb-lite`.
- **Deployment:** the WAR is deployed as `ROOT.war`, so it is served at `/`. The deployment scanner layer has to stay
  enabled because that's how the app is loaded at startup.

To upgrade WildFly, bump `version.wildfly` and keep `version.resteasy` (used by the tests) in step with the RESTEasy
version that WildFly release bundles.

## Summarizing a branch (Claude Code skill)

The `branch-summary` [Claude Code skill](https://docs.claude.com/en/docs/claude-code/skills) drafts a commit message
describing everything the current branch changes relative to `main`. It's useful for squash merges and PR
descriptions.

```text
/branch-summary            # compare against main (falls back to master, origin/main, origin/master)
/branch-summary develop    # compare against another base branch
```

The output has a one-sentence theme line, a blank line, and then a bullet per meaningful change, most significant
first:

```text
Add bootable JAR packaging and REST endpoint tests

- Switch packaging to wildfly-maven-plugin with bootable-jar enabled
- Add HelloEndpoint JAX-RS resource under /api
- Add RESTEasy mock-dispatcher tests and JaCoCo coverage reporting
```

The skill compares from the merge base (`git diff main...HEAD`), so commits added to `main` after the branch split
off are ignored. It summarizes the actual diff rather than the commit subjects. Uncommitted changes are listed
separately and aren't included in the message. It only prints the message. It doesn't commit or push unless you ask.

The skill is defined in [`.claude/skills/branch-summary/SKILL.md`](.claude/skills/branch-summary/SKILL.md), so it's
available to anyone running Claude Code in this repository.
