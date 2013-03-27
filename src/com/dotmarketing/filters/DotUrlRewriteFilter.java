package com.dotmarketing.filters;

import com.dotmarketing.util.Config;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.Rule;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by Jonathan Gamba
 * Date: 3/26/13
 */
public class DotUrlRewriteFilter extends UrlRewriteFilter {

    private static DotUrlRewriteFilter urlRewriteFilter;

    @Override
    public void init ( FilterConfig filterConfig ) throws ServletException {
        super.init( filterConfig );
        urlRewriteFilter = this;
    }

    /**
     * Static reference to this instance for easy access
     *
     * @return
     */
    public static DotUrlRewriteFilter getUrlRewriteFilter () {
        return urlRewriteFilter;
    }

    /**
     * Complete list of Rules on this rewrite filter
     *
     * @return
     * @throws Exception
     */
    public List<Rule> getRules () throws Exception {

        Conf conf = getConf();
        return conf.getRules();
    }

    /**
     * Add a given Rule to this rewrite filter
     *
     * @param rule
     * @throws Exception
     */
    public void addRule ( Rule rule ) throws Exception {

        //Initialise the rule, if success add it to the filter
        Boolean initialized = rule.initialise( Config.CONTEXT );
        if ( initialized ) {

            //Adding it the rule to the current filter configuration
            Conf conf = getConf();
            conf.addRule( rule );

            //Apply the rules changes
            reload();
        } else {
            throw new RuntimeException( "Error initializing Rewrite Rule!" );
        }
    }

    /**
     * Remove a given Rule for this Rewrite filter
     *
     * @param rule
     * @throws Exception
     */
    public void removeRule ( Rule rule ) throws Exception {

        //Get the current list of rules
        List<Rule> rules = getRules();
        //Remove the given Rule
        rules.remove( rule );
    }

    /**
     * Reloads the current configuration, if changes on the Rules are made calling this method will apply them
     *
     * @throws Exception
     */
    public void reload () throws Exception {

        //Checks and apply the current filter configuration
        Conf conf = getConf();
        checkConf( conf );
    }

    /**
     * Returns the last configuration loaded for this url rewrite filter, the access of this
     * Conf object is private on the parent of this class, but in order to allow programmatically
     * addition of Rules we are accessing the Conf field using reflection.
     *
     * @return
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    private Conf getConf () throws IllegalAccessException, NoSuchFieldException {

        Field confLastLoadedField = UrlRewriteFilter.class.getDeclaredField( "confLastLoaded" );
        confLastLoadedField.setAccessible( true );
        Conf confLastLoaded = (Conf) confLastLoadedField.get( urlRewriteFilter );
        confLastLoadedField.setAccessible( false );

        return confLastLoaded;
    }

}