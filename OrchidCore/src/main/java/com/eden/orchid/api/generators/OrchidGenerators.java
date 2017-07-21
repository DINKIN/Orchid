package com.eden.orchid.api.generators;

import com.caseyjbrooks.clog.Clog;
import com.eden.common.json.JSONElement;
import com.eden.common.util.EdenUtils;
import com.eden.orchid.api.OrchidContext;
import com.eden.orchid.api.indexing.OrchidIndex;
import com.eden.orchid.api.options.OptionsHolder;
import com.eden.orchid.api.resources.OrchidResources;
import com.eden.orchid.api.resources.resource.FreeableResource;
import com.eden.orchid.api.theme.pages.OrchidPage;
import com.eden.orchid.impl.indexing.OrchidCompositeIndex;
import com.eden.orchid.impl.indexing.OrchidInternalIndex;
import com.eden.orchid.impl.indexing.OrchidRootExternalIndex;
import com.eden.orchid.impl.indexing.OrchidRootInternalIndex;
import com.eden.orchid.utilities.ObservableTreeSet;
import com.eden.orchid.utilities.OrchidUtils;
import com.eden.orchid.utilities.PrioritizedSetFilter;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Singleton
public final class OrchidGenerators {

    private Set<OrchidGenerator> allGenerators;
    private Set<OrchidGenerator> generators;
    private OrchidContext context;
    private OrchidResources orchidResources;

    private OrchidRootInternalIndex internalIndex;
    private OrchidRootExternalIndex externalIndex;
    private OrchidCompositeIndex compositeIndex;

    private boolean renderParallel = false;

    @Inject
    public OrchidGenerators(OrchidContext context, Set<OrchidGenerator> generators, OrchidResources orchidResources) {
        this.context = context;
        this.allGenerators = new ObservableTreeSet<>(generators);
        this.orchidResources = orchidResources;
    }

    public void startIndexing() {
        this.generators = new PrioritizedSetFilter<>(context, "generators", this.allGenerators).getFilteredSet();

        buildInternalIndex();
        buildExternalIndex();
        mergeIndices(this.internalIndex, this.externalIndex);
    }

    public void startGeneration() {
        generators.stream()
                  .forEach(this::useGenerator);
    }

// Indexing phase
//----------------------------------------------------------------------------------------------------------------------

    private void buildInternalIndex() {
        this.internalIndex = new OrchidRootInternalIndex();
        generators.stream()
                  .forEach(this::indexGenerator);
    }

    private void indexGenerator(OrchidGenerator generator) {
        Clog.d("Indexing generator: #{$1}:[#{$2 | className}]", generator.getPriority(), generator);

        if(generator instanceof OptionsHolder) {
            JSONElement el = context.query("options." + generator.getKey());
            if(OrchidUtils.elementIsObject(el)) {
                ((OptionsHolder) generator).extractOptions(context, (JSONObject) el.getElement());
            }
            else {
                ((OptionsHolder) generator).extractOptions(context, new JSONObject());
            }
        }

        List<? extends OrchidPage> generatorPages = generator.startIndexing();

        if (!EdenUtils.isEmpty(generator.getKey()) && generatorPages != null && generatorPages.size() > 0) {
            OrchidInternalIndex index = new OrchidInternalIndex(generator.getKey());
            for(OrchidPage page : generatorPages) {
                index.addToIndex(generator.getKey() + "/" + page.getReference().getPath(), page);
                if(page.getResource() instanceof FreeableResource) {
                    ((FreeableResource) page.getResource()).free();
                }
            }
            this.internalIndex.addChildIndex(generator.getKey(), index);
        }
    }

    private void buildExternalIndex() {
        this.externalIndex = new OrchidRootExternalIndex();

        JSONElement externalIndexReferences = context.query("options.externalIndex");

        if(OrchidUtils.elementIsArray(externalIndexReferences)) {
            JSONArray externalIndex = (JSONArray) externalIndexReferences.getElement();

            for (int i = 0; i < externalIndex.length(); i++) {
                JSONObject indexJson = this.orchidResources.loadAdditionalFile(externalIndex.getString(i));
                if(indexJson != null) {
                    OrchidIndex index = OrchidIndex.fromJSON(context, indexJson);
                    this.externalIndex.addChildIndex(index);
                }
            }
        }
    }

    private void mergeIndices(OrchidIndex... indices) {
        this.compositeIndex = new OrchidCompositeIndex("composite");
        for(OrchidIndex index : indices) {
            if(index != null) {
                this.compositeIndex.mergeIndex(index);
            }
        }
    }

// Generation Phase
//----------------------------------------------------------------------------------------------------------------------

    private void useGenerator(OrchidGenerator generator) {
        Clog.d("Using generator: #{$1}:[#{$2 | className}]", generator.getPriority(), generator);

        List<? extends OrchidPage> generatorPages = null;
        if(!EdenUtils.isEmpty(generator.getKey())) {
            generatorPages = internalIndex.getGeneratorPages(generator.getKey());
        }
        if(generatorPages == null) {
            generatorPages = new ArrayList<>();
        }
        if(renderParallel) {
            generator.startGeneration(generatorPages);
        }
        else {
            generator.startGeneration(generatorPages);
        }
    }

    public Set<OrchidGenerator> getAllGenerators() {
        return allGenerators;
    }

    public void setAllGenerators(Set<OrchidGenerator> allGenerators) {
        this.allGenerators = allGenerators;
    }

    public Set<OrchidGenerator> getGenerators() {
        return generators;
    }

    public void setGenerators(Set<OrchidGenerator> generators) {
        this.generators = generators;
    }

    public OrchidContext getContext() {
        return context;
    }

    public void setContext(OrchidContext context) {
        this.context = context;
    }

    public OrchidResources getOrchidResources() {
        return orchidResources;
    }

    public void setOrchidResources(OrchidResources orchidResources) {
        this.orchidResources = orchidResources;
    }

    public OrchidRootInternalIndex getInternalIndex() {
        return internalIndex;
    }

    public void setInternalIndex(OrchidRootInternalIndex internalIndex) {
        this.internalIndex = internalIndex;
    }

    public OrchidRootExternalIndex getExternalIndex() {
        return externalIndex;
    }

    public void setExternalIndex(OrchidRootExternalIndex externalIndex) {
        this.externalIndex = externalIndex;
    }

    public OrchidCompositeIndex getCompositeIndex() {
        return compositeIndex;
    }

    public void setCompositeIndex(OrchidCompositeIndex compositeIndex) {
        this.compositeIndex = compositeIndex;
    }
}