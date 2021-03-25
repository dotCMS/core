package com.dotmarketing.image.focalpoint;

import java.util.Map;
import java.util.Optional;

public interface FocalPointAPI {

    /**
     * This writes a focal point for an image field for a contentlet/field variable current
     * 
     * @param inode
     * @param fieldVar
     * @param focalPoint
     */
    void writeFocalPoint(String inode, String fieldVar, FocalPoint focalPoint);

    /**
     * this take string value like ".345,.7567" tries to parse it into 2 floats which describe the focal
     * point
     * 
     * @param forcalPoint
     * @return
     */
    Optional<FocalPoint> parseFocalPoint(String forcalPoint);


    /**
     * given a contentlet and a field variable, this tries to read the focal point value for that inode
     * 
     * @param inode
     * @param fieldVar
     * @return
     */
    Optional<FocalPoint> readFocalPoint(String inode, String fieldVar);


    /**
     * given a list of parameters, this will return an optional focal point from those parameters
     * 
     * @param parameters
     * @return
     */
    Optional<FocalPoint> parseFocalPointFromParams(Map<String, String[]> parameters);

}
