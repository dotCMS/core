package com.tonicsystems.jarjar;

import com.tonicsystems.jarjar.ext_util.EntryStruct;
import com.tonicsystems.jarjar.resource.ContentRewriter;
import com.tonicsystems.jarjar.resource.MatchableRule;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 11/15/13
 */
public class CustomContentRewriter implements ContentRewriter {

    private final List<MatchableRule> rules = new LinkedList<MatchableRule>();

    public CustomContentRewriter ( Collection<? extends Rule> ruleList ) {
        for ( Rule rule : ruleList ) {
            rules.add( new MatchableRule( rule ) );
        }
    }

    public boolean accepts ( EntryStruct struct ) {

        return !struct.name.endsWith( ".class" )
                && !struct.name.endsWith( ".java" )
                && !struct.name.endsWith( "MANIFEST.MF" );
    }

    public String replace ( String content ) {
        String replacement = content;
        for ( MatchableRule rule : rules ) {
            replacement = rule.replace( replacement );
        }
        return replacement;
    }

    @Override
    public List<MatchableRule> getRules () {
        return rules;
    }

}