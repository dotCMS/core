--postgres
CREATE INDEX idx_tree ON tree USING btree (child, parent, relation_type);
CREATE INDEX idx_tree_1 ON tree USING btree (parent);
CREATE INDEX idx_tree_2 ON tree USING btree (child);
CREATE INDEX idx_tree_3 ON tree USING btree (relation_type);
CREATE INDEX idx_tree_4 ON tree USING btree (parent, child, relation_type);
CREATE INDEX idx_tree_5 ON tree USING btree (parent, relation_type);
CREATE INDEX idx_tree_6 ON tree USING btree (child, relation_type);

CREATE INDEX idx_contentlet_3 ON contentlet USING btree (inode);

CREATE INDEX idx_permisision_4 ON permission USING btree (permission_type);

CREATE INDEX idx_permission_reference_2 ON permission_reference USING btree(reference_id);
CREATE INDEX idx_permission_reference_3 ON permission_reference USING btree(reference_id,permission_type);
CREATE INDEX idx_permission_reference_4 ON permission_reference USING btree(asset_id,permission_type);
CREATE INDEX idx_permission_reference_5 ON permission_reference USING btree(asset_id,reference_id,permission_type);
CREATE INDEX idx_permission_reference_6 ON permission_reference USING btree(permission_type);

CREATE UNIQUE INDEX idx_field_velocity_structure ON field (velocity_var_name,structure_inode);


alter table chain_state add constraint fk_state_chain foreign key (chain_id) references chain(id);
alter table chain_state add constraint fk_state_code foreign key (link_code_id) references chain_link_code(id);
alter table chain_state_parameter add constraint fk_parameter_state foreign key (chain_state_id) references chain_state(id);

alter table permission add constraint permission_role_fk foreign key (roleid) references cms_role(id);

alter table contentlet add constraint FK_structure_inode foreign key (structure_inode) references structure(inode);


ALTER TABLE structure ALTER fixed SET NOT NULL;
ALTER TABLE structure ALTER fixed SET DEFAULT false;

ALTER TABLE field ALTER fixed SET NOT NULL;
ALTER TABLE field ALTER fixed SET DEFAULT false;
ALTER TABLE field ALTER read_only SET NOT NULL;
ALTER TABLE field ALTER read_only SET DEFAULT false;

ALTER TABLE campaign ALTER active SET DEFAULT false;

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('dotcms.org.default', 'default', current_timestamp, 'password', 'f', 'f', '', '', '', 't', '01/01/1970', 'default@dotcms.org', '01', 'f', 'f', 'Welcome!', '', current_timestamp, 0, 'f', 't');
create index addres_userid_index on address(userid);
create index tag_user_id_index on tag(user_id);
create index tag_inode_tagid on tag_inode(tag_id);
create index tag_inode_inode on tag_inode(inode);
-- These two indexes are here instead of the hibernate file because Oracle by default creates an index on a unique field.  So creating an index would try to create the same index twice.
create index idx_chain_link_code_classname on chain_link_code (class_name);
create index idx_chain_key_name on chain (key_name);
CREATE TABLE dist_journal
(
  id bigserial NOT NULL,
  object_to_index character varying(1024) NOT NULL,
  serverid character varying(64),
  journal_type integer NOT NULL,
  time_entered timestamp without time zone NOT NULL,
  CONSTRAINT dist_journal_pkey PRIMARY KEY (id),
  CONSTRAINT dist_journal_object_to_index_key UNIQUE (object_to_index, serverid, journal_type)
);

create table plugin_property (
   plugin_id varchar(255) not null,
   propkey varchar(255) not null,
   original_value varchar(255) not null,
   current_value varchar(255) not null,
   primary key (plugin_id, propkey)
);
alter table plugin_property add constraint fk_plugin_plugin_property foreign key (plugin_id) references plugin(id);

CREATE TABLE dist_process ( id bigserial NOT NULL, object_to_index character varying(1024) NOT NULL, serverid character varying(64), journal_type integer NOT NULL, time_entered timestamp without time zone NOT NULL, CONSTRAINT dist_process_pkey PRIMARY KEY (id));
CREATE INDEX dist_process_index on dist_process (object_to_index, serverid,journal_type);

