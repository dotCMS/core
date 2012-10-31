--oracle
CREATE INDEX idx_tree_1 ON tree (parent);
CREATE INDEX idx_tree_2 ON tree (child);
CREATE INDEX idx_tree_3 ON tree (relation_type);
CREATE INDEX idx_tree_4 ON tree (parent, child, relation_type);
CREATE INDEX idx_tree_5 ON tree (parent, relation_type);
CREATE INDEX idx_tree_6 ON tree (child, relation_type);

CREATE INDEX idx_permisision_4 ON permission (permission_type);

CREATE INDEX idx_permission_reference_2 ON permission_reference (reference_id);
CREATE INDEX idx_permission_reference_3 ON permission_reference (reference_id,permission_type);
CREATE INDEX idx_permission_reference_4 ON permission_reference (asset_id,permission_type);
CREATE INDEX idx_permission_reference_5 ON permission_reference (asset_id,reference_id,permission_type);
CREATE INDEX idx_permission_reference_6 ON permission_reference (permission_type);

CREATE UNIQUE INDEX idx_field_velocity_structure ON field (velocity_var_name,structure_inode);

alter table chain_state add constraint fk_state_chain foreign key (chain_id) references chain(id);
alter table chain_state add constraint fk_state_code foreign key (link_code_id) references chain_link_code(id);
alter table chain_state_parameter add constraint fk_parameter_state foreign key (chain_state_id) references chain_state(id);

alter table permission add constraint permission_role_fk foreign key (roleid) references cms_role(id);

alter table contentlet add constraint FK_structure_inode foreign key (structure_inode) references structure(inode);

ALTER TABLE structure MODIFY fixed DEFAULT 0;

ALTER TABLE field MODIFY fixed DEFAULT 0;
ALTER TABLE field MODIFY read_only DEFAULT 0;

ALTER TABLE campaign MODIFY active DEFAULT 0;

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('dotcms.org.default', 'default', sysdate, 'password', '0', '0', '', '', '', '1', to_date('1970-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS'), 'default@dotcms.org', '01', '0', '0', 'Welcome!', '', sysdate, 0, '0', '1');

create index addres_userid_index on address(userid);
create index tag_user_id_index on tag(user_id);
create index tag_inode_tagid on tag_inode(tag_id);
create index tag_inode_inode on tag_inode(inode);
CREATE TABLE "DIST_JOURNAL" ( "ID" INTEGER NOT NULL ,
"OBJECT_TO_INDEX" VARCHAR2(255), "SERVERID" VARCHAR2(64),
"JOURNAL_TYPE" INTEGER, "TIME_ENTERED" TIMESTAMP, PRIMARY KEY ("ID")
VALIDATE , UNIQUE ("OBJECT_TO_INDEX", "SERVERID", "JOURNAL_TYPE")
VALIDATE );
CREATE SEQUENCE dist_journal_id_seq
START WITH 1
INCREMENT BY 1;
create trigger DIST_JOURNAL_trg
before insert on DIST_JOURNAL
for each row
when (new.id is null)
begin
    select dist_journal_id_seq.nextval into :new.id from dual;
end;
/

CREATE TABLE dist_process ( ID INTEGER NOT NULL , OBJECT_TO_INDEX VARCHAR2(255), SERVERID VARCHAR2(64), JOURNAL_TYPE INTEGER, TIME_ENTERED TIMESTAMP, PRIMARY KEY (ID) VALIDATE );
CREATE SEQUENCE dist_process_id_seq START WITH 1 INCREMENT BY 1;
create trigger dist_process_trg
before insert on dist_process
for each row
when (new.id is null)
begin
select dist_process_id_seq.nextval into :new.id from dual;
end;
/

CREATE INDEX dist_process_index on dist_process (object_to_index, serverid,journal_type);


CREATE TABLE dist_reindex_journal (
  ID INTEGER NOT NULL ,
  INODE_TO_INDEX varchar2(100),
  IDENT_TO_INDEX varchar2(100),
  SERVERID VARCHAR2(64),
  priority INTEGER,
  TIME_ENTERED TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  index_val varchar2(325) ,
  dist_action INTEGER DEFAULT 1 NOT NULL,
  PRIMARY KEY (ID) VALIDATE);

