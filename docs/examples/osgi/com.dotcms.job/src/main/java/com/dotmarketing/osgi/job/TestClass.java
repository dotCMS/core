package com.dotmarketing.osgi.job;

import com.dotmarketing.util.Logger;

/**
 * Created by Jonathan Gamba
 * Date: 1/29/13
 */
public class TestClass {

    public void printA () {
        Logger.info( this, "Printing from TestClass.printA" );
    }

    public void printB () {
        Logger.info( this, "Printing from TestClass.printB" );
    }

}