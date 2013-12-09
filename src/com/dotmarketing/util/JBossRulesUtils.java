package com.dotmarketing.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.WorkingMemory;
import org.drools.compiler.PackageBuilder;
import org.drools.rule.Package;

public class JBossRulesUtils {
	private static RuleBase XMLRuleBase;
	private static long XMLRuleBaseLastModified;
	//private static RuleBase DSLRuleBase;
	//private static long DSLRuleBaseLastModified;
	//private static RuleBase DRLRuleBase;
	//private static long DRLRuleBaseLastModified;
	
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
		/*
		try {
			String fileName1 = Config.getStringProperty("CONFIGURED_JBOSS_RULES_FILE_NAME");
			String fileName2 = Config.getStringProperty("CONFIGURED_JBOSS_RULES_FILE_NAME");
			if (UtilMethods.isSet(fileName1) && UtilMethods.isSet(fileName2)) {
				String DRLFileName = com.dotmarketing.util.Config.CONTEXT.getRealPath(fileName1);
				String DSLFileName = com.dotmarketing.util.Config.CONTEXT.getRealPath(fileName2);
				DSLRuleBase = readRulesFromDSL(DRLFileName, DSLFileName);
				
				DRLFileNameLastModified = new File(DRLFileName).lastModified();
				DSLFileNameLastModified = new File(DSLFileName).lastModified();
			}
		} catch (Exception e) {
			Logger.info(JBossRulesUtils.class, e.getMessage());
		}
		
		try {
			String fileName = Config.getStringProperty("CONFIGURED_JBOSS_RULES_FILE_NAME");
			if (UtilMethods.isSet(fileName)) {
				String DRLFileName = com.dotmarketing.util.Config.CONTEXT.getRealPath(fileName);
				DRLRuleBase = readRulesFromDRL(DRLFileName);
				
				DRLFileNameLastModified = new File(DRLFileName).lastModified();
			}
		} catch (Exception e) {
			Logger.info(JBossRulesUtils.class, e.getMessage());
		}
		*/
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
		/*
		try {
			String fileName1 = Config.getStringProperty("CONFIGURED_JBOSS_RULES_FILE_NAME");
			String fileName2 = Config.getStringProperty("CONFIGURED_JBOSS_RULES_FILE_NAME");
			if (UtilMethods.isSet(fileName1) && UtilMethods.isSet(fileName2)) {
				String DRLFileName = com.dotmarketing.util.Config.CONTEXT.getRealPath(fileName1);
				String DSLFileName = com.dotmarketing.util.Config.CONTEXT.getRealPath(fileName2);
				
				if ((DRLFileNameLastModified  < (new File(DRLFileName).lastModified())) ||
					(DSLFileNameLastModified  < (new File(DSLFileName).lastModified())))
					loadJBossRulesFiles();
			}
		} catch (Exception e) {
			Logger.info(JBossRulesUtils.class, e.getMessage());
		}
		
		try {
			String fileName = Config.getStringProperty("CONFIGURED_JBOSS_RULES_FILE_NAME");
			if (UtilMethods.isSet(fileName)) {
				String DRLFileName = com.dotmarketing.util.Config.CONTEXT.getRealPath(fileName);
				
				if (DRLFileNameLastModified  < (new File(DRLFileName).lastModified()))
					loadJBossRulesFiles();
			}
		} catch (Exception e) {
			Logger.info(JBossRulesUtils.class, e.getMessage());
		}
		*/
	}
	
	private static RuleBase readRulesFromXML(String fileName) throws Exception {
    	final Reader source = new InputStreamReader(new FileInputStream(fileName));
    	
        final PackageBuilder builder = new PackageBuilder();
        
        builder.addPackageFromXml(source);
        
        final Package pkg = builder.getPackage();
        
        final RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        ruleBase.addPackage(pkg);
        return ruleBase;
    }
	/*
	private static RuleBase readRulesFromDSL(String DRLFileName, String DSLFileName) throws Exception {
    	final Reader source = new InputStreamReader(new FileInputStream(DRLFileName));
    	
        Reader dsl = new InputStreamReader(new FileInputStream(DSLFileName));
        
        final PackageBuilder builder = new PackageBuilder();
        
        builder.addPackageFromDrl(source, dsl);
        
        final Package pkg = builder.getPackage();
        
        final RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        ruleBase.addPackage(pkg);
        return ruleBase;
    }
	
	private static RuleBase readRulesFromDRL(String fileName) throws Exception {
    	final Reader source = new InputStreamReader(new FileInputStream(fileName));
    	
        final PackageBuilder builder = new PackageBuilder();
        
        builder.addPackageFromDrl(source);
        
        final Package pkg = builder.getPackage();
        
        final RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        ruleBase.addPackage(pkg);
        return ruleBase;
    }
	*/
	//public static void checkObjectRulesFromXML(Object obj, String fileName) {
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
	/*
	//public static void checkObjectRulesFromDSL(Object obj, String DRLFileName, String DSLFileName) {
	public static void checkObjectRulesFromDSL(Object obj) {
        try {
        	//final RuleBase ruleBase = readRulesFromDSL(DRLFileName, DSLFileName);
            //final WorkingMemory workingMemory = ruleBase.newWorkingMemory();
            checkReloadJBossRulesFiles();
            if (DSLRuleBase != null) {
        		final WorkingMemory workingMemory = DSLRuleBase.newWorkingMemory();
            	
            	workingMemory.assertObject(obj);
            	workingMemory.fireAllRules();
            }
        } catch (final Throwable t) {
        	Logger.info(JBossRulesUtils.class, t.getMessage());
        }
    }
	
	//public static void checkObjectRulesFromDRL(Object obj, String fileName) {
	public static void checkObjectRulesFromDRL(Object obj) {
        try {
        	//final RuleBase ruleBase = readRulesFromDRL(fileName);
            //final WorkingMemory workingMemory = ruleBase.newWorkingMemory();
            checkReloadJBossRulesFiles();
            if (DRLRuleBase != null) {
        		final WorkingMemory workingMemory = DRLRuleBase.newWorkingMemory();
            	
            	workingMemory.assertObject(obj);
            	workingMemory.fireAllRules();
            }
        } catch (final Throwable t) {
        	Logger.info(JBossRulesUtils.class, t.getMessage());
        }
    }
    */
}