package com.eden.orchid.api;

import com.eden.common.json.JSONElement;
import com.eden.common.util.EdenUtils;
import com.eden.orchid.Orchid;
import com.eden.orchid.api.compilers.CompilerService;
import com.eden.orchid.api.events.EventService;
import com.eden.orchid.api.generators.GeneratorService;
import com.eden.orchid.api.indexing.IndexService;
import com.eden.orchid.api.options.OptionsService;
import com.eden.orchid.api.publication.PublicationService;
import com.eden.orchid.api.registration.InjectionService;
import com.eden.orchid.api.render.RenderService;
import com.eden.orchid.api.resources.ResourceService;
import com.eden.orchid.api.site.OrchidSite;
import com.eden.orchid.api.tasks.TaskService;
import com.eden.orchid.api.theme.ThemeService;
import lombok.Getter;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
@Getter
public final class OrchidContextImpl implements OrchidContext {

    private OrchidSite site;

    private Map<Class<? extends OrchidService>, OrchidService> services;

    @Inject
    public OrchidContextImpl(
            OrchidSite site,
            CompilerService compilerService,
            ThemeService themeService,
            EventService eventService,
            IndexService indexService,
            ResourceService resourceService,
            TaskService taskService,
            OptionsService optionsService,
            GeneratorService generatorService,
            RenderService renderService,
            PublicationService publicationService,
            InjectionService injectionService,

            Set<OrchidService> additionalServices
    ) {
        Orchid.getInstance().setState(Orchid.State.BOOTSTRAP);
        this.site = site;

        services = new HashMap<>();
        initializeService(OrchidSite.class,         site);
        initializeService(CompilerService.class,    compilerService);
        initializeService(ThemeService.class,       themeService);
        initializeService(EventService.class,       eventService);
        initializeService(IndexService.class,       indexService);
        initializeService(ResourceService.class,    resourceService);
        initializeService(TaskService.class,        taskService);
        initializeService(OptionsService.class,     optionsService);
        initializeService(GeneratorService.class,   generatorService);
        initializeService(RenderService.class,      renderService);
        initializeService(PublicationService.class, publicationService);
        initializeService(InjectionService.class,   injectionService);

        for(OrchidService service : additionalServices) {
            services.put(service.getClass(), service);
        }

        initialize(this);

        Orchid.getInstance().setState(Orchid.State.IDLE);
    }

// Service Delegation
//----------------------------------------------------------------------------------------------------------------------

    private <T extends OrchidService> void initializeService(Class<T> serviceClass, T service) {
        services.put(serviceClass, service);
    }

    @Override
    public void initialize(OrchidContext context) {
        services.values().forEach(service -> service.initialize(context));
        broadcast(Orchid.Lifecycle.InitComplete.fire(this));
    }

    @Override
    public void start() {
        services.values().forEach(OrchidService::onStart);
        broadcast(Orchid.Lifecycle.OnStart.fire(this));
        services.values().forEach(OrchidService::onPostStart);
    }

    @Override
    public void finish() {
        broadcast(Orchid.Lifecycle.OnFinish.fire(this));
        services.values().forEach(OrchidService::onFinish);
        broadcast(Orchid.Lifecycle.Shutdown.fire(this));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends OrchidService> T getService(Class<T> serviceClass) {
        return (T) services.get(serviceClass);
    }

    @Override
    public Collection<OrchidService> getServices() {
        return services.values();
    }

    @Override
    public void extractServiceOptions() {
        services.values().forEach(service -> {
            JSONElement el = query(service.optionsQuery());
            if (EdenUtils.elementIsObject(el)) {
                service.extractOptions(this, (JSONObject) el.getElement());
            }
            else {
                service.extractOptions(this, new JSONObject());
            }
        });
    }

}