CREATE INDEX dist_reindex_index1 on dist_reindex_journal (inode_to_index);
CREATE INDEX dist_reindex_index2 on dist_reindex_journal (dist_action);
CREATE INDEX dist_reindex_index3 on dist_reindex_journal (serverid);
CREATE INDEX dist_reindex_index4 on dist_reindex_journal (ident_to_index,serverid);
CREATE INDEX dist_reindex_index on dist_reindex_journal (serverid,dist_action);
CREATE INDEX dist_reindex_index5 ON dist_reindex_journal (priority, time_entered);
CREATE INDEX dist_reindex_index6 ON dist_reindex_journal (priority);

CREATE SEQUENCE dist_reindex_id_seq START WITH 1 INCREMENT BY 1;

create trigger dist_reindex_journal_trg
		before insert on dist_reindex_journal
		for each row
		when (new.id is null)
		begin
		select dist_reindex_id_seq.nextval into :new.id from dual;
		end;
/


CREATE TABLE quartz_log ( ID INTEGER NOT NULL , JOB_NAME VARCHAR2(255), SERVERID VARCHAR2(64),  TIME_STARTED TIMESTAMP, PRIMARY KEY (ID) VALIDATE );

create table plugin_property (
   plugin_id varchar2(255) not null,
   propkey varchar2(255) not null,
   original_value varchar2(255) not null,
   current_value varchar2(255) not null,
   primary key (plugin_id, propkey)
);

alter table plugin_property add constraint fk_plugin_plugin_property foreign key (plugin_id) references plugin(id);

CREATE SEQUENCE quartz_log_id_seq START WITH 1 INCREMENT BY 1;
create trigger quartz_log_trg
before insert on quartz_log
for each row
when (new.id is null)
begin
select quartz_log_id_seq.nextval into :new.id from dual;
end;
/

CREATE OR REPLACE TRIGGER check_identifier_host_inode
BEFORE INSERT OR UPDATE ON identifier
FOR EACH ROW
DECLARE
BEGIN
    dbms_output.put_line('asset_type: ' || SUBSTR(:new.asset_type,0,7));
    dbms_output.put_line('host_inode: ' || :new.host_inode);
    IF SUBSTR(:new.asset_type,0,7) <> 'content' AND (:new.host_inode is NULL OR :new.host_inode = '') THEN
    	RAISE_APPLICATION_ERROR(-20000, 'Cannot insert/update a null or empty host inode for this kind of identifier');
    END IF;
END;
/

ALTER TABLE cms_role ADD CONSTRAINT cms_role2_unique UNIQUE (role_key);
ALTER TABLE cms_role ADD CONSTRAINT cms_role3_unique UNIQUE (db_fqn);
alter table cms_role add constraint fkcms_role_parent foreign key (parent) references cms_role;

ALTER TABLE cms_layout ADD CONSTRAINT cms_layout_unique_1 UNIQUE (layout_name);

ALTER TABLE portlet ADD CONSTRAINT portlet_unique_1 UNIQUE (portletid);
ALTER TABLE cms_layouts_portlets ADD CONSTRAINT cms_layouts_portlets_unq_1 UNIQUE (portlet_id, layout_id);
alter table cms_layouts_portlets add constraint fkcms_layouts_portlets foreign key (layout_id) references cms_layout;

ALTER TABLE users_cms_roles ADD CONSTRAINT users_cms_roles1_unique UNIQUE (role_id, user_id);
alter table users_cms_roles add constraint fkusers_cms_roles1 foreign key (role_id) references cms_role;
alter table users_cms_roles add constraint fkusers_cms_roles2 foreign key (user_id) references user_;

ALTER TABLE layouts_cms_roles ADD CONSTRAINT layouts_cms_roles1_unique UNIQUE (role_id, layout_id);
alter table layouts_cms_roles add constraint fklayouts_cms_roles1 foreign key (role_id) references cms_role;
alter table layouts_cms_roles add constraint fklayouts_cms_roles2 foreign key (layout_id) references cms_layout;

ALTER TABLE containers add constraint containers_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE template add constraint template_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE htmlpage add constraint htmlpage_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE file_asset add constraint file_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE contentlet add constraint content_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE links add constraint links_identifier_fk foreign key (identifier) references identifier(id);