CREATE TABLE dist_reindex_journal
(
  id bigserial NOT NULL,
  inode_to_index character varying(100) NOT NULL,
  ident_to_index character varying(100) NOT NULL,
  serverid character varying(64),
  priority integer NOT NULL,
  time_entered timestamp without time zone NOT NULL DEFAULT CURRENT_DATE,
  index_val varchar(325),
  dist_action integer NOT NULL DEFAULT 1,
  CONSTRAINT dist_reindex_journal_pkey PRIMARY KEY (id)
);

CREATE INDEX dist_reindex_index1 on dist_reindex_journal (inode_to_index);
CREATE INDEX dist_reindex_index2 on dist_reindex_journal (dist_action);
CREATE INDEX dist_reindex_index3 on dist_reindex_journal (serverid);
CREATE INDEX dist_reindex_index4 on dist_reindex_journal (ident_to_index,serverid);
CREATE INDEX dist_reindex_index on dist_reindex_journal (serverid,dist_action);
CREATE INDEX dist_reindex_index5 ON dist_reindex_journal (priority, time_entered);
CREATE INDEX dist_reindex_index6 ON dist_reindex_journal (priority);
CREATE INDEX idx_identifier ON identifier USING btree (id);

CREATE TABLE quartz_log (id bigserial NOT NULL, JOB_NAME character varying(255) NOT NULL, serverid character varying(64), time_started timestamp without time zone NOT NULL, CONSTRAINT quartz_log_pkey PRIMARY KEY (id));

alter table cms_role add CONSTRAINT cms_role_name_role_key UNIQUE (role_key);
alter table cms_role add CONSTRAINT cms_role_name_db_fqn UNIQUE (db_fqn);
alter table cms_role add constraint fkcms_role_parent foreign key (parent) references cms_role;

alter table users_cms_roles add CONSTRAINT users_cms_roles_parent1 UNIQUE (role_id,user_id);
alter table users_cms_roles add constraint fkusers_cms_roles1 foreign key (role_id) references cms_role;
alter table users_cms_roles add constraint fkusers_cms_roles2 foreign key (user_id) references user_;

ALTER TABLE cms_layout add CONSTRAINT cms_layout_name_parent UNIQUE (layout_name);

alter table portlet add CONSTRAINT portlet_role_key UNIQUE (portletid);
alter table cms_layouts_portlets add CONSTRAINT cms_layouts_portlets_parent1 UNIQUE (layout_id,portlet_id);
alter table cms_layouts_portlets add constraint fkcms_layouts_portlets foreign key (layout_id) references cms_layout;

alter table layouts_cms_roles add constraint fklayouts_cms_roles1 foreign key (role_id) references cms_role;
alter table layouts_cms_roles add constraint fklayouts_cms_roles2 foreign key (layout_id) references cms_layout;
alter table layouts_cms_roles add CONSTRAINT layouts_cms_roles_parent1 UNIQUE (role_id,layout_id);

ALTER TABLE containers add constraint containers_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE template add constraint template_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE htmlpage add constraint htmlpage_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE file_asset add constraint file_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE contentlet add constraint content_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE links add constraint links_identifier_fk foreign key (identifier) references identifier(id);


CREATE OR REPLACE FUNCTION "boolIntResult"("intParam" integer, "boolParam" boolean)
  RETURNS boolean AS
$BODY$select case
		WHEN $2 AND $1 != 0 then true
		WHEN $2 != true AND $1 = 0 then true
		ELSE false
	END$BODY$
  LANGUAGE 'sql' VOLATILE
;
CREATE OR REPLACE FUNCTION "intBoolResult"("boolParam" boolean, "intParam" integer)
  RETURNS boolean AS
$BODY$select case
		WHEN $1 AND $2 != 0 then true
		WHEN $1 != true AND $2 = 0 then true
		ELSE false
	END$BODY$
  LANGUAGE 'sql' VOLATILE
 ;
CREATE OPERATOR =(
  PROCEDURE = "intBoolResult",
  LEFTARG = bool,
  RIGHTARG = int4);

