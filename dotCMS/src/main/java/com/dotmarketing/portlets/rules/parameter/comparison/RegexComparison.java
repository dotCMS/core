package com.dotmarketing.portlets.rules.parameter.comparison;

import java.util.regex.Pattern;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * @author Geoff M. Granum
 */
public class RegexComparison extends Comparison<String> {

    
    public static final Cache<String, Pattern> patternsCache = Caffeine.newBuilder().maximumSize(1000).build();
    
    
    public RegexComparison() {
        super("regex");
    }

    @Override
    public boolean perform(String argA, String regex) {
        Pattern pattern= patternsCache.get(regex, k-> Pattern.compile(k));
        
        
        
        return pattern.matcher(argA).matches();
    }
}
 
