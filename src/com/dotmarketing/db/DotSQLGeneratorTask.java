package com.dotmarketing.db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.regex.Matcher;

import net.sf.hibernate.cfg.Configuration;
import net.sf.hibernate.tool.hbm2ddl.SchemaExport;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.dotmarketing.util.Logger;

public final class DotSQLGeneratorTask extends Task {
	/*
	 * This class is meant to be run from ant in the project root. It will
	 * create the db based on the Inode.hbm.xml
	 * 
	 */
	private String dialect;

	public void setDialect(String dialect) {
		this.dialect = dialect;
	}

	public void execute() throws BuildException {
        Logger.info(this, "dialect:" + dialect);       
        
		if (dialect == null) {
			Logger.info(this, "no dialect:" + dialect);
		}
		
		try{
			Class.forName("oracle.jdbc.driver.OracleDriver");
			Class.forName("com.mysql.jdbc.Driver");
			Class.forName("org.postgresql.Driver");
			Class.forName("net.sourceforge.jtds.jdbc.Driver");	
		}
		catch(Exception e){
			 Logger.info(this, "Driver not found dialect:" + e);
			 throw new BuildException("database driver not found");
		}
		
		try {
			Configuration cfg = new Configuration();
			cfg.setProperty("hibernate.dialect", dialect);
            
			if (dialect.equals("net.sf.hibernate.dialect.MySQLDialect")){
                cfg.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
				cfg.addResource("com/dotmarketing/beans/DotCMSId.hbm.xml");
			}else if (dialect.equals("net.sf.hibernate.dialect.SybaseDialect")){
				cfg.addResource("com/dotmarketing/beans/DotCMSId.hbm.xml");
                cfg.setProperty("hibernate.connection.driver_class", "net.sourceforge.jtds.jdbc.Driver");
			}else if(dialect.equals("net.sf.hibernate.dialect.OracleDialect")){
				cfg.addResource("com/dotmarketing/beans/DotCMSSeq.hbm.xml");
                cfg.setProperty("hibernate.connection.driver_class", "oracle.jdbc.driver.OracleDriver");
			}else{
				cfg.addResource("com/dotmarketing/beans/DotCMSSeq.hbm.xml");
                cfg.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
			}

            SchemaExport sexp = new SchemaExport(cfg);
			sexp.setDelimiter(";");
			//DOTCMS-2915
			String basedir = this.getProject().getProperties().get("basedir").toString();
            File in = new File(basedir, "sql/sql.tmp"); 
			String[] x = dialect.split("[.]");
			String sqlFileName = ("sql/cms/dotcms_" + x[x.length - 1].replaceAll("Dialect", "") + ".sql").toLowerCase();
			sqlFileName = sqlFileName.replaceAll("sybase", "mssql-sybase");
            Logger.info(this, "writing file:" + sqlFileName);
            //DOTCMS-2915
            //File out = new File(sqlFileName);
            File out = new File(basedir, sqlFileName);
            
			boolean afterDrops = false;
			sexp.setOutputFile(in.getAbsolutePath());
			sexp.create(false, false);
			BufferedReader r = new BufferedReader(new FileReader(in));
			BufferedWriter wr = new BufferedWriter(new FileWriter(out));
			while (r.ready()) {
				String myLine = r.readLine().toLowerCase();
				
				if (myLine.startsWith("create table")) {
					afterDrops = true;
					java.util.regex.Pattern p = java.util.regex.Pattern.compile("[a-zA-Z][A-Z,a-z,0-9]*\\s[A-Z,a-z,0-9]*\\s[a-zA-Z][A-Z,a-z,0-9]*\\s\\([a-zA-Z][A-Z,a-z,0-9]*.*");
					Matcher m = p.matcher(myLine);
					if(m.matches()){
						//System.out.println("matches " + myLine);
						String [] fields = myLine.split(", ");
						StringBuffer s = new StringBuffer("");
						for(int i = 0; i < fields.length; i++){
							if(i != fields.length - 1){
								s.append("\t" + fields[i] + "," + "\n");
							}else{
								s.append("\t" + fields[i]);
							}
							
						}
						myLine = s.toString();
					}
				}
                if (dialect.equals("net.sf.hibernate.dialect.PostgreSQLDialect")) {
                    if (myLine.contains("varchar(123456789)")) {
                        myLine = myLine.replaceAll("varchar\\(123456789\\)", "text");
                    }
                } else if (dialect.equals("net.sf.hibernate.dialect.OracleDialect")) {
                    if (myLine.contains("varchar2(123456789)")) {
                        myLine = myLine.replaceAll("varchar2\\(123456789\\)", "clob");
                    }
                    else if (myLine.contains(" long ") || myLine.contains(" long,")) {
                        myLine = myLine.replaceAll("long", "nclob");
                    }
                } else if (dialect.equals("net.sf.hibernate.dialect.MySQLDialect")) {
                    if (myLine.contains("varchar(123456789)")) {
                        myLine = myLine.replaceAll("varchar\\(123456789\\)", "longtext");
                    }
                    if(myLine.contains("bit,")) {
                        myLine = myLine.replaceAll("bit,", "varchar(1),");
                    }
                } else if (dialect.equals("net.sf.hibernate.dialect.SybaseDialect")) {
                    if (myLine.contains("varchar(123456789)")) {
                        myLine = myLine.replaceAll("varchar\\(123456789\\)", "text");
                    }
                }
				if (afterDrops) {
					wr.write(myLine);
					wr.write("\n");					
				}
			}

			r.close();
			wr.close();
			in.delete();
		} catch (Exception e) {
			throw new BuildException(e);

		}
	}

}