CREATE OPERATOR =(
  PROCEDURE = "boolIntResult",
  LEFTARG = int4,
  RIGHTARG = bool);

CREATE OR REPLACE FUNCTION "boolBigIntResult"("intParam" bigint, "boolParam" boolean)
  RETURNS boolean AS
$BODY$select case
		WHEN $2 AND $1 != 0 then true
		WHEN $2 != true AND $1 = 0 then true
		ELSE false
	END$BODY$
  LANGUAGE 'sql' VOLATILE
;
CREATE OR REPLACE FUNCTION "bigIntBoolResult"("boolParam" boolean, "intParam" bigint)
  RETURNS boolean AS
$BODY$select case
		WHEN $1 AND $2 != 0 then true
		WHEN $1 != true AND $2 = 0 then true
		ELSE false
	END$BODY$
  LANGUAGE 'sql' VOLATILE
;
CREATE OPERATOR =(
   PROCEDURE="bigIntBoolResult",
   LEFTARG=boolean,
   RIGHTARG=bigint);


CREATE OPERATOR =(
  PROCEDURE = "boolBigIntResult",
  LEFTARG = bigint,
  RIGHTARG = bool);

CREATE OR REPLACE FUNCTION identifier_host_inode_check() RETURNS trigger AS '
DECLARE
	inodeType varchar(100);
