package com.dotcms.rendering.velocity.services;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class VelocityPrettyWriter extends OutputStreamWriter {

    public VelocityPrettyWriter(OutputStream out) throws UnsupportedEncodingException {
        super(out, "UTF8");

    }


    public VelocityPrettyWriter(OutputStream out, Charset cs) throws UnsupportedEncodingException {
        super(out, "UTF8");

    }

    @Override
    public void write(char[] chars) throws IOException {
        chars = replacePounds(chars);
        super.write(new String(chars));
    }

    @Override
    public void write(String chars) throws IOException {

        chars = new String(replacePounds(chars.toCharArray()));
        super.write(chars);
    }

    @Override
    public void write(char[] chars, int start, int len) throws IOException {
        chars = replacePounds(chars);


        super.write(chars, start, chars.length);
    }

    @Override
    public void write(String chars, int start, int len) throws IOException {
        chars = new String(replacePounds(chars.toCharArray()));

        super.write(chars, start, chars.length());
    }


    private char[] replacePounds(char[] chars) {
        StringBuffer sw = new StringBuffer();


        for (char c : chars) {
            if (c == '#') {
                sw.append('\n');
            }
            sw.append(c);
        }

        return sw.toString()
            .toCharArray();

    }



}
