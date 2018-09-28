package com.dotcms.rendering.velocity.servlet;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

class VelocityFilterWriter extends FilterWriter {

    private boolean firstNonWhiteSpace = false;

    public VelocityFilterWriter(Writer arg0) {
        super(arg0);

    }

    @Override
    public void write(char[] arg0) throws IOException {
        if (firstNonWhiteSpace) {
            super.write(arg0);
        } else {

            for (int i = 0; i < arg0.length; i++) {
                if (arg0[i] > 32) {
                    firstNonWhiteSpace = true;
                }
                if (firstNonWhiteSpace) {
                    super.write(arg0[i]);
                }

            }

        }

    }

    @Override
    public void write(String arg0) throws IOException {
        if (firstNonWhiteSpace) {
            super.write(arg0);
        } else {
            char[] stringChar = arg0.toCharArray();
            for (int i = 0; i < stringChar.length; i++) {

                if (stringChar[i] > 32) {
                    firstNonWhiteSpace = true;
                    super.write(arg0.substring(i, stringChar.length));
                    break;
                }

            }

        }

    }

}