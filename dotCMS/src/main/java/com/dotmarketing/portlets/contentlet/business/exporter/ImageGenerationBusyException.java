package com.dotmarketing.portlets.contentlet.business.exporter;

import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;

/** The instance has no free image generation permits. */
public final class ImageGenerationBusyException extends BinaryContentExporterException {

    /** Signal temporary unavailability without returning the original as a rendition. */
    ImageGenerationBusyException() {
        super("Image generation is at capacity");
    }
}