create table import_audit (
	id integer not null,
	start_date timestamp,
	userid varchar(255),
	filename varchar(512),
	status integer,
	last_inode varchar(100),
	records_to_import integer,
	serverid varchar(255),
	primary key (id)
	);

alter table category modify (category_velocity_var_name varchar2(255) not null);

alter table import_audit add( warnings nclob,
	errors nclob,
	results nclob,
	messages nclob);

alter table structure modify host default 'SYSTEM_HOST';
alter table structure modify folder default 'SYSTEM_FOLDER';
alter table structure add constraint fk_structure_folder foreign key (folder) references folder(inode);
alter table structure modify (velocity_var_name varchar2(255) not null);
alter table structure add constraint unique_struct_vel_var_name unique (velocity_var_name);


CREATE OR REPLACE TRIGGER structure_host_folder_trigger
BEFORE INSERT OR UPDATE ON structure
FOR EACH ROW
DECLARE
   folderInode varchar2(100);
   hostInode varchar2(100);
BEGIN
    IF (:NEW.host <> 'SYSTEM_HOST' AND :NEW.folder <> 'SYSTEM_FOLDER') THEN
        select host_inode, folder.inode INTO hostInode, folderInode from folder,identifier where folder.identifier = identifier.id and folder.inode = :NEW.folder;
	  IF (:NEW.host <> hostInode) THEN
		RAISE_APPLICATION_ERROR(-20000, 'Cannot assign host/folder to structure, folder does not belong to given host');
	  END IF;
    ELSE
       IF(:NEW.host IS NULL OR :NEW.host = '' OR :NEW.host = 'SYSTEM_HOST' OR :NEW.folder IS NULL OR :NEW.folder = '' OR :NEW.folder = 'SYSTEM_FOLDER') THEN
          IF(:NEW.host = 'SYSTEM_HOST' OR :NEW.host IS NULL OR :NEW.host = '') THEN
               :NEW.host := 'SYSTEM_HOST';
               :NEW.folder := 'SYSTEM_FOLDER';
          END IF;
          IF(:NEW.folder = 'SYSTEM_FOLDER' OR :NEW.folder IS NULL OR :NEW.folder = '') THEN
             :NEW.folder := 'SYSTEM_FOLDER';
          END IF;
       END IF;
    END IF;
END;
/
create or replace
 PACKAGE types
AS
TYPE ref_cursor IS REF CURSOR;
END;
/
CREATE OR REPLACE TYPE reindex_record AS OBJECT (
  ID INTEGER,
  INODE_TO_INDEX varchar2(36),
  IDENT_TO_INDEX varchar2(36),
  priority INTEGER,
  dist_action INTEGER
);
/
CREATE OR REPLACE TYPE reindex_record_list IS TABLE OF reindex_record;
/
CREATE OR REPLACE FUNCTION load_records_to_index(server_id VARCHAR2, records_to_fetch NUMBER)
   RETURN types.ref_cursor IS
 cursor_ret types.ref_cursor;
 data_ret reindex_record_list;
BEGIN
  data_ret := reindex_record_list();
  FOR dj in (SELECT * FROM dist_reindex_journal
         WHERE serverid IS NULL AND rownum<=records_to_fetch
         ORDER BY priority ASC
         FOR UPDATE)
  LOOP
    UPDATE dist_reindex_journal SET serverid=server_id WHERE id=dj.id;
    data_ret.extend;
    data_ret(data_ret.Last) := reindex_record(dj.id,dj.inode_to_index,dj.ident_to_index,dj.priority,dj.dist_action);
  END LOOP;
  OPEN cursor_ret FOR
    SELECT * FROM TABLE(CAST(data_ret AS reindex_record_list));
  RETURN cursor_ret;
END;
/
CREATE OR REPLACE PACKAGE check_parent_path_pkg as
    type ridArray is table of rowid index by binary_integer;
    newRows ridArray;
    empty   ridArray;
END;
/
CREATE OR REPLACE TRIGGER check_parent_path_bi
BEFORE INSERT OR UPDATE ON identifier
BEGIN
  check_parent_path_pkg.newRows := check_parent_path_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER check_parent_path_aifer
