package org.matsim.core.utils.misc;

import org.apache.log4j.Level;

public class DiagnosticLog {
    public static Level info = Level.DEBUG;
    public static Level debug = Level.DEBUG;
    public static void setInfoLevel(Level logLevel) {
        DiagnosticLog.info = logLevel;
    }
    public static void setDebugLevel(Level logLevel) {
        DiagnosticLog.debug = logLevel;
    }
}
