package com.dotcms.visitor.filter.characteristics;

import com.dotcms.visitor.domain.Visitor;
import java.io.Serializable;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface Character {

    default Map<String, Serializable>  getMap(HttpServletRequest request){
        return getMap(request, null, null);
    }


    default Map<String, Serializable>  getMap(HttpServletRequest request, HttpServletResponse response){
        return getMap(request, response, null);
    }


    Map<String, Serializable>  getMap(HttpServletRequest request, HttpServletResponse response, Visitor visitor);
    
    void  clearMap();
    
    
    
    
    
}