AFTER INSERT OR UPDATE ON identifier
FOR EACH ROW
BEGIN
   check_parent_path_pkg.newRows(check_parent_path_pkg.newRows.count+1) := :new.rowid;
END;
/
CREATE OR REPLACE TRIGGER identifier_parent_path_check
AFTER INSERT OR UPDATE ON identifier
DECLARE
  rowcount varchar2(100);
  assetIdentifier varchar2(100);
  parentPath varchar2(100);
  hostInode varchar2(100);
BEGIN
   for i in 1 .. check_parent_path_pkg.newRows.count LOOP
      select id,parent_path,host_inode into assetIdentifier,parentPath,hostInode from identifier where rowid = check_parent_path_pkg.newRows(i);
      IF(parentPath='/' OR parentPath='/System folder') THEN
        return;
      ELSE
        select count(*) into rowcount from identifier where asset_type='folder' and host_inode = hostInode and parent_path||asset_name||'/' = parentPath and id <> assetIdentifier;
        IF (rowcount = 0) THEN
           RAISE_APPLICATION_ERROR(-20000, 'Cannot insert/update for this path does not exist for the given host');
        END IF;
      END IF;
END LOOP;
END;
/
CREATE OR REPLACE PACKAGE htmlpage_pkg as
 type array is table of htmlpage%rowtype index by binary_integer;
 oldvals array;
 empty array;
END;
/
CREATE OR REPLACE TRIGGER htmlpage_versions_bd
BEFORE DELETE ON htmlpage
BEGIN
  htmlpage_pkg.oldvals := htmlpage_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER htmlpage_versions_bdfer
BEFORE DELETE ON htmlpage
FOR EACH ROW
BEGIN
   htmlpage_pkg.oldvals(htmlpage_pkg.oldvals.count+1).identifier := :old.identifier;
END;
/
CREATE OR REPLACE TRIGGER htmlpage_versions_trigger
AFTER DELETE ON htmlpage
DECLARE
   versionsCount integer;
BEGIN
   for i in 1 .. htmlpage_pkg.oldvals.count LOOP
     select count(*) into versionsCount from htmlpage where identifier = htmlpage_pkg.oldvals(i).identifier;
     IF (versionsCount = 0)THEN
	 DELETE from identifier where id = htmlpage_pkg.oldvals(i).identifier;
     END IF;
   END LOOP;
END;
/
CREATE OR REPLACE PACKAGE file_pkg as
  type array is table of file_asset%rowtype index by binary_integer;
  oldvals array;
  empty array;
END;
/
CREATE OR REPLACE trigger file_versions_bd
BEFORE DELETE ON file_asset
BEGIN
    file_pkg.oldvals := file_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER file_versions_bdfer
BEFORE DELETE ON file_asset
FOR EACH ROW
BEGIN
    file_pkg.oldvals(file_pkg.oldvals.count+1).identifier := :old.identifier;
END;
/
CREATE OR REPLACE TRIGGER  file_versions_trigger
AFTER DELETE ON file_asset
DECLARE
   versionsCount integer;
BEGIN
   for i in 1 .. file_pkg.oldvals.count LOOP
    select count(*) into versionsCount from file_asset where identifier = file_pkg.oldvals(i).identifier;
     IF (versionsCount = 0)THEN
	 DELETE from identifier where id = file_pkg.oldvals(i).identifier;
     END IF;
   END LOOP;
END;
/
CREATE OR REPLACE PACKAGE content_pkg as
  type array is table of contentlet%rowtype index by binary_integer;
  oldvals array;
  empty array;
END;
/
CREATE OR REPLACE TRIGGER content_versions_bd
BEFORE DELETE ON contentlet
BEGIN
   content_pkg.oldvals := content_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER  content_versions_bdfer
BEFORE DELETE ON contentlet
FOR EACH ROW
BEGIN
    content_pkg.oldvals(content_pkg.oldvals.count+1).identifier := :old.identifier;
END;
/
CREATE OR REPLACE TRIGGER  content_versions_trigger
AFTER DELETE ON contentlet
DECLARE
   versionsCount integer;
BEGIN
   for i in 1 .. content_pkg.oldvals.count LOOP
     select count(*) into versionsCount from contentlet where identifier = content_pkg.oldvals(i).identifier;
     IF (versionsCount = 0)THEN
	   DELETE from identifier where id = content_pkg.oldvals(i).identifier;
     END IF;
   END LOOP;
