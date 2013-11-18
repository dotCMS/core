package com.tonicsystems.jarjar;

import com.tonicsystems.jarjar.ext_util.EntryStruct;
import com.tonicsystems.jarjar.resource.LineRewriter;
import com.tonicsystems.jarjar.resource.MatchableRule;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 11/15/13
 */
public class CustomLineRewriter implements LineRewriter {

    private final List<MatchableRule> rules = new LinkedList<MatchableRule>();

    public CustomLineRewriter ( Collection<Rule> ruleList ) {
        for ( Rule rule : ruleList ) {
            rules.add( new MatchableRule( rule ) );
        }
    }

    public boolean accepts ( EntryStruct struct ) {

        return !struct.name.endsWith( ".class" )
                && !struct.name.endsWith( ".java" )
                && !struct.name.endsWith( "MANIFEST.MF" );
    }

    public String replaceLine ( String line ) {
        for ( MatchableRule rule : rules ) {
            String replacement = rule.replace( line );
            if ( !replacement.equals( line ) ) {
                return replacement;
            }
        }
        return line;
    }

    @Override
    public List<MatchableRule> getRules () {
        return rules;
    }

}