package com.dotmarketing.portlets.rules.parameter.type.constraint;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Geoff M. Granum
 */
public class TypeConstraint {

    @JsonIgnore
    public final Function<String, Boolean> fn;
    public final String id;
    public final Map<String, Object> args;

    public TypeConstraint(String id, Function<String, Boolean> fn) {
        this(id, fn, Collections.emptyMap());
    }

    public TypeConstraint(String id, Function<String, Boolean> fn, Object arg) {
        this(id, fn, ImmutableMap.of("value", arg));
    }

    public TypeConstraint(String id, Function<String, Boolean> fn, Map<String, Object> args) {
        this.fn = fn;
        this.id = id;
        this.args = ImmutableMap.copyOf(args);
    }


}
 
