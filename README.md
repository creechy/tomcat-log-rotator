# Tomcat Log Rotator

`tomcat-log-rotator` is a Tomcat lifecycle listener that rotates every
[`AccessLogValve`](https://tomcat.apache.org/tomcat-9.0-doc/api/org/apache/catalina/valves/AccessLogValve.html)
configured below a Tomcat Engine on a cron schedule. It's useful to force
access logs to be rotated at or before some specific period, for example, 
to ensure all logs are available for external log processing frameworks.

The listener recursively scans the Engine, Hosts, and Contexts when the Engine
starts. At each scheduled run, it calls `rotate()` on each rotatable access-log
valve it finds.

## Requirements

- Java 17 or later
- Tomcat 9 (the project is compiled against Tomcat 9.0.121)

Tomcat supplies the Catalina APIs at runtime. The packaged artifact includes a
relocated copy of cron4j, so it does not require a separate cron4j installation.

## Build

Use the included Gradle wrapper:

```sh
./gradlew shadowJar
```

The deployable, dependency-inclusive JAR is written to:

```text
build/libs/tomcat-log-rotator-1.0-SNAPSHOT-all.jar
```

To check formatting:

```sh
./gradlew spotlessCheck
```

## Install and configure

1. Build the shaded JAR and copy it to Tomcat's `lib/` directory.
2. Register `PeriodicAccessLogRotator` as a lifecycle listener on the Engine in
   `conf/server.xml`.
3. Set `cronExpression` to the rotation schedule you want.

For example, this rotates access logs every hour on the hour:

```xml
<Engine name="Catalina" defaultHost="localhost">
  <Listener className="org.fakebelieve.tomcat.PeriodicAccessLogRotator"
            cronExpression="0 * * * *" />
  <!-- Hosts, contexts, and AccessLogValves -->
</Engine>
```

Restart Tomcat after installing or changing the listener configuration. Tomcat's
logs will report the configured cron expression when the listener starts and
will log each rotation.

## Scheduling

Schedules use cron4j's five-field cron format:

```text
minute hour day-of-month month day-of-week
```

Examples:

| Schedule | Expression |
| --- | --- |
| Every hour, on the hour | `0 * * * *` |
| Every day at midnight | `0 0 * * *` |
| Every 15 minutes | `*/15 * * * *` |
| Every Monday at 02:30 | `30 2 * * 1` |

If omitted, `cronExpression` defaults to `0 * * * *`.

## Notes

- Only `AccessLogValve` instances whose `rotatable` property is enabled are
  rotated.
- The listener is intended for Engine-level configuration; it discovers valves
  attached to the Engine and all of its child containers.
- Rotation uses the normal `AccessLogValve.rotate()` behavior, so file naming
  and retention remain controlled by each valve's Tomcat configuration.
