package com.dotcms.test.util.assertion;

import com.dotcms.variant.model.Variant;

import java.io.File;
import java.util.Map;

/**
 * {@link AssertionChecker} concrete class for {@link Variant}
 */
public class VariantChecker implements AssertionChecker<Variant> {

    @Override
    public Map<String, Object> getFileArguments(final Variant variant, final File file) {
        return Map.of(
                "name", variant.name(),
                "description", variant.description().get()
        );

    }

    @Override
    public String getFilePathExpected(final File file) {
        return "/bundlers-test/variant/variant.json";
    }

    public File getFileInner(Variant asset, File bundleRoot) {
        final String path = bundleRoot.getPath() + File.separator + "live" + File.separator +
                "System Host" + File.separator + asset.name() + ".variant.json";

        return new File(path);
    }

}
