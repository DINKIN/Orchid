package com.eden.orchid.impl.publication;

import com.caseyjbrooks.clog.Clog;
import com.eden.common.util.EdenUtils;
import com.eden.orchid.api.OrchidContext;
import com.eden.orchid.api.options.annotations.Description;
import com.eden.orchid.api.options.annotations.Option;
import com.eden.orchid.api.publication.OrchidPublisher;
import com.eden.orchid.utilities.InputStreamPrinter;
import lombok.Getter;
import lombok.Setter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotBlank;
import java.io.File;
import java.util.concurrent.Executors;

@Description(value = "Run arbitrary shell scripts.", name = "Script")
public class ScriptPublisher extends OrchidPublisher {

    private final String resourcesDir;

    @Getter @Setter
    @Option
    @Description("The executable name")
    @NotBlank(message = "Must set the command to run.")
    private String[] command;

    @Getter @Setter
    @Option
    @Description("The working directory of the script to run")
    private String cwd;

    @Inject
    public ScriptPublisher(OrchidContext context, @Named("src") String resourcesDir) {
        super(context, "script", 100);
        this.resourcesDir = resourcesDir;
    }

    @Override
    public void publish() {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(command);

            String directory;
            if(!EdenUtils.isEmpty(cwd)) {
                directory = cwd;
                if(directory.startsWith("~")) {
                    directory = System.getProperty("user.home") + directory.substring(1);
                }
            }
            else {
                directory = resourcesDir;
            }

            builder.directory(new File(directory));

            Clog.i("[{}]> {}", directory, String.join(" ", command));

            Process process = builder.start();

            Executors.newSingleThreadExecutor().submit(new InputStreamPrinter(process.getInputStream()));
            process.waitFor();
        }
        catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}
