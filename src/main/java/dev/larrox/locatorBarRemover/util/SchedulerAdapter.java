package dev.larrox.locatorBarRemover.util;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public class SchedulerAdapter {

    private final Plugin plugin;
    private final FoliaSchedulerAccessor foliaAccessor;

    public SchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.foliaAccessor = FoliaSchedulerAccessor.create();
    }

    public void run(Runnable runnable) {
        if (runOnFoliaGlobalScheduler(runnable, 0L)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public void runLater(Runnable runnable, long delayTicks) {
        if (runOnFoliaGlobalScheduler(runnable, delayTicks)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
    }

    private boolean runOnFoliaGlobalScheduler(Runnable runnable, long delayTicks) {
        if (!foliaAccessor.available()) {
            return false;
        }

        try {
            Server server = Bukkit.getServer();
            Object scheduler = foliaAccessor.getGlobalRegionScheduler(server);
            Consumer<Object> consumer = ignored -> runnable.run();

            if (delayTicks <= 0L) {
                foliaAccessor.runNow(scheduler, plugin, consumer);
            } else {
                foliaAccessor.runDelayed(scheduler, plugin, consumer, delayTicks);
            }
            return true;
        } catch (IllegalAccessException | InvocationTargetException error) {
            plugin.getLogger().warning("Failed to use Folia scheduler, falling back to Bukkit scheduler: " + error.getMessage());
            return false;
        }
    }

    private record FoliaSchedulerAccessor(Method getGlobalRegionScheduler, Method run, Method runDelayed) {

        static FoliaSchedulerAccessor create() {
                try {
                    Server server = Bukkit.getServer();
                    Method globalRegionSchedulerMethod = server.getClass().getMethod("getGlobalRegionScheduler");
                    Class<?> schedulerClass = globalRegionSchedulerMethod.getReturnType();
                    Method runMethod = schedulerClass.getMethod("run", Plugin.class, Consumer.class);
                    Method runDelayedMethod = schedulerClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                    return new FoliaSchedulerAccessor(globalRegionSchedulerMethod, runMethod, runDelayedMethod);
                } catch (NoSuchMethodException ignored) {
                    return new FoliaSchedulerAccessor(null, null, null);
                }
            }

            boolean available() {
                return getGlobalRegionScheduler != null;
            }

            Object getGlobalRegionScheduler(Server server) throws InvocationTargetException, IllegalAccessException {
                return getGlobalRegionScheduler.invoke(server);
            }

            void runNow(Object scheduler, Plugin plugin, Consumer<Object> consumer) throws InvocationTargetException, IllegalAccessException {
                run.invoke(scheduler, plugin, consumer);
            }

            void runDelayed(Object scheduler, Plugin plugin, Consumer<Object> consumer, long delayTicks) throws InvocationTargetException, IllegalAccessException {
                runDelayed.invoke(scheduler, plugin, consumer, delayTicks);
            }
        }
}