END;
/
CREATE OR REPLACE PACKAGE link_pkg as
 type array is table of links%rowtype index by binary_integer;
 oldvals array;
 empty array;
END;
/
CREATE OR REPLACE TRIGGER link_versions_bd
BEFORE DELETE ON links
BEGIN
   link_pkg.oldvals := link_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER link_versions_bdfer
BEFORE DELETE ON links
FOR EACH ROW
BEGIN
    link_pkg.oldvals(link_pkg.oldvals.count+1).identifier := :old.identifier;
END;
/
CREATE OR REPLACE TRIGGER link_versions_trigger
AFTER DELETE ON links
DECLARE
   versionsCount integer;
BEGIN
   for i in 1 .. link_pkg.oldvals.count LOOP
      select count(*) into versionsCount from links where identifier = link_pkg.oldvals(i).identifier;
      IF (versionsCount = 0)THEN
	  DELETE from identifier where id = link_pkg.oldvals(i).identifier;
      END IF;
END LOOP;
END;
/
CREATE OR REPLACE PACKAGE container_pkg as
 type array is table of containers%rowtype index by binary_integer;
 oldvals array;
 empty array;
END;
/
CREATE OR REPLACE TRIGGER container_versions_bd
BEFORE DELETE ON containers
BEGIN
   container_pkg.oldvals := container_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER container_versions_bdfer
BEFORE DELETE ON containers
FOR EACH ROW
BEGIN
     container_pkg.oldvals(container_pkg.oldvals.count+1).identifier := :old.identifier;
END;
/
CREATE OR REPLACE TRIGGER container_versions_trigger
AFTER DELETE ON containers
DECLARE
  versionsCount integer;
BEGIN
   for i in 1 .. container_pkg.oldvals.count LOOP
     select count(*) into versionsCount from containers where identifier = container_pkg.oldvals(i).identifier;
     IF (versionsCount = 0)THEN
	  DELETE from identifier where id = container_pkg.oldvals(i).identifier;
     END IF;
   END LOOP;
END;
/
CREATE OR REPLACE PACKAGE template_pkg as
  type array is table of template%rowtype index by binary_integer;
  oldvals    array;
  empty    array;
END;
/
CREATE OR REPLACE TRIGGER template_versions_bd
BEFORE DELETE ON template
BEGIN
   template_pkg.oldvals := template_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER template_versions_bdfer
BEFORE DELETE ON template
FOR EACH ROW
BEGIN
   template_pkg.oldvals(template_pkg.oldvals.count+1).identifier := :old.identifier;
END;
/
CREATE OR REPLACE TRIGGER template_versions_trigger
AFTER DELETE ON template
DECLARE
  versionsCount integer;
BEGIN
  for i in 1 .. template_pkg.oldvals.count LOOP
    select count(*) into versionsCount from template where identifier = template_pkg.oldvals(i).identifier;
    IF (versionsCount = 0)THEN
	DELETE from identifier where id = template_pkg.oldvals(i).identifier;
    END IF;
  END LOOP;
END;
/


ALTER TABLE clickstream MODIFY start_date TIMESTAMP;
ALTER TABLE clickstream MODIFY end_date TIMESTAMP;
ALTER TABLE clickstream_request MODIFY timestampper TIMESTAMP;
ALTER TABLE clickstream_404 MODIFY timestampper TIMESTAMP;
ALTER TABLE analytic_summary_period MODIFY full_date TIMESTAMP;
ALTER TABLE analytic_summary MODIFY avg_time_on_site TIMESTAMP;
ALTER TABLE analytic_summary_workstream MODIFY mod_date TIMESTAMP;
ALTER TABLE analytic_summary_visits MODIFY visit_time TIMESTAMP;
ALTER TABLE dashboard_user_preferences MODIFY mod_date TIMESTAMP;
alter table structure add constraint fk_structure_host foreign key (host) references identifier(id);

create index idx_template3 on template (lower(title));

CREATE INDEX idx_contentlet_4 ON contentlet (structure_inode);
ALTER TABLE Folder add constraint folder_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE containers add constraint structure_fk foreign key (structure_inode) references structure(inode);
ALTER TABLE htmlpage add constraint template_id_fk foreign key (template_id) references identifier(id);

