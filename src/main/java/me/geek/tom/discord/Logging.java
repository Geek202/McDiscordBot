package me.geek.tom.discord;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class Logging {

    public static final Marker BOT = MarkerManager.getMarker("BOT");
    public static final Marker COMMAND = MarkerManager.getMarker("COMMAND");
    public static final Marker LAUNCH = MarkerManager.getMarker("LAUNCH");
    public static final Marker SETUP = MarkerManager.getMarker("SETUP");

}
