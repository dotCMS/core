package com.dotcms.util;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Prints one of the startup banners from {@code /ascii-art.txt}. Keeping the art in a resource
 * rather than in text blocks means no backslash escaping, real trailing spaces, and nothing
 * retained in the constant pool once startup is done.
 */
public class AsciiArt {

	private static final String ART_RESOURCE = "/ascii-art.txt";
	private static final String SEPARATOR = "\n%%%\n";
	private static final AtomicBoolean artDone = new AtomicBoolean(false);

	public static void doArt() {

		if (!artDone.compareAndSet(false, true)) {
			return;
		}

		final String[] art = load();
		if (art.length == 0) {
			return;
		}

		if (Config.getBooleanProperty("SHOW_ALL_ASCII_ART", false)) {
			for (final String banner : art) {
				print(banner);
				Logger.info(AsciiArt.class, "------------------------------------");
			}
		} else {
			print(art[(int) (System.currentTimeMillis() % art.length)]);
		}
	}

	private static String[] load() {
		try (final InputStream in = AsciiArt.class.getResourceAsStream(ART_RESOURCE)) {
			return in == null
					? new String[0]
					: new String(in.readAllBytes(), StandardCharsets.UTF_8).split(SEPARATOR);
		} catch (final IOException e) {
			Logger.debug(AsciiArt.class, "unable to read " + ART_RESOURCE + ": " + e.getMessage());
			return new String[0];
		}
	}

	/**
	 * Leading newlines so the art starts in column 0 — otherwise the logger prefix shifts the
	 * first row right and the banner looks broken.
	 */
	private static void print(final String art) {
		Logger.info(AsciiArt.class, "\n\n" + art + "\n");
	}

}