CREATE OR REPLACE TRIGGER  check_template_identifier
BEFORE INSERT OR UPDATE ON htmlpage
FOR EACH ROW
DECLARE
  rowcount varchar2(100);
BEGIN
  select count(*) into rowcount from identifier where id= :NEW.template_id and asset_type='template';
  IF (rowcount = 0) THEN
    RAISE_APPLICATION_ERROR(-20000, 'Template Id should be the identifier of a template');
  END IF;
END;
/
CREATE OR REPLACE PACKAGE folder_pkg as
   type array is table of folder%rowtype index by binary_integer;
   oldvals array;
   empty array;
END;
/
CREATE OR REPLACE trigger folder_identifier_bd
BEFORE DELETE ON folder
BEGIN
folder_pkg.oldvals := folder_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER folder_identifier_bdfer
BEFORE DELETE ON folder
FOR EACH ROW
BEGIN
  folder_pkg.oldvals(folder_pkg.oldvals.count+1).identifier := :old.identifier;
END;
/
CREATE OR REPLACE TRIGGER  folder_identifier_trigger
AFTER DELETE ON folder
DECLARE
  versionsCount integer;
BEGIN
  for i in 1 .. folder_pkg.oldvals.count LOOP
    select count(*) into versionsCount from folder where identifier = folder_pkg.oldvals(i).identifier;
    IF (versionsCount = 0)THEN
	 DELETE from identifier where id = folder_pkg.oldvals(i).identifier;
    END IF;
 END LOOP;
END;
/
alter table contentlet add constraint fk_user_contentlet foreign key (mod_user) references user_(userid);
alter table htmlpage add constraint fk_user_htmlpage foreign key (mod_user) references user_(userid);
alter table containers add constraint fk_user_containers foreign key (mod_user) references user_(userid);
alter table template add constraint fk_user_template foreign key (mod_user) references user_(userid);
alter table file_asset add constraint fk_user_file_asset foreign key (mod_user) references user_(userid);
alter table links add constraint fk_user_links foreign key (mod_user) references user_(userid);

create index idx_template_id on template_containers(template_id);
alter table template_containers add constraint FK_template_id foreign key (template_id) references identifier(id);
alter table template_containers add constraint FK_container_id foreign key (container_id) references identifier(id);

CREATE OR REPLACE PACKAGE child_assets_pkg as
   type array is table of identifier%rowtype index by binary_integer;
   oldvals array;
   empty array;
END;
/
CREATE OR REPLACE trigger check_child_assets_bd
BEFORE DELETE ON identifier
BEGIN
child_assets_pkg.oldvals := child_assets_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER check_child_assets_bdfer
BEFORE DELETE ON identifier
FOR EACH ROW
Declare
  i    number default child_assets_pkg.oldvals.count+1;
BEGIN
  child_assets_pkg.oldvals(i).id := :old.id;
  child_assets_pkg.oldvals(i).asset_type := :old.asset_type;
  child_assets_pkg.oldvals(i).asset_name:= :old.asset_name;
  child_assets_pkg.oldvals(i).parent_path:= :old.parent_path;
  child_assets_pkg.oldvals(i).host_inode:= :old.host_inode;
END;
/
CREATE OR REPLACE TRIGGER  check_child_assets_trigger
AFTER DELETE ON identifier
DECLARE
  pathCount integer;
BEGIN
  for i in 1 .. child_assets_pkg.oldvals.count LOOP
    IF(child_assets_pkg.oldvals(i).asset_type='folder')THEN
      select count(*) into pathCount from identifier where parent_path = child_assets_pkg.oldvals(i).parent_path||child_assets_pkg.oldvals(i).asset_name||'/' and host_inode = child_assets_pkg.oldvals(i).host_inode;
    END IF;
    IF(child_assets_pkg.oldvals(i).asset_type='contentlet')THEN
      select count(*) into pathCount from identifier where host_inode = child_assets_pkg.oldvals(i).id;
    END IF;
    IF (pathCount > 0 )THEN
          RAISE_APPLICATION_ERROR(-20000, 'Cannot delete as this path has children');
    END IF;
 END LOOP;
