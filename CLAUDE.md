# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Git: read-only

The agent may only run **read-only** git operations (e.g. `git status`, `git diff`, `git log`, `git show`, `git blame`, `git branch --list`). Do **not** run anything that changes repository state or touches a remote: no `commit`, `add`, `stash`, `checkout`/`switch`, `branch` create/delete, `merge`, `rebase`, `reset`, `restore`, `tag`, `cherry-pick`, `push`, `pull` or `fetch`, and no opening PRs. Leave all of that to the user.

## What this is

A minimal Jakarta EE 10 WAR (`naic`) that is packaged as a **WildFly 42 bootable JAR**: a single executable jar containing a trimmed WildFly server plus the app.

## Toolchain

Pinned in `.sdkmanrc` (Azul Zulu 21, Maven 3.10.0). Run `sdk env` in the repo root before building (`sdk env install` on a fresh machine). The user's global SDKMAN default is a different JDK, so builds outside `sdk env` won't match. `maven.compiler.release=21` should stay in step with the pinned JDK.

## Commands

```sh
mvn clean package                    # builds target/naic.war AND target/naic-bootable.jar
java -jar target/naic-bootable.jar   # runs WildFly with the app at http://localhost:8080/
mvn test -Dtest=ClassName#method     # single test (JUnit 4)
mvn test                             # also writes JaCoCo coverage to target/site/jacoco/index.html (+ jacoco.xml/csv)
```

`mvn package` takes a while on the first run because it downloads the WildFly Galleon feature pack. It provisions the server into `target/server/` and briefly boots it during the build. That embedded boot log in the build output is expected.

## How the bootable JAR is built

All in `pom.xml`:
- `org.wildfly.plugins:wildfly-maven-plugin` (version in `version.wildfly.plugin`) runs its `package` goal with `<bootable-jar>true</bootable-jar>`. Do **not** switch back to `wildfly-jar-maven-plugin`, which is deprecated.
- The server comes from the `org.wildfly:wildfly-galleon-pack:${version.wildfly}` feature pack. Galleon **layers** decide which subsystems are included (currently `jaxrs-server`, which covers servlet, JSP and JAX-RS). Add layers there when the app needs more (e.g. `jpa`, `cdi`, `ejb-lite`).
- The plugin must be declared under `<build><plugins>`, not only `<pluginManagement>`, or no bootable jar is produced.
- The WAR is deployed as `ROOT.war` (context root `/`). This plugin drops it into `standalone/deployments`, so the **`deployment-scanner` layer must not be excluded**, or the server boots with no app and returns 404.

## Testing endpoints

JAX-RS endpoints are tested in memory with RESTEasy's mock dispatcher (`org.jboss.resteasy.mock`, test-scoped `resteasy-core`), MockMvc-style: no server, but real routing/param binding/content negotiation. See `HelloEndpointTest`. Request paths are relative to `@ApplicationPath` (omit `/api`). There's no CDI in this harness, so injected dependencies must be supplied by hand. Keep `version.resteasy` matched to the RESTEasy bundled in the chosen WildFly (check `target/server/modules/.../resteasy-core/main/` after a package).

## Conventions

- Use Jakarta namespaces (`jakarta.*`, `web.xml` schema `https://jakarta.ee/xml/ns/jakartaee` v6.0). `jakarta.jakartaee-api` is `provided` because WildFly supplies it at runtime.
