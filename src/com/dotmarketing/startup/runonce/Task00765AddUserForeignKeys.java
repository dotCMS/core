package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.*;

public class Task00765AddUserForeignKeys extends AbstractJDBCStartupTask {

	@Override
	public String getMSSQLScript() {
		return
		"update contentlet set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update htmlpage set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update file_asset set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update containers set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update template set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update links set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"drop index file_asset.idx_file_1; \n"+
        "alter table file_asset alter column mod_user varchar(100); \n"+
        "create index idx_file_1 on file_asset (mod_user); \n"+
		"alter table contentlet add constraint fk_user_contentlet foreign key (mod_user) references user_(userid); \n"+
		"alter table htmlpage add constraint fk_user_htmlpage foreign key (mod_user) references user_(userid); \n"+
		"alter table containers add constraint fk_user_containers foreign key (mod_user) references user_(userid); \n"+
		"alter table template add constraint fk_user_template foreign key (mod_user) references user_(userid); \n"+
		"alter table file_asset add constraint fk_user_file_asset foreign key (mod_user) references user_(userid); \n"+
		"alter table links add constraint fk_user_links foreign key (mod_user) references user_(userid); ";
	}

	@Override
	public String getMySQLScript() {
		return
		"update contentlet set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update htmlpage set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update file_asset set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update containers set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update template set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update links set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"alter table file_asset modify mod_user varchar(100); \n"+
		"alter table contentlet add constraint fk_user_contentlet foreign key (mod_user) references user_(userid); \n"+
		"alter table htmlpage add constraint fk_user_htmlpage foreign key (mod_user) references user_(userid); \n"+
		"alter table containers add constraint fk_user_containers foreign key (mod_user) references user_(userid); \n"+
		"alter table template add constraint fk_user_template foreign key (mod_user) references user_(userid); \n"+
		"alter table file_asset add constraint fk_user_file_asset foreign key (mod_user) references user_(userid); \n"+
		"alter table links add constraint fk_user_links foreign key (mod_user) references user_(userid); ";
	}

	@Override
	public String getOracleScript() {
		return
		"update contentlet set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update htmlpage set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update file_asset set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update containers set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update template set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update links set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"alter table file_asset modify mod_user varchar2(100); \n"+
		"alter table contentlet add constraint fk_user_contentlet foreign key (mod_user) references user_(userid); \n"+
		"alter table htmlpage add constraint fk_user_htmlpage foreign key (mod_user) references user_(userid); \n"+
		"alter table containers add constraint fk_user_containers foreign key (mod_user) references user_(userid); \n"+
		"alter table template add constraint fk_user_template foreign key (mod_user) references user_(userid); \n"+
		"alter table file_asset add constraint fk_user_file_asset foreign key (mod_user) references user_(userid); \n"+
		"alter table links add constraint fk_user_links foreign key (mod_user) references user_(userid); ";
	}

	@Override
	public String getPostgresScript() {
		return
		"update contentlet set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update htmlpage set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update file_asset set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update containers set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update template set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"update links set mod_user = 'system' where not exists(select userid from user_ where userid = mod_user); \n "+
		"alter table file_asset alter column mod_user type varchar(100); \n"+
		"alter table contentlet add constraint fk_user_contentlet foreign key (mod_user) references user_(userid); \n"+
		"alter table htmlpage add constraint fk_user_htmlpage foreign key (mod_user) references user_(userid); \n"+
		"alter table containers add constraint fk_user_containers foreign key (mod_user) references user_(userid); \n"+
		"alter table template add constraint fk_user_template foreign key (mod_user) references user_(userid); \n"+
		"alter table file_asset add constraint fk_user_file_asset foreign key (mod_user) references user_(userid); \n"+
		"alter table links add constraint fk_user_links foreign key (mod_user) references user_(userid); ";
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

	public boolean forceRun() {
		return true;
	}

}
