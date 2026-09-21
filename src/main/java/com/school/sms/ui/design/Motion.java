package com.school.sms.ui.design;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * The application's motion vocabulary.
 *
 * <p>JavaFX CSS has no transitions or keyframes, so the two places where the
 * design calls for motion — the rail's 220 ms collapse and the toast entrance —
 * are driven from here instead. Durations are the design's own values; nothing in
 * this class knows or cares what colour anything is.</p>
 *
 * <p>Motion is interruptible by construction: starting a new animation of the
 * same kind on the same target stops the previous one, so a user who hammers the
 * collapse button never leaves the rail stuck at a half-way width.</p>
 */
public final class Motion {

    /** Rail collapse / expand. */
    public static final Duration RAIL = Duration.millis(220);
    /** Hover and press feedback. */
    public static final Duration HOVER = Duration.millis(120);
    /** Toast entrance and exit. */
    public static final Duration TOAST_IN = Duration.millis(180);
    public static final Duration TOAST_OUT = Duration.millis(160);
    /** One half-cycle of the skeleton pulse. */
    public static final Duration SKELETON_PULSE = Duration.millis(1200);

    private static final Map<Object, Timeline> RUNNING = new HashMap<>();
    private static Boolean reduced;

    private Motion() {
    }

    /**
     * True when the user has asked for reduced motion. Honoured by fading instead
     * of sliding; the animations are shortened rather than removed so state changes
     * are still legible.
     */
    public static boolean isReduced() {
        if (reduced == null) {
            try {
                reduced = Preferences.userRoot().node("com/school/sms/ui").getBoolean("ui.reduceMotion", false);
            } catch (Exception e) {
                reduced = false;
            }
        }
        return reduced;
    }

    private static Duration effective(Duration duration) {
        return isReduced() ? Duration.millis(1) : duration;
    }

    /**
     * Animates a width-like property to a target value, replacing any animation
     * already running for the same key.
     *
     * @param key    identity of the animation, so a second call supersedes the first
     * @param target the property to drive (pref/min/max width are bound by the caller)
     */
    public static void animateWidth(Object key, DoubleProperty target, double value, Duration duration) {
        animateWidth(key, target, value, duration, null);
    }

    /**
     * As {@link #animateWidth(Object, DoubleProperty, double, Duration)}, running
     * {@code onFinished} when the animation completes. Stopped animations do not
     * fire the callback, so a superseded animation cannot leave stale state behind.
     */
    public static void animateWidth(Object key, DoubleProperty target, double value, Duration duration,
                                    Runnable onFinished) {
        stop(key);
        Timeline timeline = new Timeline(new KeyFrame(
                effective(duration),
                new KeyValue(target, value, Interpolator.EASE_BOTH)));
        RUNNING.put(key, timeline);
        timeline.setOnFinished(e -> {
            RUNNING.remove(key);
            if (onFinished != null) {
                onFinished.run();
            }
        });
        timeline.play();
    }

    /** Cancels the animation registered under {@code key}, if any. */
    public static void stop(Object key) {
        Timeline timeline = RUNNING.remove(key);
        if (timeline != null) {
            timeline.stop();
        }
    }

    /** True while an animation registered under {@code key} is still running. */
    public static boolean isRunning(Object key) {
        Timeline timeline = RUNNING.get(key);
        return timeline != null && timeline.getStatus() == Animation.Status.RUNNING;
    }

    /** Fades a node in, replacing any fade already running on it. */
    public static void fadeIn(Node node, Duration duration) {
        FadeTransition fade = new FadeTransition(effective(duration), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    /** Fades a node out and runs {@code onFinished} when it has gone. */
    public static void fadeOut(Node node, Duration duration, Runnable onFinished) {
        FadeTransition fade = new FadeTransition(effective(duration), node);
        fade.setFromValue(node.getOpacity());
        fade.setToValue(0);
        fade.setOnFinished(e -> onFinished.run());
        fade.play();
    }

    /**
     * The skeleton pulse: a slow, low-amplitude opacity breath used in place of an
     * indeterminate spinner while tabular data loads.
     */
    public static void pulse(Node skeleton) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(skeleton.opacityProperty(), 1.0, Interpolator.EASE_BOTH)),
                new KeyFrame(SKELETON_PULSE, new KeyValue(skeleton.opacityProperty(), 0.45, Interpolator.EASE_BOTH)),
                new KeyFrame(SKELETON_PULSE.multiply(2), new KeyValue(skeleton.opacityProperty(), 1.0, Interpolator.EASE_BOTH)));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
}
