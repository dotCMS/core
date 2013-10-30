package com.dotcms.packager;

import com.tonicsystems.jarjar.Rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jonathan Gamba
 *         Date: 10/24/13
 */
public class Wildcard {

    private Collection<Pattern> patterns;
    private Pattern simplePattern;
    private String replaceWith;

    public Wildcard ( Rule rule ) {
        init( rule.getPattern(), rule.getResult() );
    }

    public Wildcard ( String pattern, String result ) {
        init( pattern, result );
    }

    private void init ( String pattern, String result ) {

        if ( pattern.equals( "**" ) ) {
            throw new IllegalArgumentException( "'**' is not a valid pattern" );
        }
        if ( pattern.contains( "***" ) ) {
            throw new IllegalArgumentException( "The sequence '***' is invalid in a package pattern" );
        }

        patterns = new ArrayList<Pattern>();

        String regex = pattern;
        regex = regex.replaceAll( "\\*\\*", "" );
        simplePattern = Pattern.compile( regex );
        patterns.add( Pattern.compile( regex + "\\w+<" ) );//For xml files
        patterns.add( Pattern.compile( regex + "\\w+\"" ) );//For xml files
        patterns.add( Pattern.compile( regex + "\\w+\\n" ) );//For properties files

        //Clean up the result
        if ( result.lastIndexOf( "@" ) != -1 ) {
            result = result.substring( 0, result.lastIndexOf( "@" ) );
        }
        this.replaceWith = result;
    }

    public String replace ( String value ) {

        for ( Pattern pattern : patterns ) {

            Matcher matcher = pattern.matcher( value );
            while ( matcher.find() ) {

                String group = matcher.group();
                String replacement = group.replaceAll( simplePattern.pattern(), replaceWith );
                value = value.replaceAll( group, replacement );
            }
        }

        return value;
    }

}