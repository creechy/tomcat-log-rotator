package org.fakebelieve.tomcat;

import it.sauronsoftware.cron4j.Scheduler;
import java.util.HashSet;
import java.util.Set;
import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Valve;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class PeriodicAccessLogRotator implements LifecycleListener {

    private static final Log log = LogFactory.getLog(PeriodicAccessLogRotator.class);

    private Scheduler scheduler;
    private String cronExpression = "5 0 * * *"; // Default: just after midnight

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        // Use Lifecycle.START_EVENT and Lifecycle.STOP_EVENT
        if (Lifecycle.START_EVENT.equals(event.getType())) {
            if (event.getLifecycle() instanceof Engine engine) {
                startPeriodicRotation(engine);
            }
        } else if (Lifecycle.STOP_EVENT.equals(event.getType())) {
            stopPeriodicRotation();
        }
    }

    private void startPeriodicRotation(Engine engine) {
        scheduler = new Scheduler();
        // Schedule task using cron4j
        scheduler.schedule(cronExpression, () -> {
            try {
                Set<AccessLogValve> valves = new HashSet<>();
                collectValvesRecursively(engine, valves);

                for (AccessLogValve valve : valves) {
                    // Force log file rotation
                    if (valve.isRotatable()) {
                        log.info("Rotating log: " + valve.getPrefix() + valve.getSuffix());
                        valve.rotate();
                    }
                }
            } catch (Exception e) {
                log.error("Error rotating AccessLogValves", e);
            }
        });

        scheduler.start();
        if (log != null) {
            log.info("PeriodicAccessLogRotator started with cron expression: " + cronExpression);
        }
    }

    private void collectValvesRecursively(Container container, Set<AccessLogValve> valves) {
        if (container == null) return;

        // 1. Check current container's pipeline for AccessLogValves
        if (container.getPipeline() != null) {
            for (Valve valve : container.getPipeline().getValves()) {
                if (valve instanceof AccessLogValve) {
                    valves.add((AccessLogValve) valve);
                }
            }
        }

        // 2. Traverse down through child containers (Engine -> Hosts -> Contexts)
        for (Container child : container.findChildren()) {
            collectValvesRecursively(child, valves);
        }
    }

    private void stopPeriodicRotation() {
        if (scheduler != null && scheduler.isStarted()) {
            scheduler.stop();
        }
    }
}
