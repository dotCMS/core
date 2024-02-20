package com.dotcms.rendering.velocity;

import org.apache.velocity.context.Context;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.*;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.CacheLocator;
import com.dotcms.rendering.velocity.util.VelocityUtil;

/**
 * VelocityUtilTest
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VelocityMacroCacheTest {
	private final String macroName = "velocityTest";
	
	private final String macroTemplate = "#macro(velocityTest $var)$var#end";
	private final String macroArg = "HERE IS MY MACRO";
	private final String callMacro = "#velocityTest('" + macroArg+ "')";
	private Context  ctx = VelocityUtil.getBasicContext();
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Test the velocity path is the default path
     */
    @Test
    public void test010MacroCaches() throws Exception {

    	String[] macro = CacheLocator.getVeloctyResourceCache().getMacro(macroName);
    	assertTrue("there is no macro in cache", macro==null);
    	
    	VelocityUtil.eval(macroTemplate, ctx);
    	
    	macro = CacheLocator.getVeloctyResourceCache().getMacro(macroName);

    	assertTrue("there is a macro named velocityTest in cache", macro[0].equals(macroName));
    	assertTrue("there is macro velocityTest is correct cache", macro[1].equals(macroTemplate));

    }

    /**
     * Test the velocity path is the default path
     */
    @Test
    public void test020MacroSurvivesVelocityDump() throws Exception {
    	
    	VelocityUtil.getEngine().getRuntimeServices().dumpGlobalVMNamespace();
    	
    	// 
    	String x = VelocityUtil.eval(callMacro, ctx);
    	assertTrue("macroworks", x.trim().equals(macroArg));
    	
    }
    
    /**
     * Test the velocity path is the default path
     */
    @Test
    public void test030MacroDoesNotSurviveTotalDump() throws Exception {
    	

    	VelocityUtil.getEngine().getRuntimeServices().dumpGlobalVMNamespace();
    	CacheLocator.getCacheAdministrator().flushAll();
    	
    	// 
    	String x = VelocityUtil.eval(callMacro, ctx);
    	assertTrue("Macro does not exist", x.trim().equals(callMacro));
    	
    }
}
