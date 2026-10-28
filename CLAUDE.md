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
- The server comes from the `org.wildfly:wildfly-galleon-pack:${version.wildfly}` feature pack. Galleon **layers** decide which subsystems are included (currently `jaxrs-server` for servlet, JSP, JAX-RS and CDI; `embedded-activemq` for an in-VM Artemis JMS broker; and `ejb` for message-driven beans). Add layers there when the app needs more (e.g. `jpa`). MDBs need the full `ejb` layer because `ejb-lite` has no MDB support.
- The plugin must be declared under `<build><plugins>`, not only `<pluginManagement>`, or no bootable jar is produced.
- The WAR is deployed as `ROOT.war` (context root `/`). This plugin drops it into `standalone/deployments`, so the **`deployment-scanner` layer must not be excluded**, or the server boots with no app and returns 404.

## Messaging

The JMS queue is declared in code by `@JMSDestinationDefinition` on `NaicQueueListener` (JNDI `java:/queue/naic`, Artemis queue `naic`), so there's no server-side config. `NaicQueueListener` is the MDB that logs each message. `POST /api/messages` (`MessageEndpoint`) sends a `text/plain` body to the queue. The MDB uses the ejb3 subsystem's default resource adapter (`activemq-ra`, provided by `embedded-activemq`).

The embedded broker is clustered with one peer by `src/main/wildfly/cluster-messaging.cli`, which the plugin runs at package time via `<packaging-scripts>`. It uses static connectors over HTTP and `ON_DEMAND` load balancing, so a message POSTed to either node is consumed exactly once, round-robin across nodes. The peer is set at runtime with `-Dnaic.peer.port` (default 8180), `-Dnaic.peer.host` (default localhost) and `-Dnaic.cluster.password`. Two local nodes:

```sh
java -jar target/naic-bootable.jar -Djboss.node.name=node1 -Dnaic.peer.port=8180
java -jar target/naic-bootable.jar -Djboss.node.name=node2 -Djboss.socket.binding.port-offset=100 -Dnaic.peer.port=8080
```

`naic-cluster` (in `.local/bin`, put on `PATH` by direnv's `.envrc`) runs both of these in the foreground with `[node1]`/`[node2]`-prefixed output, and Ctrl-C stops both. Extra arguments are passed to both JVMs. `naic-send [-n 1|2] [-c COUNT] [MESSAGE...]`, next to it, POSTs messages to one node (default node1). It reads one message per line from stdin when given no messages. A single node on its own still works. With no peer to connect to, it keeps every message for its own MDB.

## Testing endpoints

JAX-RS endpoints are tested in memory with RESTEasy's mock dispatcher (`org.jboss.resteasy.mock`, test-scoped `resteasy-core`), MockMvc-style: no server, but real routing/param binding/content negotiation. See `HelloEndpointTest`. Request paths are relative to `@ApplicationPath` (omit `/api`). There's no CDI in this harness, so injected dependencies must be supplied by hand. JMS interfaces are stubbed with `java.lang.reflect.Proxy` because there's no mocking library (see `MessageEndpointTest`, `NaicQueueListenerTest`). Keep `version.resteasy` matched to the RESTEasy bundled in the chosen WildFly (check `target/server/modules/.../resteasy-core/main/` after a package).

## Conventions

- Use Jakarta namespaces (`jakarta.*`, `web.xml` schema `https://jakarta.ee/xml/ns/jakartaee` v6.0). `jakarta.jakartaee-api` is `provided` because WildFly supplies it at runtime.
- A class's own logger is always named after the class it's declared in: `private static final Logger LOG = Logger.getLogger(ThisClass.class.getName());`. The one exception is a test that captures what the class under test logs. It has to look up that class's logger, so name the field for what it is (e.g. `listenerLog`), not as the test's own logger.
- Use the collection method that says what you mean instead of index or size arithmetic: `getFirst()`/`getLast()` rather than `get(0)`/`get(size() - 1)` (Java 21 sequenced collections), `isEmpty()` rather than `size() == 0`, `contains`, `removeFirst()` and so on. This applies in tests too.
- In tests, prefer a clear, readable assertion to one the reader has to decode from numbers: `assertFalse(list.isEmpty())` / `assertTrue(list.isEmpty())` rather than `assertEquals(1, list.size())` / `assertEquals(0, list.size())`.