END;
/
CREATE OR REPLACE PROCEDURE renameFolderChildren(oldPath IN varchar2,newPath IN varchar2,hostInode IN varchar2) IS
  newFolderPath varchar2(100);
  oldFolderPath varchar2(100);
  assetName varchar2(100);
BEGIN
 UPDATE identifier SET  parent_path  = newPath where parent_path = oldPath and host_inode = hostInode;
 FOR i in (select * from identifier where asset_type='folder' and parent_path = newPath and host_inode = hostInode)
  LOOP
   newFolderPath := newPath || i.asset_name || '/';
   oldFolderPath := oldPath || i.asset_name || '/';
   renameFolderChildren(oldFolderPath,newFolderPath,hostInode);
  END LOOP;
END;
/
CREATE OR REPLACE TRIGGER rename_folder_assets_trigger
AFTER UPDATE ON Folder
FOR EACH ROW
DECLARE
 oldPath varchar2(100);
 newPath varchar2(100);
 hostInode varchar2(100);
BEGIN
	IF :NEW.name <> :OLD.name THEN
      SELECT parent_path||asset_name||'/',parent_path ||:NEW.name||'/',host_inode INTO oldPath,newPath,hostInode from identifier where id = :NEW.identifier;
      UPDATE identifier SET asset_name = :NEW.name where id = :NEW.identifier;
      renameFolderChildren(oldPath,newPath,hostInode);
    END IF;
END;
/
CREATE OR REPLACE FUNCTION dotFolderPath(parent_path IN varchar2, asset_name IN varchar2) RETURN varchar2 IS
BEGIN
  IF parent_path='/System folder' THEN
    RETURN '/';
  ELSE
    RETURN parent_path || asset_name || '/';
  END IF;
END;
/
alter table contentlet_version_info add constraint fk_con_ver_info_ident foreign key (identifier) references identifier(id) on delete cascade;
alter table container_version_info  add constraint fk_container_ver_info_ident  foreign key (identifier) references identifier(id);
alter table template_version_info   add constraint fk_template_ver_info_ident   foreign key (identifier) references identifier(id);
alter table htmlpage_version_info   add constraint fk_htmlpage_ver_info_ident   foreign key (identifier) references identifier(id);
alter table fileasset_version_info  add constraint fk_fileasset_ver_info_ident  foreign key (identifier) references identifier(id);
alter table link_version_info       add constraint fk_link_ver_info_ident      foreign key (identifier) references identifier(id);

alter table contentlet_version_info add constraint fk_con_ver_info_working foreign key (working_inode) references contentlet(inode);
alter table container_version_info  add constraint fk_container_ver_info_working  foreign key (working_inode) references containers(inode);
alter table template_version_info   add constraint fk_template_ver_info_working   foreign key (working_inode) references template(inode);
alter table htmlpage_version_info   add constraint fk_htmlpage_ver_info_working   foreign key (working_inode) references htmlpage(inode);
alter table fileasset_version_info  add constraint fk_fileasset_ver_info_working  foreign key (working_inode) references file_asset(inode);
alter table link_version_info       add constraint fk_link_version_info_working       foreign key (working_inode) references links(inode);

alter table contentlet_version_info add constraint fk_con_ver_info_live foreign key (live_inode) references contentlet(inode);
alter table container_version_info  add constraint fk_container_ver_info_live  foreign key (live_inode) references containers(inode);
alter table template_version_info   add constraint fk_template_ver_info_live   foreign key (live_inode) references template(inode);
alter table htmlpage_version_info   add constraint fk_htmlpage_ver_info_live   foreign key (live_inode) references htmlpage(inode);
alter table fileasset_version_info  add constraint fk_fileasset_ver_info_live  foreign key (live_inode) references file_asset(inode);
alter table link_version_info       add constraint fk_link_version_info_live       foreign key (live_inode) references links(inode);

alter table contentlet_version_info add constraint fk_con_lang_ver_info_lang foreign key (lang) references language(id);

alter table folder add constraint fk_folder_file_structure_type foreign key(default_file_type) references structure(inode);

