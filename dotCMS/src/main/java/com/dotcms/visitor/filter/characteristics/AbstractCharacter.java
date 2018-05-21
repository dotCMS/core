package com.dotcms.visitor.filter.characteristics;

import com.dotcms.visitor.domain.Visitor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractCharacter implements Character {

    protected final HttpServletRequest request;
    protected final HttpServletResponse response;
    protected final Visitor visitor;
    protected static ThreadLocal<Map<String, Serializable>> myMap = ThreadLocal.withInitial(LinkedHashMap::new);

    public AbstractCharacter(HttpServletRequest request, HttpServletResponse response, Visitor visitor) {
        this.request=request;
        this.response=response;

        this.visitor=visitor;

    }
    
    public AbstractCharacter(AbstractCharacter incomingCharacter) {
        this.request=incomingCharacter.request;
        this.response=incomingCharacter.response;;
        this.visitor=incomingCharacter.visitor;
    }
    public final void clearMap() {
        myMap.get().clear();
    }
    
    public final Map<String, Serializable> getMap() {
        return myMap.get();
    }
}
