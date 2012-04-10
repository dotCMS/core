This plugin allows you to add form tab "CMS admin" a backend portlet to manage system logging.
Portlet needs a new table on db, within 3 fields: log_name, description and enabled flag.
Sql will be something like :

CREATE TABLE "U_DOTCMS20_CHRI"."LOG_MAPPER"
  (
    "ENABLED"     NUMBER(1,0) NOT NULL ENABLE,
    "LOG_NAME"    VARCHAR2(30 CHAR) NOT NULL ENABLE,
    "DESCRIPTION" VARCHAR2(50 CHAR) NOT NULL ENABLE,
    CONSTRAINT "LOG_MAPPER_PK" PRIMARY KEY ("LOG_NAME") USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT) TABLESPACE "TS_DOTCMS20_CHRI" ENABLE
  )
  
  At the moment whe need to manage only 3 log file , so our table is filled in this way: 
  
		-	1		dotcms-userActivity.log		Log Users action on pages, structures, documents.
		-	1		dotcms-security.log				Log users login activity into dotCMS.
		-	1		dotcms-adminaudit.log		Log Admin activity on dotCMS.
  
  Same issue involves changing on logging  management with new logger classes  and one new rolling  appender.
  Plugin's deploy will only import  configuration, src need to be incorporated with dotCMS source code. 