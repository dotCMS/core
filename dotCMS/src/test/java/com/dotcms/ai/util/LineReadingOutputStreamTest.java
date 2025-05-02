package com.dotcms.ai.util;

import static org.junit.Assert.assertEquals;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Unit tests for {@link LineReadingOutputStream} to ensure it properly handles UTF-8 characters
 * and doesn't corrupt the encoding during streaming.
 */
public class LineReadingOutputStreamTest {

    /**
     * Test that the LineReadingOutputStream correctly handles basic ASCII text.
     */
    @Test
    public void testBasicAsciiText() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineReadingOutputStream lros = new LineReadingOutputStream(baos);

        String input = "Hello, world!\n";
        lros.write(input.getBytes(StandardCharsets.UTF_8));
        lros.close();

        String output = baos.toString(StandardCharsets.UTF_8.name());
        assertEquals("Hello, world!\n", output);
    }

    /**
     * Test that the LineReadingOutputStream correctly handles UTF-8 characters.
     */
    @Test
    public void testUtf8Characters() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineReadingOutputStream lros = new LineReadingOutputStream(baos);

        // String with various UTF-8 characters
        String input = "こんにちは世界! Привет, мир! 你好，世界! مرحبا بالعالم!\n";
        lros.write(input.getBytes(StandardCharsets.UTF_8));
        lros.close();

        String output = baos.toString(StandardCharsets.UTF_8.name());
        assertEquals(input, output);
    }

    /**
     * Test that the LineReadingOutputStream correctly handles multiple lines with UTF-8 characters.
     */
    @Test
    public void testMultipleUtf8Lines() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineReadingOutputStream lros = new LineReadingOutputStream(baos);

        // Multiple lines with UTF-8 characters
        String line1 = "こんにちは世界!\n";
        String line2 = "Привет, мир!\n";
        String line3 = "你好，世界!\n";
        String line4 = "مرحبا بالعالم!\n";

        lros.write(line1.getBytes(StandardCharsets.UTF_8));
        lros.write(line2.getBytes(StandardCharsets.UTF_8));
        lros.write(line3.getBytes(StandardCharsets.UTF_8));
        lros.write(line4.getBytes(StandardCharsets.UTF_8));
        lros.close();

        String output = baos.toString(StandardCharsets.UTF_8.name());
        String expected = line1 + line2 + line3 + line4;
        assertEquals(expected, output);
    }

    /**
     * Test that the LineReadingOutputStream correctly handles UTF-8 characters split across multiple writes.
     */
    @Test
    public void testSplitUtf8Characters() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineReadingOutputStream lros = new LineReadingOutputStream(baos);

        // UTF-8 string with multi-byte characters
        String fullString = "こんにちは世界!\n";
        byte[] fullBytes = fullString.getBytes(StandardCharsets.UTF_8);

        // Split the byte array into multiple chunks to simulate multiple writes
        int chunkSize = 3; // Small chunk size to ensure we split multi-byte characters
        for (int i = 0; i < fullBytes.length; i += chunkSize) {
            int length = Math.min(chunkSize, fullBytes.length - i);
            lros.write(fullBytes, i, length);
        }
        lros.close();

        String output = baos.toString(StandardCharsets.UTF_8.name());
        assertEquals(fullString, output);
    }

    /**
     * Test that the LineReadingOutputStream correctly handles JSON with UTF-8 characters.
     */
    @Test
    public void testJsonWithUtf8() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineReadingOutputStream lros = new LineReadingOutputStream(baos);

        // JSON string with UTF-8 characters
        String json = "{\"message\": \"こんにちは世界! Привет, мир! 你好，世界! مرحبا بالعالم!\"}\n";
        lros.write(json.getBytes(StandardCharsets.UTF_8));
        lros.close();

        String output = baos.toString(StandardCharsets.UTF_8.name());
        assertEquals(json, output);
    }

    /**
     * Test that the LineReadingOutputStream correctly handles streaming JSON with UTF-8 characters.
     */
    @Test
    public void testStreamingJsonWithUtf8() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineReadingOutputStream lros = new LineReadingOutputStream(baos);

        // Simulate OpenAI streaming response format
        String[] jsonLines = {
            "data: {\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"created\":1694268190,\"model\":\"gpt-3.5-turbo-0613\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"こんにちは\"},\"finish_reason\":null}]}\n",
            "data: {\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"created\":1694268190,\"model\":\"gpt-3.5-turbo-0613\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"世界\"},\"finish_reason\":null}]}\n",
            "data: {\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"created\":1694268190,\"model\":\"gpt-3.5-turbo-0613\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"! Привет\"},\"finish_reason\":null}]}\n",
            "data: {\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"created\":1694268190,\"model\":\"gpt-3.5-turbo-0613\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\", мир!\"},\"finish_reason\":null}]}\n",
            "data: {\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"created\":1694268190,\"model\":\"gpt-3.5-turbo-0613\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\" 你好，世界!\"},\"finish_reason\":null}]}\n",
            "data: {\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"created\":1694268190,\"model\":\"gpt-3.5-turbo-0613\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\" مرحبا بالعالم!\"},\"finish_reason\":null}]}\n",
            "data: {\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"created\":1694268190,\"model\":\"gpt-3.5-turbo-0613\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n",
            "data: [DONE]\n"
        };

        for (String line : jsonLines) {
            lros.write(line.getBytes(StandardCharsets.UTF_8));
        }
        lros.close();

        String output = baos.toString(StandardCharsets.UTF_8.name());
        String expected = String.join("", jsonLines);
        assertEquals(expected, output);
    }


    /**
     * Test that the LineReadingOutputStream correctly handles large UTF-8 content that exceeds the buffer size.
     */
    @Test
    public void testLargeUtf8Content() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineReadingOutputStream lros = new LineReadingOutputStream(baos);

        // Create a large string with repeated UTF-8 characters
        StringBuilder largeStringBuilder = new StringBuilder();
        String repeatedUtf8 = "こんにちは世界! Привет, мир! 你好，世界! مرحبا بالعالم! ";
        // Repeat the string to create content larger than the 8KB buffer
        for (int i = 0; i < 1000; i++) {
            largeStringBuilder.append(repeatedUtf8);
        }
        largeStringBuilder.append("\n");
        String largeString = largeStringBuilder.toString();

        lros.write(largeString.getBytes(StandardCharsets.UTF_8));
        lros.close();

        String output = baos.toString(StandardCharsets.UTF_8.name());
        assertEquals(largeString, output);
    }

    /**
     * Test that the LineReadingOutputStream correctly handles byte-by-byte writing of UTF-8 characters.
     */
    @Test
    public void testByteByByteUtf8Writing() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineReadingOutputStream lros = new LineReadingOutputStream(baos);

        // UTF-8 string with multi-byte characters
        String utf8String = "こんにちは世界!\n";
        byte[] utf8Bytes = utf8String.getBytes(StandardCharsets.UTF_8);

        // Write byte by byte
        for (byte b : utf8Bytes) {
            lros.write(b);
        }
        lros.close();

        String output = baos.toString(StandardCharsets.UTF_8.name());
        assertEquals(utf8String, output);
    }

    /**
     * Test that the LineReadingOutputStream correctly handles emoji characters (which are 4-byte UTF-8 sequences).
     */
    @Test
    public void testEmojiCharacters() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineReadingOutputStream lros = new LineReadingOutputStream(baos);

        // String with emoji characters (4-byte UTF-8 sequences)
        String emojiString = "Hello 😀 World 🌍 Test 🚀\n";
        lros.write(emojiString.getBytes(StandardCharsets.UTF_8));
        lros.close();

        String output = baos.toString(StandardCharsets.UTF_8.name());
        assertEquals(emojiString, output);
    }
}
