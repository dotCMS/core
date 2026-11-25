package com.dotmarketing.image.filter;

import com.dotmarketing.util.Logger;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.stream.ImageInputStream;

/**
 * Drop-in replacement for {@code ImageIO.read(File)} that fails instead of silently returning a
 * partially-decoded image. The default tolerant JPEG reader reports a truncated/corrupt read only
 * via {@link IIOReadWarningListener}, never by throwing — {@code ImageIO.read(File)} registers no
 * listener, so that corruption is otherwise invisible (black/grey rows, HTTP 200, cached forever).
 * Only truncation / short-read warnings are fatal: readers also warn about conditions they fully
 * recover from (bogus APPn segments, extraneous bytes before a marker), and those images are valid.
 */
public final class StrictImageReader {

    /** Lower-case fragments of the warnings image readers emit when the data stops early. */
    private static final String[] TRUNCATION_WARNINGS = {
            "premature end", "missing eoi", "truncated", "end of stream", "end of file", "eof"};

    private StrictImageReader() {
    }

    /**
     * Decodes the first image of the given file, failing on a truncated or undecodable read.
     *
     * @param file the image file to decode
     * @return the fully decoded image
     * @throws IOException if no reader handles the file, the decode fails, or the reader reports
     *                     truncated data
     */
    public static BufferedImage read(final File file) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            if (iis == null) {
                throw new IOException("no reader for file: " + file);
            }
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new IOException("no reader for file: " + file);
            }
            final ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                return read(reader, null, file);
            } finally {
                reader.dispose();
            }
        }
    }

    /**
     * Decodes the first image of a reader whose input is already set, failing on a truncation
     * warning or a {@code null} image. Other reader warnings are logged and the image is accepted.
     *
     * <p>A lone "Missing EOI marker" warning is not fatal: the JPEG decoder reports it for a file
     * that holds all its image data but lacks the final end marker, which browsers render fine. A
     * file that is really cut short always also reports "premature end of data segment".</p>
     *
     * @param reader reader whose input is already set
     * @param params read params, or {@code null} for the reader defaults
     * @param source what is being decoded, for messages only
     * @return the fully decoded image
     * @throws IOException if the decode fails or the reader reports truncated data
     */
    public static BufferedImage read(final ImageReader reader, @Nullable final ImageReadParam params,
            final Object source) throws IOException {
        final List<String> warnings = new ArrayList<>();
        final IIOReadWarningListener listener = (warningSource, message) -> warnings.add(message);
        reader.addIIOReadWarningListener(listener);
        try {
            final BufferedImage image = reader.read(0, params);
            for (final String warning : warnings) {
                if (isTruncationWarning(warning) && !isMissingEoiWarning(warning)) {
                    throw new IOException("corrupt or truncated image data while decoding " + source
                            + ": " + warning);
                }
            }
            if (image == null) {
                throw new IOException("unable to decode image: " + source);
            }
            if (!warnings.isEmpty()) {
                Logger.warn(StrictImageReader.class,
                        "recoverable warnings while decoding " + source + ": " + warnings);
            }
            return image;
        } finally {
            reader.removeIIOReadWarningListener(listener);
        }
    }

    /**
     * Tells whether a reader warning only says the JPEG end marker is missing, e.g. "Truncated File -
     * Missing EOI marker". On its own it means the image data is complete.
     *
     * @param message the reader warning, may be {@code null}
     * @return {@code true} if the warning is about the missing EOI marker
     */
    static boolean isMissingEoiWarning(@Nullable final String message) {
        return message != null && message.toLowerCase(Locale.ROOT).contains("missing eoi");
    }

    /**
     * Tells whether a reader warning means the data ended early (truncated or short read).
     *
     * @param message the reader warning, may be {@code null}
     * @return {@code true} if the warning reports truncation
     */
    static boolean isTruncationWarning(@Nullable final String message) {
        if (message == null) {
            return false;
        }
        final String lower = message.toLowerCase(Locale.ROOT);
        for (final String fragment : TRUNCATION_WARNINGS) {
            if (lower.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
