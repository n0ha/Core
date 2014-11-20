package org.eclipse.persistence;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.tools.profiler.PerformanceProfiler;

/**
 *
 * @author Škoda Petr
 */
public class PerformanceSessionCustomizer implements SessionCustomizer {

    @Override
    public void customize(Session session) throws Exception {
        // Set writer for logs - times are in nano seconds!! (10^-9)
        if (System.getProperty("eclipse-link.log") != null) {
            session.setLog(new OutputStreamWriter(new FileOutputStream(System.getProperty("eclipse-link.log"))));

            // https://docs.oracle.com/middleware/1212/toplink/TLADG/performance.htm#TLADG446
            // http://wiki.eclipse.org/EclipseLink/UserGuide/JPA/Advanced_JPA_Development/Performance/Performance_Monitoring_and_Profiling/Performance_Profiling
            final PerformanceProfiler profiler = new PerformanceProfiler();
            session.setProfiler(profiler);
        }

        // Monitor for group fetching
        // Enable this monitor using the System property org.eclipse.persistence.fetchgroupmonitor=true.

        // QueryMonitor
        // The monitor dumps the number of query cache hits and executions (misses) once every 100 seconds.
    }

}