BEGIN
  IF (tg_op = ''INSERT'' OR tg_op = ''UPDATE'') AND substr(NEW.asset_type, 0, 8) <> ''content'' AND
		(NEW.host_inode IS NULL OR NEW.host_inode = '''') THEN
		RAISE EXCEPTION ''Cannot insert/update a null or empty host inode for this kind of identifier'';
		RETURN NULL;
  ELSE
		RETURN NEW;
  END IF;

  RETURN NULL;
END
' LANGUAGE plpgsql;

CREATE TRIGGER required_identifier_host_inode_trigger BEFORE INSERT OR UPDATE
    ON identifier FOR EACH ROW
    EXECUTE PROCEDURE identifier_host_inode_check ();

create table import_audit (
	id bigint not null,
	start_date timestamp,
	userid varchar(255),
	filename varchar(512),
	status int,
	last_inode varchar(100),
	records_to_import bigint,
	serverid varchar(255),
	primary key (id)
	);

alter table category alter column category_velocity_var_name set not null;

alter table import_audit add column warnings text,
	add column errors text,
	add column results text,
	add column messages text;

alter table structure alter host set default 'SYSTEM_HOST';
alter table structure alter folder set default 'SYSTEM_FOLDER';
alter table structure add constraint fk_structure_folder foreign key (folder) references folder(inode);
alter table structure alter column velocity_var_name set not null;
alter table structure add constraint unique_struct_vel_var_name unique (velocity_var_name);

CREATE OR REPLACE FUNCTION structure_host_folder_check() RETURNS trigger AS '
DECLARE
    folderInode varchar(100);
    hostInode varchar(100);
BEGIN
    IF ((tg_op = ''INSERT'' OR tg_op = ''UPDATE'') AND (NEW.host IS NOT NULL AND NEW.host <> '''' AND NEW.host <> ''SYSTEM_HOST''
          AND NEW.folder IS NOT NULL AND NEW.folder <> ''SYSTEM_FOLDER'' AND NEW.folder <> '''')) THEN
          select host_inode,folder.inode INTO hostInode,folderInode from folder,identifier where folder.identifier = identifier.id and folder.inode=NEW.folder;
	  IF (FOUND AND NEW.host = hostInode) THEN
		 RETURN NEW;
	  ELSE
		 RAISE EXCEPTION ''Cannot assign host/folder to structure, folder does not belong to given host'';
		 RETURN NULL;
	  END IF;
    ELSE
        IF((tg_op = ''INSERT'' OR tg_op = ''UPDATE'') AND (NEW.host IS NULL OR NEW.host = '''' OR NEW.host= ''SYSTEM_HOST''
           OR NEW.folder IS NULL OR NEW.folder = '''' OR NEW.folder = ''SYSTEM_FOLDER'')) THEN
          IF(NEW.host = ''SYSTEM_HOST'' OR NEW.host IS NULL OR NEW.host = '''') THEN
             NEW.host = ''SYSTEM_HOST'';
             NEW.folder = ''SYSTEM_FOLDER'';
          END IF;
          IF(NEW.folder = ''SYSTEM_FOLDER'' OR NEW.folder IS NULL OR NEW.folder = '''') THEN
             NEW.folder = ''SYSTEM_FOLDER'';
          END IF;
        RETURN NEW;
        END IF;
    END IF;
  RETURN NULL;
END
' LANGUAGE plpgsql;

CREATE TRIGGER structure_host_folder_trigger BEFORE INSERT OR UPDATE
    ON structure FOR EACH ROW
    EXECUTE PROCEDURE structure_host_folder_check();

CREATE OR REPLACE FUNCTION load_records_to_index(server_id character varying, records_to_fetch int)
  RETURNS SETOF dist_reindex_journal AS'
DECLARE
   dj dist_reindex_journal;
BEGIN

    FOR dj IN SELECT * FROM dist_reindex_journal
       WHERE serverid IS NULL
       ORDER BY priority ASC
       LIMIT records_to_fetch
       FOR UPDATE
    LOOP
        UPDATE dist_reindex_journal SET serverid=server_id WHERE id=dj.id;
        RETURN NEXT dj;
    END LOOP;

END'
LANGUAGE 'plpgsql';


 CREATE OR REPLACE FUNCTION file_versions_check() RETURNS trigger AS '
   DECLARE
	 versionsCount integer;
   BEGIN
	IF (tg_op = ''DELETE'') THEN
          select count(*) into versionsCount from file_asset where identifier = OLD.identifier;
          IF (versionsCount = 0)THEN
             DELETE from identifier where id = OLD.identifier;
          ELSE
             RETURN OLD;
          END IF;
       END IF;
    RETURN NULL;
  END
' LANGUAGE plpgsql;
CREATE TRIGGER file_versions_check_trigger AFTER DELETE
ON file_asset FOR EACH ROW
EXECUTE PROCEDURE file_versions_check();


CREATE OR REPLACE FUNCTION content_versions_check() RETURNS trigger AS '
   DECLARE
       versionsCount integer;
   BEGIN
       IF (tg_op = ''DELETE'') THEN
         select count(*) into versionsCount from contentlet where identifier = OLD.identifier;
         IF (versionsCount = 0)THEN
		DELETE from identifier where id = OLD.identifier;
	   ELSE
	      RETURN OLD;
	   END IF;
	END IF;
   RETURN NULL;
   END
  ' LANGUAGE plpgsql;
 CREATE TRIGGER content_versions_check_trigger AFTER DELETE
 ON contentlet FOR EACH ROW
 EXECUTE PROCEDURE content_versions_check();

CREATE OR REPLACE FUNCTION link_versions_check() RETURNS trigger AS '
  DECLARE
	versionsCount integer;
  BEGIN
  IF (tg_op = ''DELETE'') THEN
    select count(*) into versionsCount from links where identifier = OLD.identifier;
    IF (versionsCount = 0)THEN
	DELETE from identifier where id = OLD.identifier;
    ELSE
	RETURN OLD;
    END IF;
  END IF;
RETURN NULL;
END
' LANGUAGE plpgsql;
CREATE TRIGGER link_versions_check_trigger AFTER DELETE
ON links FOR EACH ROW
EXECUTE PROCEDURE link_versions_check();


CREATE OR REPLACE FUNCTION container_versions_check() RETURNS trigger AS '
  DECLARE
	versionsCount integer;
  BEGIN
  IF (tg_op = ''DELETE'') THEN
    select count(*) into versionsCount from containers where identifier = OLD.identifier;
    IF (versionsCount = 0)THEN
	DELETE from identifier where id = OLD.identifier;
    ELSE
	RETURN OLD;
    END IF;
  END IF;
RETURN NULL;
END
' LANGUAGE plpgsql;

CREATE TRIGGER container_versions_check_trigger AFTER DELETE
ON containers FOR EACH ROW
EXECUTE PROCEDURE container_versions_check();

CREATE OR REPLACE FUNCTION template_versions_check() RETURNS trigger AS '
  DECLARE
	versionsCount integer;
  BEGIN
  IF (tg_op = ''DELETE'') THEN
    select count(*) into versionsCount from template where identifier = OLD.identifier;
    IF (versionsCount = 0)THEN
	DELETE from identifier where id = OLD.identifier;
    ELSE
	RETURN OLD;
    END IF;
  END IF;
RETURN NULL;
END
' LANGUAGE plpgsql;

CREATE TRIGGER template_versions_check_trigger AFTER DELETE
ON template FOR EACH ROW
EXECUTE PROCEDURE template_versions_check();

CREATE OR REPLACE FUNCTION htmlpage_versions_check() RETURNS trigger AS '
  DECLARE
	versionsCount integer;
  BEGIN
  IF (tg_op = ''DELETE'') THEN
    select count(*) into versionsCount from htmlpage where identifier = OLD.identifier;
    IF (versionsCount = 0)THEN
	DELETE from identifier where id = OLD.identifier;
    ELSE
	RETURN OLD;
    END IF;
  END IF;
RETURN NULL;
END
' LANGUAGE plpgsql;

CREATE TRIGGER htmlpage_versions_check_trigger AFTER DELETE
ON htmlpage FOR EACH ROW
EXECUTE PROCEDURE htmlpage_versions_check();

CREATE OR REPLACE FUNCTION identifier_parent_path_check() RETURNS trigger AS '
 DECLARE
    folderId varchar(100);
  BEGIN
     IF (tg_op = ''INSERT'' OR tg_op = ''UPDATE'') THEN
      IF(NEW.parent_path=''/'') OR (NEW.parent_path=''/System folder'') THEN
        RETURN NEW;
     ELSE
      select id into folderId from identifier where asset_type=''folder'' and host_inode = NEW.host_inode and parent_path||asset_name||''/'' = NEW.parent_path and id <> NEW.id;
      IF FOUND THEN
        RETURN NEW;
      ELSE
        RAISE EXCEPTION ''Cannot insert/update for this path does not exist for the given host'';
        RETURN NULL;
      END IF;
     END IF;
    END IF;
RETURN NULL;
END
  ' LANGUAGE plpgsql;
CREATE TRIGGER identifier_parent_path_trigger
  BEFORE INSERT OR UPDATE
  ON identifier FOR EACH ROW
  EXECUTE PROCEDURE identifier_parent_path_check();

CREATE OR REPLACE FUNCTION check_child_assets() RETURNS trigger AS '
DECLARE
   pathCount integer;
BEGIN
   IF (tg_op = ''DELETE'') THEN
      IF(OLD.asset_type =''folder'') THEN
	   select count(*) into pathCount from identifier where parent_path = OLD.parent_path||OLD.asset_name||''/''  and host_inode = OLD.host_inode;
	END IF;
	IF(OLD.asset_type =''contentlet'') THEN
	   select count(*) into pathCount from identifier where host_inode = OLD.id;
	END IF;
	IF (pathCount > 0 )THEN
	  RAISE EXCEPTION ''Cannot delete as this path has children'';
	  RETURN NULL;
	ELSE
	  RETURN OLD;
	END IF;
   END IF;
   RETURN NULL;
END
' LANGUAGE plpgsql;

CREATE TRIGGER check_child_assets_trigger BEFORE DELETE
ON identifier FOR EACH ROW
EXECUTE PROCEDURE check_child_assets();

alter table structure add constraint fk_structure_host foreign key (host) references identifier(id);

create index idx_template3 on template (lower(title));

CREATE INDEX idx_contentlet_4 ON contentlet (structure_inode);

alter table contentlet add constraint fk_user_contentlet foreign key (mod_user) references user_(userid);
alter table htmlpage add constraint fk_user_htmlpage foreign key (mod_user) references user_(userid);
alter table containers add constraint fk_user_containers foreign key (mod_user) references user_(userid);
alter table template add constraint fk_user_template foreign key (mod_user) references user_(userid);
alter table file_asset add constraint fk_user_file_asset foreign key (mod_user) references user_(userid);
alter table links add constraint fk_user_links foreign key (mod_user) references user_(userid);

ALTER TABLE Folder add constraint folder_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE containers add constraint structure_fk foreign key (structure_inode) references structure(inode);
ALTER TABLE htmlpage add constraint template_id_fk foreign key (template_id) references identifier(id);

CREATE OR REPLACE FUNCTION check_template_id()RETURNS trigger AS '
DECLARE
   templateId varchar(100);
BEGIN
   IF (tg_op = ''INSERT'' OR tg_op = ''UPDATE'') THEN
  	 select id into templateId from identifier where asset_type=''template'' and id = NEW.template_id;
  	 IF FOUND THEN
          RETURN NEW;
	 ELSE
	    RAISE EXCEPTION ''Template Id should be the identifier of a template'';
	    RETURN NULL;
	 END IF;
   END IF;
   RETURN NULL;
END
' LANGUAGE plpgsql;
CREATE TRIGGER check_template_identifier
BEFORE INSERT OR UPDATE
ON htmlpage
FOR EACH ROW
EXECUTE PROCEDURE check_template_id();

CREATE OR REPLACE FUNCTION folder_identifier_check() RETURNS trigger AS '
DECLARE
   versionsCount integer;
BEGIN
   IF (tg_op = ''DELETE'') THEN
      select count(*) into versionsCount from folder where identifier = OLD.identifier;
	IF (versionsCount = 0)THEN
	  DELETE from identifier where id = OLD.identifier;
	ELSE
	  RETURN OLD;
	END IF;
   END IF;
   RETURN NULL;
END
' LANGUAGE plpgsql;
CREATE TRIGGER folder_identifier_check_trigger AFTER DELETE
ON folder FOR EACH ROW
EXECUTE PROCEDURE folder_identifier_check();

create index idx_template_id on template_containers(template_id);
alter table template_containers add constraint FK_template_id foreign key (template_id) references identifier(id);
alter table template_containers add constraint FK_container_id foreign key (container_id) references identifier(id);

CREATE OR REPLACE FUNCTION renameFolderChildren(old_path varchar(100),new_path varchar(100),hostInode varchar(100))
RETURNS void AS '
DECLARE
   fi identifier;
   new_folder_path varchar(100);
   old_folder_path varchar(100);
BEGIN
    UPDATE identifier SET  parent_path  = new_path where parent_path = old_path and host_inode = hostInode;
    FOR fi IN select * from identifier where asset_type=''folder'' and parent_path = new_path and host_inode = hostInode LOOP
	 new_folder_path := new_path ||fi.asset_name||''/'';
	 old_folder_path := old_path ||fi.asset_name||''/'';
	 PERFORM renameFolderChildren(old_folder_path,new_folder_path,hostInode);
    END LOOP;
END
'LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION rename_folder_and_assets()
RETURNS trigger AS '
DECLARE
   old_parent_path varchar(100);
   old_path varchar(100);
   new_path varchar(100);
   old_name varchar(100);
   hostInode varchar(100);
BEGIN
   IF (tg_op = ''UPDATE'' AND NEW.name<>OLD.name) THEN
      select asset_name,parent_path,host_inode INTO old_name,old_parent_path,hostInode from identifier where id = NEW.identifier;
      old_path := old_parent_path || old_name || ''/'';
      new_path := old_parent_path || NEW.name || ''/'';
      UPDATE identifier SET asset_name = NEW.name where id = NEW.identifier;
      PERFORM renameFolderChildren(old_path,new_path,hostInode);
      RETURN NEW;
   END IF;
RETURN NULL;
END
'LANGUAGE plpgsql;
CREATE TRIGGER rename_folder_assets_trigger AFTER UPDATE
ON Folder FOR EACH ROW
EXECUTE PROCEDURE rename_folder_and_assets();

CREATE OR REPLACE FUNCTION dotFolderPath(parent_path text, asset_name text)
  RETURNS text AS '
BEGIN
  IF(parent_path=''/System folder'') THEN
    RETURN ''/'';
  ELSE
    RETURN parent_path || asset_name || ''/'';
  END IF;
END;'
LANGUAGE plpgsql;

alter table contentlet_version_info add constraint fk_contentlet_version_info_identifier foreign key (identifier) references identifier(id) on delete cascade;
alter table container_version_info  add constraint fk_container_version_info_identifier  foreign key (identifier) references identifier(id);
alter table template_version_info   add constraint fk_template_version_info_identifier   foreign key (identifier) references identifier(id);
alter table htmlpage_version_info   add constraint fk_htmlpage_version_info_identifier   foreign key (identifier) references identifier(id);
alter table fileasset_version_info  add constraint fk_fileasset_version_info_identifier  foreign key (identifier) references identifier(id);
alter table link_version_info       add constraint fk_link_version_info_identifier       foreign key (identifier) references identifier(id);

alter table contentlet_version_info add constraint fk_contentlet_version_info_working foreign key (working_inode) references contentlet(inode);
alter table container_version_info  add constraint fk_container_version_info_working  foreign key (working_inode) references containers(inode);
alter table template_version_info   add constraint fk_template_version_info_working   foreign key (working_inode) references template(inode);
alter table htmlpage_version_info   add constraint fk_htmlpage_version_info_working   foreign key (working_inode) references htmlpage(inode);
alter table fileasset_version_info  add constraint fk_fileasset_version_info_working  foreign key (working_inode) references file_asset(inode);
alter table link_version_info       add constraint fk_link_version_info_working       foreign key (working_inode) references links(inode);

alter table contentlet_version_info add constraint fk_contentlet_version_info_live foreign key (live_inode) references contentlet(inode);
alter table container_version_info  add constraint fk_container_version_info_live  foreign key (live_inode) references containers(inode);
alter table template_version_info   add constraint fk_template_version_info_live   foreign key (live_inode) references template(inode);
alter table htmlpage_version_info   add constraint fk_htmlpage_version_info_live   foreign key (live_inode) references htmlpage(inode);
alter table fileasset_version_info  add constraint fk_fileasset_version_info_live  foreign key (live_inode) references file_asset(inode);
alter table link_version_info       add constraint fk_link_version_info_live       foreign key (live_inode) references links(inode);

alter table contentlet_version_info add constraint fk_contentlet_version_info_lang foreign key (lang) references language(id);

alter table folder add constraint fk_folder_file_structure_type foreign key(default_file_type) references structure(inode);

alter table workflowtask_files add constraint FK_workflow_id foreign key (workflowtask_id) references workflow_task(id);
--alter table workflowtask_files add constraint FK_task_file_inode foreign key (file_inode) references file_asset(inode);

alter table workflow_comment add constraint workflowtask_id_comment_FK foreign key (workflowtask_id) references workflow_task(id);
alter table workflow_history add constraint workflowtask_id_history_FK foreign key (workflowtask_id) references workflow_task(id);

alter table contentlet add constraint fk_contentlet_lang foreign key (language_id) references language(id);



create table workflow_scheme(
	id varchar(36) primary key,
	name varchar(255) not null,
	description text,
	archived boolean default false,
	mandatory boolean default false,
	default_scheme boolean default false,
	entry_action_id varchar(36)
);

create table workflow_step(
	id varchar(36) primary key,
	name varchar(255) not null,
	scheme_id varchar(36) references workflow_scheme(id),
	my_order int default 0,
	resolved boolean default false,
	escalation_enable boolean default false,
	escalation_action varchar(36),
	escalation_time int default 0
);
create index workflow_idx_step_scheme on workflow_step(scheme_id);

-- Permissionable ---
create table workflow_action(
	id varchar(36) primary key,
	step_id varchar(36) not null  references workflow_step(id),
	name varchar(255) not null,
	condition_to_progress text,
	next_step_id varchar(36) not null references workflow_step(id),
	next_assign varchar(36) not null references cms_role(id),
	my_order int default 0,
	assignable boolean default false,
	commentable boolean default false,
	requires_checkout boolean default false,
	icon varchar(255) default 'defaultWfIcon',
	use_role_hierarchy_assign bool default false
);
create index workflow_idx_action_step on workflow_action(step_id);


create table workflow_action_class(
	id varchar(36) primary key,
	action_id varchar(36) references workflow_action(id),
	name varchar(255) not null,
	my_order int default 0,
	clazz text
);
create index workflow_idx_action_class_action on workflow_action_class(action_id);

create table workflow_action_class_pars(
	id varchar(36) primary key,
	workflow_action_class_id varchar(36) references workflow_action_class(id),
	key varchar(255) not null,
	value text
);
create index workflow_idx_action_class_param_action on
	workflow_action_class_pars(workflow_action_class_id);


create table workflow_scheme_x_structure(
	id varchar(36) primary key,
	scheme_id varchar(36) references workflow_scheme(id),
	structure_id varchar(36) references structure(inode)
);

create unique index workflow_idx_scheme_structure_2 on
	workflow_scheme_x_structure(structure_id);


delete from workflow_history;
delete from workflow_comment;
delete from workflowtask_files;
delete from workflow_task;
alter table workflow_task add constraint FK_workflow_task_asset foreign key (webasset) references identifier(id);
alter table workflow_task add constraint FK_workflow_assign foreign key (assigned_to) references cms_role(id);
alter table workflow_task add constraint FK_workflow_step foreign key (status) references workflow_step(id);
alter table workflow_step add constraint fk_escalation_action foreign key (escalation_action) references workflow_action(id);


alter table contentlet_version_info add constraint FK_con_ver_lockedby foreign key (locked_by) references user_(userid);
alter table container_version_info  add constraint FK_tainer_ver_info_lockedby  foreign key (locked_by) references user_(userid);
alter table template_version_info   add constraint FK_temp_ver_info_lockedby   foreign key (locked_by) references user_(userid);
alter table htmlpage_version_info   add constraint FK_page_ver_info_lockedby   foreign key (locked_by) references user_(userid);
alter table fileasset_version_info  add constraint FK_fil_ver_info_lockedby  foreign key (locked_by) references user_(userid);
alter table link_version_info       add constraint FK_link_ver_info_lockedby       foreign key (locked_by) references user_(userid);


ALTER TABLE tag ALTER COLUMN host_id set default 'SYSTEM_HOST';
alter table tag add constraint tag_tagname_host unique (tagname, host_id);
alter table tag_inode add constraint fk_tag_inode_tagid foreign key (tag_id) references tag (tag_id);

ALTER TABLE tag ALTER COLUMN user_id TYPE text;

-- ****** Indicies Data Storage *******
create table indicies (
  index_name varchar(30) primary key,
  index_type varchar(16) not null unique
);
-- ****** Log Console Table *******
  CREATE TABLE log_mapper (
    enabled   	 numeric(1,0) NOT null,
    log_name 	 varchar(30) NOT null,
    description  varchar(50) NOT null,
    primary key (log_name)
  );

  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-userActivity.log','Log Users action on pages, structures, documents.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-security.log','Log users login activity into dotCMS.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-adminaudit.log','Log Admin activity on dotCMS.');
  
create index idx_identifier_perm on identifier (asset_type,host_inode);

-- ****** Content Publishing Framework *******
CREATE TABLE publishing_queue
(id bigserial PRIMARY KEY NOT NULL,
operation int8, asset VARCHAR(2000) NOT NULL,
language_id  int8 NOT NULL, entered_date TIMESTAMP,
last_try TIMESTAMP, num_of_tries int8 NOT NULL DEFAULT 0,
in_error bool DEFAULT 'f', last_results TEXT,
publish_date TIMESTAMP, server_id VARCHAR(256), 
type VARCHAR(256), bundle_id VARCHAR(256), target text);

CREATE TABLE publishing_queue_audit
(bundle_id VARCHAR(256) PRIMARY KEY NOT NULL, 
status INTEGER, 
status_pojo text, 
status_updated TIMESTAMP, 
create_date TIMESTAMP);

-- ****** Content Publishing Framework - End Point Management *******
CREATE TABLE publishing_end_point (
	server_name varchar(1024) unique,
	address varchar(250),
	enabled bool,
	auth_key varchar(1024),
	sending bool);