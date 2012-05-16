package com.dotcms.autoupdater;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class DownloadProgress {

    NumberFormat nf;
    int maxMessageLength = 0;
    long kcount = 0;
    long length;

    public DownloadProgress ( long length ) {

        this.length = length;
        nf = new DecimalFormat( "#,#00.00" );
    }

    public String getProgressMessage ( int count, long startTime, long currentTime ) {

        long diff = count - kcount;
        long speed = 0;
        if ( ( currentTime - startTime ) != 0 ) {
            speed = ( diff / ( currentTime - startTime ) ) * 1000; // in b/s
        }
        speed /= 1024; // in Kb/s
        startTime = currentTime;

        kcount = count;
        String message = kcount / 1024 + " kB " + Messages.getString( "DownloadProgress.text.downloaded" );
        if ( length > 0 ) {
            //Calculate percent done.
            float percent = ( ( new Float( kcount ) * 100f ) / new Float( length ) );
            message += ", " + nf.format( percent ) + "% " + Messages.getString( "DownloadProgress.text.done" );
        }

        message += " (" + speed + " kB/s";
        if ( length > 0 ) {
            String timeString = "--";
            if ( speed != 0 ) {
                long left = ( ( length - kcount ) / 1024 ) / speed;
                timeString = left + "";

            }
            message += ", " + timeString + " " + Messages.getString( "DownloadProgress.text.seconds.left" );
        }
        message += ")";

        if ( message.length() < maxMessageLength ) {
            //Pad the message
            for ( int j = 0; j < maxMessageLength - message.length(); j++ ) {
                message += " ";
            }
        } else {
            maxMessageLength = message.length();
        }
        return message;
    }

}