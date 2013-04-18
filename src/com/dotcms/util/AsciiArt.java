package com.dotcms.util;

import com.dotmarketing.util.Logger;


public class AsciiArt {
    
	private static boolean artDone = false;
	
	public static void doArt(){
		
		if(artDone) return;
		

		
		artDone=true;
		
		Logger.info(com.dotcms.util.AsciiArt.class, "                                                                                   ");
		Logger.info(com.dotcms.util.AsciiArt.class, "                                                                                   ");
		Logger.info(com.dotcms.util.AsciiArt.class, "                                                                                   ");
		Logger.info(com.dotcms.util.AsciiArt.class, "           OOOO                            7777777   7777       7777     77777777  ");
		Logger.info(com.dotcms.util.AsciiArt.class, "           OOOO                 OO       777777777  77777      77777    77777777   ");
		Logger.info(com.dotcms.util.AsciiArt.class, "           OOOO                OOO      77777       777777     77777   7777        ");
		Logger.info(com.dotcms.util.AsciiArt.class, "     OOOOOOOOOO   OOOOOOOO   OOOOOOOOO 7777         7777777   777777   7777        ");
		Logger.info(com.dotcms.util.AsciiArt.class, "    OOOO  OOOOO  OOOO  OOOO    OOOO    7777         7777777  7777777    777777     ");
		Logger.info(com.dotcms.util.AsciiArt.class, "   OOOO    OOOO  OOO    OOOO   OOOO    7777         77777777 777 7777     777777   ");
		Logger.info(com.dotcms.util.AsciiArt.class, "   OOOO    OOOO OOOO    OOOO   OOOO    7777         777  777 777 7777        7777  ");
		Logger.info(com.dotcms.util.AsciiArt.class, "   OOOO    OOOO  OOO    OOOO   OOOO    77777        777  777777  7777         7777 ");
		Logger.info(com.dotcms.util.AsciiArt.class, "    OOOO   OOOO  OOOO   OOO    OOOO     77777       777   7777   7777        7777  ");
		Logger.info(com.dotcms.util.AsciiArt.class, "     OOOOOOOOOO   OOOOOOOO      OOOOO    777777777  777   7777   7777  777777777   ");
		Logger.info(com.dotcms.util.AsciiArt.class, "                                                                                   ");
		Logger.info(com.dotcms.util.AsciiArt.class, "                                                         Content Management System ");
		// Logger.info(com.dotcms.util.AsciiArt.class, "                                                        copyright " + Calendar.getInstance().get(Calendar.YEAR) + ", dotCMS LLC");
		Logger.info(com.dotcms.util.AsciiArt.class, "                                                                                   ");
		Logger.info(com.dotcms.util.AsciiArt.class, "                                                                                   ");
	
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			//Logger.error(AsciiArt.class,e.getMessage(),e);
		}
	
	}
	
	
	
}