package com.eden.orchid.api.tasks;

import com.caseyjbrooks.clog.Clog;
import com.eden.orchid.Orchid;
import com.eden.orchid.api.OrchidContext;
import com.eden.orchid.api.events.On;
import com.eden.orchid.api.events.OrchidEvent;
import com.eden.orchid.api.events.OrchidEventListener;
import com.eden.orchid.api.options.annotations.Description;
import com.eden.orchid.api.options.annotations.IntDefault;
import com.eden.orchid.api.options.annotations.Option;
import com.eden.orchid.api.server.FileWatcher;
import com.eden.orchid.api.server.OrchidServer;
import com.eden.orchid.utilities.OrchidUtils;
import com.google.inject.name.Named;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * @since v1.0.0
 * @orchidApi services
 */
@Singleton
public final class TaskServiceImpl implements TaskService, OrchidEventListener {

    private OrchidContext context;
    private final Set<OrchidTask> tasks;
    private final Set<OrchidCommand> commands;

    private final OrchidServer server;
    private final FileWatcher watcher;

    private final String task;
    private final String resourcesDir;

    @Getter private TaskType taskType;

    private long lastBuild;

    @Getter @Setter
    @Option @IntDefault(1)
    @Description("The minimum time, in seconds, to wait in between builds.")
    private int watchDebounceTimeout;

    @Inject
    public TaskServiceImpl(
            Set<OrchidTask> tasks,
            Set<OrchidCommand> commands,
            @Named("task") String task,
            @Named("resourcesDir") String resourcesDir,
            OrchidServer server,
            FileWatcher watcher) {
        this.tasks = new TreeSet<>(tasks);
        this.commands = new TreeSet<>(commands);

        this.server = server;
        this.watcher = watcher;

        this.task = task;
        this.resourcesDir = resourcesDir;

        this.lastBuild = 0;
    }

    @Override
    public void initialize(OrchidContext context) {
        this.context = context;
    }

    @Override
    public void initOptions() {
        context.clearOptions();
        context.broadcast(Orchid.Lifecycle.ClearCache.fire(this));
        context.loadOptions();

        context.extractServiceOptions();
    }

    @Override
    public void onPostStart() {
        runTask(task);
    }

    @Override
    public boolean runTask(String taskName) {
        OrchidTask foundTask = tasks
                .stream()
                .sorted()
                .filter(task -> task.getName().equals(taskName))
                .findFirst()
                .orElse(null);

        if (foundTask != null) {
            taskType = foundTask.getTaskType();
            context.broadcast(Orchid.Lifecycle.TaskStart.fire(this));
            foundTask.run();
            context.broadcast(Orchid.Lifecycle.TaskFinish.fire(this));
            return true;
        }
        else {
            Clog.e("Could not find task {} to run", taskName);
            return false;
        }
    }

    @Override
    public boolean runCommand(String input) {
        String[] inputPieces = input.split("\\s+");
        String commandName = inputPieces[0];
        String commandArgs = String.join(" ", Arrays.copyOfRange(inputPieces, 1, inputPieces.length));

        if(!Orchid.getInstance().getState().isWorkingState()) {
            OrchidCommand foundCommand = commands
                    .stream()
                    .sorted()
                    .filter(command -> command.getKey().equalsIgnoreCase(commandName))
                    .findFirst()
                    .orElse(null);

            if (foundCommand != null) {
                OrchidCommand freshCommand = context.resolve(foundCommand.getClass());

                JSONObject paramsJSON = OrchidUtils.parseCommandArgs(commandArgs, freshCommand.parameters());

                freshCommand.extractOptions(context, paramsJSON);

                try {
                    freshCommand.run(commandName);
                    return true;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                Clog.e("Could not find matching command for {}", commandName);
            }
        }
        else {
            Clog.i("Orchid is currently busy, please wait for the current job to finish then try again.");
        }

        return false;
    }

    @Override
    public void build() {
        if(!Orchid.getInstance().getState().isBuildState()) {
            long secondsSinceLastBuild = (System.currentTimeMillis() - lastBuild)/1000;

            if(secondsSinceLastBuild > watchDebounceTimeout) {
                Orchid.getInstance().setState(Orchid.State.BUILD_PREP);
                context.broadcast(Orchid.Lifecycle.BuildStart.fire(this));
                initOptions();

                context.clearThemes();
                context.pushTheme(context.getDefaultTheme());

                if(taskType == TaskType.SERVE) {
                    context.clearAdminThemes();
                    context.pushAdminTheme(context.getDefaultAdminTheme());
                }

                Clog.i("Build Starting...");

                Orchid.getInstance().setState(Orchid.State.INDEXING);
                context.broadcast(Orchid.Lifecycle.IndexingStart.fire(this));
                context.startIndexing();
                context.broadcast(Orchid.Lifecycle.IndexingFinish.fire(this));

                Orchid.getInstance().setState(Orchid.State.BUILDING);
                context.broadcast(Orchid.Lifecycle.GeneratingStart.fire(this));
                context.startGeneration();
                context.broadcast(Orchid.Lifecycle.GeneratingFinish.fire(this));

                context.broadcast(Orchid.Lifecycle.BuildFinish.fire(this));

                lastBuild = System.currentTimeMillis();

                Clog.i("Build Complete");
                Orchid.getInstance().setState(Orchid.State.IDLE);
            }
        }
        else {
            Clog.e("Build already in progress, skipping.");
        }
    }

    @Override
    public void watch() {
        watcher.startWatching(resourcesDir);
    }

    @Override
    public void serve() {
        try {
            server.start(8080);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deploy(boolean dryDeploy) {
        initOptions();

        Orchid.getInstance().setState(Orchid.State.DEPLOYING);
        Clog.i("Deploy Starting...");
        context.broadcast(Orchid.Lifecycle.DeployStart.fire(this));
        boolean success = context.publishAll(dryDeploy);
        context.broadcast(Orchid.Lifecycle.DeployFinish.fire(this));
        Orchid.getInstance().setState(Orchid.State.IDLE);

        if(success) {
            Clog.i("Deployment completed successfully");
        }
        else {
            Clog.w("Deployment failed");
        }
    }

// Build Events
//----------------------------------------------------------------------------------------------------------------------

    @On(Orchid.Lifecycle.FilesChanged.class)
    public void onFilesChanges(Orchid.Lifecycle.FilesChanged event) {
        if (server != null && server.getWebsocket() != null) {
            server.getWebsocket().sendMessage("Files Changed", "");
        }
        context.build();
    }

    @On(Orchid.Lifecycle.EndSession.class)
    public void onEndSession(Orchid.Lifecycle.EndSession event) {
        if (server != null && server.getWebsocket() != null) {
            server.getWebsocket().sendMessage("Ending Session", "");
        }
        context.broadcast(Orchid.Lifecycle.Shutdown.fire(this));
        System.exit(0);
    }

    @On(value = OrchidEvent.class, subclasses = true)
    public void onAnyEvent(OrchidEvent event) {
        if (server != null && server.getWebsocket() != null) {
            server.getWebsocket().sendMessage(event.getType(), event.toString());
        }
    }

}
