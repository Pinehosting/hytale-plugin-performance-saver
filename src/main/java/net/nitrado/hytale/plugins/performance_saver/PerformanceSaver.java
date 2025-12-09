package net.nitrado.hytale.plugins.performance_saver;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import net.nitrado.hytale.plugins.performance_saver.gc.GcObserver;

import javax.annotation.Nonnull;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PerformanceSaver extends JavaPlugin {

    private GcObserver observer;
    private ScheduledFuture<?> hytaleTask;
    private int initialViewRadius;
    private long lastAdjustment = 0;

    public PerformanceSaver(@Nonnull JavaPluginInit init) {
        super(init);

        this.observer = new GcObserver();
    }

    @Override
    protected void setup() {
    }

    @Override
    protected void start() {
        this.initialViewRadius = HytaleServer.get().getConfig().getMaxViewRadius();
        getLogger().atInfo().log("Initial view radius is %d", this.initialViewRadius);
        try {
            this.observer.start();

            this.hytaleTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::checkGcRuns,
                5, // Initial delay
                1, // Interval
                TimeUnit.SECONDS
            );
        } catch (Exception e) {
            getLogger().atSevere().log("failed starting observer: %s", e.getMessage());
        }
    }

    protected void checkGcRuns() {
        var now = ManagementFactory.getRuntimeMXBean().getUptime();
        var gcRuns = this.observer.getRecentRuns();
        var totalHeap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
        var threshold = 0.85;

        if (totalHeap < 0) {
            return;
        }

        var matches = 0;
        for (var run : gcRuns.reversed()) {
            if (run.timeMs() < this.lastAdjustment) {
                getLogger().atFiner().log("breaking because not enough measurements since last adjustment");
                break;
            }

            if (run.timeMs() < now - 60 * 1000) {
                getLogger().atFiner().log("breaking because GC too long ago");
                break;
            }

            var relativeUsage = ((double) run.bytesAfter() / totalHeap);

            if (relativeUsage <  threshold) {
                getLogger().atFiner().log("breaking because still %.2f%% left", 100 * relativeUsage);
                break;
            }

            matches++;
        }

        getLogger().atFiner().log("GC run matches: %d",  matches);

        if (matches >= 3) {
            this.reduceViewRadius();
            return;
        }

        if (matches == 0 && now - this.lastAdjustment > 30 * 1000) {
            this.increaseViewSize();
        }

    }

    protected void reduceViewRadius() {
        var minViewRadius = 2;
        var factor = 0.75;

        var currentViewRadius = HytaleServer.get().getConfig().getMaxViewRadius();
        var newViewRadius = (int) Math.max(minViewRadius, Math.floor(factor * currentViewRadius));

        if (newViewRadius >= currentViewRadius) {
            return;
        }

        Universe.get().sendMessage(Message.raw("Memory critical. Reducing view radius to " + newViewRadius));
        getLogger().atSevere().log("Memory critical. Reducing view radius to " + newViewRadius);

        this.lastAdjustment = ManagementFactory.getRuntimeMXBean().getUptime();
        HytaleServer.get().getConfig().setMaxViewRadius(newViewRadius);
    }

    protected void increaseViewSize() {
        var currentViewRadius = HytaleServer.get().getConfig().getMaxViewRadius();
        var newViewRadius = Math.min(currentViewRadius + 1, this.initialViewRadius);

        if (newViewRadius > currentViewRadius) {
            Universe.get().sendMessage(Message.raw("Increasing view radius back to " + newViewRadius));
            getLogger().atInfo().log("Increasing view radius back to " + newViewRadius);
            this.lastAdjustment = ManagementFactory.getRuntimeMXBean().getUptime();
            HytaleServer.get().getConfig().setMaxViewRadius(newViewRadius);
        }
    }

    @Override
    protected void shutdown() {
        getLogger().atInfo().log("Restoring view radius to %d", this.initialViewRadius);
        HytaleServer.get().getConfig().setMaxViewRadius(this.initialViewRadius);

        try {
            this.observer.stop();

        } catch (Exception e) {
            getLogger().atSevere().log("failed stopping observer: %s", e.getMessage());
        }
    }
}