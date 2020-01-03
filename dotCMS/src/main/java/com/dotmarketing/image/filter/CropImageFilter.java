package com.dotmarketing.image.filter;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import com.dotmarketing.image.focalpoint.FocalPoint;
import com.dotmarketing.image.focalpoint.FocalPointAPIImpl;
import com.dotmarketing.util.Logger;

public class CropImageFilter extends ImageFilter {

	public String[] getAcceptedParameters(){
		return  new String[] {
				"x (int) for left of crop",
				"y (int) for top of crop",
				"w (int) for width of crop",
				"h (int) for height of crop",
				"fp (int,int) the focal point of the crop"
		};
	}
	
	
	
	public File runFilter(File file,  Map<String, String[]> parameters) {
		int x = parameters.get(getPrefix() + "x") != null ? Integer.parseInt(parameters.get(getPrefix() + "x")[0]) : 0;
		int y = parameters.get(getPrefix() + "y") != null ? Integer.parseInt(parameters.get(getPrefix() + "y")[0]) : 0;
		float wIn = parameters.get(getPrefix() + "w") != null ? Float.parseFloat(parameters.get(getPrefix() + "w")[0]) : 0f;
		float hIn = parameters.get(getPrefix() + "h") != null ? Float.parseFloat(parameters.get(getPrefix() + "h")[0]) : 0f;
		int w=0,h=0;


        

		
		
		
		
		File resultFile = getResultsFile(file, parameters);
		
		if (!overwrite(resultFile, parameters)) {
			return resultFile;
		}
		
		BufferedImage src;
		try {
			src = ImageIO.read(file);
			
			Dimension current = new Dimension(src.getWidth(), src.getHeight());

			
	        if(wIn ==0 && hIn >0){
	            h = Math.round(hIn <=1  ? current.height * hIn : hIn);
	            w = Math.round(h * current.width / current.height);
	        }
	        else if(wIn >0 && hIn ==0){
	            w = Math.round(wIn <= 1 ? current.width * wIn : wIn);
	            h = Math.round(w * current.height / current.width);
	        }
            else if(wIn >0 && hIn >0){
                w = Math.round(wIn <= 1 ? current.width * wIn : wIn);
                h = Math.round(hIn <= 1 ? current.height * hIn : hIn);
            }
	        else{
	            w = current.width;
	            h = current.height;
	        }
	        
			
	        if(x > current.getWidth() ){
	           x=current.width;
	        }
	        if(y>current.getHeight()) {
	            y=current.height;
	        }
	        
		    Optional<Point> centerOpt = (x==0 && y==0) ? calcFocalPoint(src, parameters)  : Optional.empty();

            if (centerOpt.isPresent()) {

                final int halfWidth = Math.floorDiv(w, 2);
                final int halfHeight = Math.floorDiv(h, 2);
                final Point p = centerOpt.get();
                
                x = Math.max(p.x - halfWidth, 0);
                y = Math.max(p.y - halfHeight, 0);

                
                
                w = x + halfWidth > current.width ? current.width - x : w;
                h = y + halfHeight > current.height ? current.height - y : h;
            }
			
			if(x + w > current.width){
				w = src.getWidth()-x -1;
			}
			if(y + h > current.height){
				h = src.getHeight()-y-1;
			}
			
			
			BufferedImage out = src.getSubimage(x, y, w, h);
			ImageIO.write(out, FILE_EXT, resultFile);
			
		} catch (IOException e) {
			Logger.error(this.getClass(), e.getMessage());
		}

		
		return resultFile;
	}


    protected Optional<Point> calcFocalPoint(BufferedImage src, Map<String, String[]> parameters) {
        Dimension current = new Dimension(src.getWidth(), src.getHeight());
    
        
        String[] param = parameters.get("fp");
        if (param == null || param.length == 0) {
            String inode = parameters.get("assetInodeOrIdentifier")[0];
            String fieldVar = parameters.get("fieldVarName")[0];

            Optional<FocalPoint> optPoint =new FocalPointAPIImpl().readFocalPoint(inode, fieldVar);
            if(optPoint.isPresent()) {
                return Optional.of(new Point(Math.round(current.width * optPoint.get().x), Math.round(current.height * optPoint.get().y)));
            }
            return Optional.empty();
            //return Optional.of(new Point(current.width/2, current.height/2));
        }
        
        
        String[] p = param[0].split(":|,");
        if (p.length > 1 && p[0].contains(".") || p[1].contains(".")) {
            try {
                return Optional.of(new Point(Math.round(current.width * Float.parseFloat(p[0])), Math.round(current.height * Float.parseFloat(p[1]))));
            } catch (Exception e) {
                Logger.warnAndDebug(this.getClass(), e);
            }
            
        }
        else {
            try {
     
                return Optional.of(new Point(Integer.parseInt(p[0]), Integer.parseInt(p[1])));
            } catch (Exception e) {
                Logger.warnAndDebug(this.getClass(), e);
            }
        }
        return Optional.empty();

    }

}
