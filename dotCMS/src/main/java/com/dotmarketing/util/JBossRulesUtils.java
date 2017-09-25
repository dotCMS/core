package com.dotmarketing.util;

import com.dotcms.repackage.org.drools.RuleBase;
import com.dotcms.repackage.org.drools.RuleBaseFactory;
import com.dotcms.repackage.org.drools.WorkingMemory;
import com.dotcms.repackage.org.drools.compiler.PackageBuilder;
import com.dotcms.repackage.org.drools.rule.Package;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JBossRulesUtils {
	private static RuleBase XMLRuleBase;
	private static long XMLRuleBaseLastModified;
	
	static {
		loadJBossRulesFiles();
	}
	
	private static void loadJBossRulesFiles() {
		try {
			String fileName = Config.getStringProperty("CONFIGURED_JBOSS_RULES_FILE_NAME");
			if (UtilMethods.isSet(fileName)) {
				String XMLFileName = com.liferay.util.FileUtil.getRealPath(fileName);
				XMLRuleBase = readRulesFromXML(XMLFileName);
				
				XMLRuleBaseLastModified = new File(XMLFileName).lastModified();
			}
		} catch (Exception e) {
			Logger.info(JBossRulesUtils.class, e.getMessage());
		}

	}
	
	private static void checkReloadJBossRulesFiles() {
		try {
			String fileName = Config.getStringProperty("CONFIGURED_JBOSS_RULES_FILE_NAME");
			if (UtilMethods.isSet(fileName)) {
				String XMLFileName = com.liferay.util.FileUtil.getRealPath(fileName);
				
				if (XMLRuleBaseLastModified  < (new File(XMLFileName).lastModified()))
					loadJBossRulesFiles();
			}
		} catch (Exception e) {
			Logger.info(JBossRulesUtils.class, e.getMessage());
		}
	}
	
	private static RuleBase readRulesFromXML(String fileName) throws Exception {
    	final Reader source = new InputStreamReader(Files.newInputStream(Paths.get(fileName)));
    	
        final PackageBuilder builder = new PackageBuilder();
        
        builder.addPackageFromXml(source);
        
        final Package pkg = builder.getPackage();
        
        final RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        ruleBase.addPackage(pkg);
        return ruleBase;
    }

	public static void checkObjectRulesFromXML(Object obj) {
        try {
        	//final RuleBase ruleBase = readRulesFromXML(fileName);
            //final WorkingMemory workingMemory = ruleBase.newWorkingMemory();
        	checkReloadJBossRulesFiles();
        	if (XMLRuleBase != null) {
	        	final WorkingMemory workingMemory = XMLRuleBase.newWorkingMemory();
	            
	            workingMemory.assertObject(obj);
	            workingMemory.fireAllRules();
        	}
        } catch (final Throwable t) {
        	Logger.info(JBossRulesUtils.class, t.getMessage());
        }
    }

}