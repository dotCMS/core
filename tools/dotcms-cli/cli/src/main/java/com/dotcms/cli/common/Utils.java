package com.dotcms.cli.common;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {

    private Utils(){
        //Hide public constructor
    }

    /**
     * This method produces a file name adding an incremental postfix when another file named identically already exists
     * e.g.  if our input file is something like:
     *   my-file.yml
     * Then the produced file name would be
     *  my-file(1).yml
     *  And so on
     * @param in
     * @return
     */
    public static Path nextFileName(final Path in){

        if(!Files.exists(in)){
            return in;
        }

        String fileName = in.toString();
        String extension = "";
        String name = "";

        int idxOfDot = fileName.lastIndexOf('.');   //Get the last index of . to separate extension
        extension = fileName.substring(idxOfDot + 1);
        name = fileName.substring(0, idxOfDot);

        Path path = Paths.get(fileName);
        int counter = 1;
        while(Files.exists(path)){
            fileName = String.format("%s(%s).%s",name, counter, extension);
            path = Paths.get(fileName);
            counter++;
        }
        return Path.of(fileName);
    }

}
