package com.eden.orchid.api.theme;

import com.eden.orchid.api.OrchidContext;
import com.eden.orchid.api.options.OptionsExtractor;
import com.eden.orchid.api.options.OptionsHolder;
import com.eden.orchid.api.render.OrchidRenderer;
import com.eden.orchid.api.resources.resourceSource.DefaultResourceSource;
import com.eden.orchid.api.theme.assets.AssetHolder;
import com.eden.orchid.api.theme.assets.ThemeAssetHolder;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

public abstract class AbstractTheme extends DefaultResourceSource implements OptionsHolder, AssetHolder {

    protected AssetHolder assets;

    @Getter @Setter
    protected JSONObject options;

    @Getter @Setter
    protected boolean hasRenderedAssets;

    public AbstractTheme(OrchidContext context, int priority) {
        super(context, priority);
        this.assets = new ThemeAssetHolder(context);
    }

    public void extractOptions(OrchidContext context, JSONObject options) {
        this.options = new JSONObject(options);
        OptionsExtractor extractor = context.getInjector().getInstance(OptionsExtractor.class);
        extractor.extractOptions(this, options);
    }

    public abstract String getKey();

    public void clearCache() {
        assets.clearAssets();
        hasRenderedAssets = false;
    }

    public void initialize() {

    }

    protected void addAssets() {

    }

    public void renderAssets() {
        if (!hasRenderedAssets) {
            OrchidRenderer renderer = context.getInjector().getInstance(OrchidRenderer.class);
            getScripts()
                    .stream()
                    .forEach(renderer::renderRaw);
            getStyles()
                    .stream()
                    .forEach(renderer::renderRaw);
            hasRenderedAssets = true;
        }
    }

// Get delegates
//----------------------------------------------------------------------------------------------------------------------

    @Override
    public AssetHolder getAssetHolder() {
        return assets;
    }
}