alter table workflowtask_files add constraint FK_workflow_id foreign key (workflowtask_id) references workflow_task(id);
--alter table workflowtask_files add constraint FK_task_file_inode foreign key (file_inode) references file_asset(inode);
alter table workflow_comment add constraint wf_id_comment_FK foreign key (workflowtask_id) references workflow_task(id);
alter table workflow_history add constraint wf_id_history_FK foreign key (workflowtask_id) references workflow_task(id);

alter table contentlet add constraint fk_contentlet_lang foreign key (language_id) references language(id);

create table workflow_scheme(
	id varchar2(36) primary key,
	name varchar2(255) not null,
	description varchar2(255),
	archived number(1,0) default 0,
	mandatory number(1,0) default 0,
	default_scheme number(1,0) default 0,
	entry_action_id varchar2(36)
);

create table workflow_step(
	id varchar2(36) primary key,
	name varchar2(255) not null,
	scheme_id varchar2(36) not null references workflow_scheme(id),
	my_order number(10,0) default 0,
	resolved number(1,0) default 0,
	escalation_enable number(1,0) default 0,
    escalation_action varchar(36),
    escalation_time number(10,0) default 0
);
create index wk_idx_step_scheme on workflow_step(scheme_id);

-- Permissionable ---
create table workflow_action(
	id varchar2(36) primary key,
	step_id varchar2(36) not null  references workflow_step(id),
	name varchar2(255) not null,
	condition_to_progress nclob,
	next_step_id varchar2(36) not null references workflow_step(id),
	next_assign varchar2(36) not null references cms_role(id),
	my_order number(10,0) default 0,
	assignable number(1,0) default 0,
	commentable number(1,0) default 0,
	requires_checkout number(1,0) default 0,
	icon varchar2(255) default 'defaultWfIcon',
	use_role_hierarchy_assign number(1,0) default 0
);
create index wk_idx_act_step on workflow_action(step_id);


create table workflow_action_class(
	id varchar2(36) primary key,
	action_id varchar2(36) not null references workflow_action(id),
	name varchar2(255) not null,
	my_order number(10,0) default 0,
	clazz nclob
);
create index wk_idx_act_class_act on workflow_action_class(action_id);

create table workflow_action_class_pars(
	id varchar2(36) primary key,
	workflow_action_class_id varchar2(36) not null references workflow_action_class(id),
	key varchar2(255) not null,
	value nclob
);
create index wk_idx_actclassparamact on
	workflow_action_class_pars(workflow_action_class_id);


create table workflow_scheme_x_structure(
	id varchar2(36) primary key,
	scheme_id varchar2(36) not null references workflow_scheme(id),
	structure_id varchar2(36) not null references structure(inode)
);

create unique index wk_idx_scheme_str_2 on
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

ALTER table tag MODIFY host_id default 'SYSTEM_HOST';
alter table tag add constraint tag_tagname_host unique (tagname, host_id);
alter table tag_inode add constraint fk_tag_inode_tagid foreign key (tag_id) references tag (tag_id);

drop index tag_user_id_index;
alter table tag modify user_id long;
alter table tag modify user_id CLOB;
create index tag_user_id_index on tag(user_id) indextype is ctxsys.context;


-- ****** Indicies Data Storage *******
create table indicies (
  index_name varchar2(30) primary key,
  index_type varchar2(16) not null unique
);
  -- ****** Log Console Table *******
  create table log_mapper (
    enabled   	 number(1,0) not null,
    log_name 	 varchar2(30) not null,
    description  varchar2(50) not null,
    primary key (log_name)
  );

  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-userActivity.log','Log Users action on pages, structures, documents.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-security.log','Log users login activity into dotCMS.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-adminaudit.log','Log Admin activity on dotCMS.');

create index idx_identifier_perm on identifier (asset_type,host_inode);

CREATE TABLE broken_link (
   inode VARCHAR(36) NOT NULL, 
   field VARCHAR(36) NOT NULL,
   link VARCHAR(255) NOT NULL,
   title VARCHAR(255) NOT NULL,
   status_code integer NOT NULL,
   primary key(inode,field)
);

alter table broken_link add CONSTRAINT fk_brokenl_content
    FOREIGN KEY (inode) REFERENCES contentlet(inode) ON DELETE CASCADE;

alter table broken_link add CONSTRAINT fk_brokenl_field
    FOREIGN KEY (field) REFERENCES field(inode) ON DELETE CASCADE;
