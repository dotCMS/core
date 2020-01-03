package com.dotmarketing.image.filter;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.image.focalpoint.FocalPoint;
import com.dotmarketing.image.focalpoint.FocalPointAPIImpl;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Tuple2;



public class FocalPointImageFilter extends ImageFilter {


    final FocalPointAPIImpl util = new FocalPointAPIImpl();
    public String[] getAcceptedParameters() {
        return new String[] {"fp (float,float) specifies the focal point for a file"};
    }



    public File runFilter(File file, Map<String, String[]> parameters) {


        Optional<FocalPoint> focalPoint = util.parseFocalPointFromParams(parameters);
        
        
        if (!overwrite(file, parameters) || !focalPoint.isPresent() ) {
            return file;
        }
        


        String inode = parameters.get("assetInodeOrIdentifier")[0];
        String fieldVar = parameters.get("fieldVarName")[0];


        util.writeFocalPoint(inode, fieldVar, focalPoint.get());



        return file;

    }



    @Override
    protected File getResultsFile(File file, Map<String, String[]> parameters) throws DotRuntimeException {
        return getResultsFile(file, parameters, UtilMethods.getFileExtension(file.getName()));
    }

}
