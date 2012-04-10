-- mysql
CREATE INDEX idx_tree ON tree (child, parent, relation_type);
CREATE INDEX idx_tree_1 ON tree (parent);
CREATE INDEX idx_tree_2 ON tree (child);
CREATE INDEX idx_tree_3 ON tree (relation_type);
CREATE INDEX idx_tree_4 ON tree (parent, child, relation_type);
CREATE INDEX idx_tree_5 ON tree (parent, relation_type);
CREATE INDEX idx_tree_6 ON tree (child, relation_type);

CREATE INDEX idx_contentlet_3 ON contentlet (inode);


CREATE INDEX idx_identifier ON identifier (id);
CREATE INDEX idx_permisision_4 ON permission (permission_type);


CREATE INDEX idx_permission_reference_2 ON permission_reference (reference_id);
CREATE INDEX idx_permission_reference_3 ON permission_reference (reference_id,permission_type);
CREATE INDEX idx_permission_reference_4 ON permission_reference (asset_id,permission_type);
CREATE INDEX idx_permission_reference_5 ON permission_reference (asset_id,reference_id,permission_type);
CREATE INDEX idx_permission_reference_6 ON permission_reference (permission_type);

CREATE UNIQUE INDEX idx_field_velocity_structure ON field (velocity_var_name,structure_inode);

alter table tree add index (parent);
alter table tree add index (child);

alter table chain_state add constraint fk_state_chain foreign key (chain_id) references chain(id);
alter table chain_state add constraint fk_state_code foreign key (link_code_id) references chain_link_code(id);
alter table chain_state_parameter add constraint fk_parameter_state foreign key (chain_state_id) references chain_state(id);

alter table permission add constraint permission_role_fk foreign key (roleid) references cms_role(id);

alter table contentlet add constraint FK_structure_inode foreign key (structure_inode) references structure(inode);

ALTER TABLE structure MODIFY fixed varchar(1) DEFAULT '0' NOT NULL;

ALTER TABLE field MODIFY fixed varchar(1) DEFAULT '0' NOT NULL;
ALTER TABLE field MODIFY read_only varchar(1) DEFAULT '1' NOT NULL;

ALTER TABLE campaign MODIFY active varchar(1) DEFAULT '0' NOT NULL;

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('dotcms.org.default', 'default', now(), 'password', '0', '0', '', '', '', '1', '1970-01-01', 'default@dotcms.org', '01', '0', '0', 'Welcome!', '', now(), 0, '0', '1');

create index addres_userid_index on address(userid);
create index tag_user_id_index on tag(user_id);
create index tag_inode_tagid on tag_inode(tag_id);
create index tag_inode_inode on tag_inode(inode);
CREATE TABLE `dist_journal` (
  `id` BIGINT  NOT NULL AUTO_INCREMENT,
  `object_to_index` VARCHAR(1024)  NOT NULL,
  `serverid` VARCHAR(64)  NOT NULL,
  `journal_type` INTEGER  NOT NULL,
  `time_entered` DATETIME  NOT NULL,
  PRIMARY KEY (`id`)
);
ALTER TABLE dist_journal ADD UNIQUE (object_to_index(255), serverid,journal_type);

create table plugin_property (
   plugin_id varchar(255) not null,
   propkey varchar(255) not null,
   original_value varchar(255) not null,
   current_value varchar(255) not null
);
alter table plugin_property add constraint fk_plugin_plugin_property foreign key (plugin_id) references plugin(id);

CREATE TABLE `dist_process` (`id` BIGINT  NOT NULL AUTO_INCREMENT,`object_to_index` VARCHAR(1024)  NOT NULL,`serverid` VARCHAR(64)  NOT NULL,`journal_type` INTEGER  NOT NULL,`time_entered` DATETIME  NOT NULL, PRIMARY KEY (`id`));
CREATE INDEX dist_process_index USING BTREE on dist_process (object_to_index (255), serverid,journal_type);

CREATE TABLE `dist_reindex_journal` (`id` BIGINT  NOT NULL AUTO_INCREMENT,`inode_to_index` VARCHAR(100)  NOT NULL,`ident_to_index` VARCHAR(100)  NOT NULL,`serverid` VARCHAR(64),`priority` INTEGER  NOT NULL,`time_entered` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, index_val varchar(325), dist_action integer NOT NULL DEFAULT 1, PRIMARY KEY (`id`));

CREATE INDEX dist_reindex_index1 USING BTREE on dist_reindex_journal (inode_to_index (100));
CREATE INDEX dist_reindex_index2 USING BTREE on dist_reindex_journal (dist_action);
CREATE INDEX dist_reindex_index3 USING BTREE on dist_reindex_journal (serverid);
CREATE INDEX dist_reindex_index4 USING BTREE on dist_reindex_journal (ident_to_index,serverid);
CREATE INDEX dist_reindex_index  USING BTREE on dist_reindex_journal (serverid,dist_action);
CREATE INDEX dist_reindex_index5 USING BTREE ON dist_reindex_journal (priority, time_entered);
CREATE INDEX dist_reindex_index6 USING BTREE ON dist_reindex_journal (priority);

CREATE TABLE `quartz_log` (`id` BIGINT  NOT NULL AUTO_INCREMENT,`JOB_NAME` VARCHAR(255)  NOT NULL,`serverid` VARCHAR(64) ,`time_started` DATETIME  NOT NULL, PRIMARY KEY (`id`));


ALTER TABLE cms_role ADD UNIQUE (role_key);

alter table cms_role add constraint fkcms_role_parent foreign key (parent) references cms_role (id) ON DELETE CASCADE;


ALTER TABLE cms_layout ADD UNIQUE (layout_name);
ALTER TABLE portlet ADD UNIQUE (portletid);
ALTER TABLE cms_layouts_portlets ADD UNIQUE (portlet_id, layout_id);
alter table cms_layouts_portlets add constraint fkcms_layouts_portlets foreign key (layout_id) references cms_layout(id);

ALTER TABLE users_cms_roles ADD UNIQUE (role_id, user_id);
alter table users_cms_roles add constraint fkusers_cms_roles1 foreign key (role_id) references cms_role (id);
alter table users_cms_roles add constraint fkusers_cms_roles2 foreign key (user_id) references user_ (userid);

ALTER TABLE layouts_cms_roles ADD UNIQUE (role_id, layout_id);
alter table layouts_cms_roles add constraint fklayouts_cms_roles1 foreign key (role_id) references cms_role (id);
alter table layouts_cms_roles add constraint fklayouts_cms_roles2 foreign key (layout_id) references cms_layout (id);

ALTER TABLE containers add constraint containers_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE template add constraint template_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE htmlpage add constraint htmlpage_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE file_asset add constraint file_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE contentlet add constraint content_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE links add constraint links_identifier_fk foreign key (identifier) references identifier(id);


create table dist_reindex_lock (dummy int);
create table dist_lock (dummy int);

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

alter table category modify column category_velocity_var_name varchar(255) not null;

alter table import_audit add column warnings text,
	add column errors text,
	add column results text,
	add column messages text;

alter table structure modify host varchar(100) default 'SYSTEM_HOST' not null;
alter table structure modify folder varchar(100) default 'SYSTEM_FOLDER' not null;
alter table structure add constraint fk_structure_folder foreign key (folder) references folder(inode);
alter table structure modify column velocity_var_name varchar(255) not null;
alter table structure add constraint unique_struct_vel_var_name unique (velocity_var_name);

DROP PROCEDURE IF EXISTS load_records_to_index;
CREATE PROCEDURE load_records_to_index(IN server_id VARCHAR(100), IN records_to_fetch INT)
BEGIN
DECLARE v_id BIGINT;
DECLARE v_inode_to_index VARCHAR(100);
DECLARE v_ident_to_index VARCHAR(100);
DECLARE v_serverid VARCHAR(64);
DECLARE v_priority INT;
DECLARE v_time_entered TIMESTAMP;
DECLARE v_index_val VARCHAR(325);
DECLARE v_dist_action INT;
DECLARE cursor_end BOOL DEFAULT FALSE;
DECLARE cur1 CURSOR FOR SELECT * FROM dist_reindex_journal WHERE serverid IS NULL or serverid='' ORDER BY priority ASC LIMIT 10 FOR UPDATE;
DECLARE CONTINUE HANDLER FOR NOT FOUND SET cursor_end:=TRUE;

DROP TEMPORARY TABLE IF EXISTS tmp_records_reindex;
CREATE TEMPORARY TABLE tmp_records_reindex (
  id BIGINT PRIMARY KEY,
  inode_to_index varchar(36),
  ident_to_index varchar(36),
  dist_action INT,
  priority INT
) ENGINE=MEMORY;

OPEN cur1;
WHILE (NOT cursor_end) DO
  FETCH cur1 INTO v_id,v_inode_to_index,v_ident_to_index,v_serverid,v_priority,v_time_entered,v_index_val,v_dist_action;
  IF (NOT cursor_end) THEN
    UPDATE dist_reindex_journal SET serverid=server_id WHERE id=v_id;
    INSERT INTO tmp_records_reindex VALUES (v_id, v_inode_to_index, v_ident_to_index, v_dist_action, v_priority);
  END IF;
END WHILE;
CLOSE cur1;

SELECT * FROM tmp_records_reindex;

END;
#
DROP TRIGGER IF EXISTS check_parent_path_when_update;
CREATE TRIGGER check_parent_path_when_update  BEFORE UPDATE
on identifier
FOR EACH ROW
BEGIN
DECLARE idCount INT;
DECLARE canUpdate boolean default false;
 IF @disable_trigger IS NULL THEN
   select count(id)into idCount from identifier where asset_type='folder' and CONCAT(parent_path,asset_name,'/')= NEW.parent_path and host_inode = NEW.host_inode and id <> NEW.id;
   IF(idCount > 0 OR NEW.parent_path = '/' OR NEW.parent_path = '/System folder') THEN
     SET canUpdate := TRUE;
   END IF;
   IF(canUpdate = FALSE) THEN
     delete from Cannot_update_for_this_path_does_not_exist_for_the_given_host;
   END IF;
 END IF;
END
#
DROP TRIGGER IF EXISTS check_parent_path_when_insert;
CREATE TRIGGER check_parent_path_when_insert  BEFORE INSERT
on identifier
FOR EACH ROW
BEGIN
DECLARE idCount INT;
DECLARE canInsert boolean default false;
 select count(id)into idCount from identifier where asset_type='folder' and CONCAT(parent_path,asset_name,'/')= NEW.parent_path and host_inode = NEW.host_inode and id <> NEW.id;
 IF(idCount > 0 OR NEW.parent_path = '/' OR NEW.parent_path = '/System folder') THEN
   SET canInsert := TRUE;
 END IF;
 IF(canInsert = FALSE) THEN
  delete from Cannot_insert_for_this_path_does_not_exist_for_the_given_host;
 END IF;
END
#
DROP PROCEDURE IF EXISTS checkVersions;
CREATE PROCEDURE checkVersions(IN ident VARCHAR(100),IN tableName VARCHAR(20), OUT versionsCount INT)
BEGIN
  SET versionsCount := 0;
  IF(tableName = 'htmlpage') THEN
    select count(inode) into versionsCount from htmlpage where identifier = ident;
  END IF;
  IF(tableName = 'file_asset') THEN
    select count(inode) into versionsCount from file_asset where identifier = ident;
  END IF;
  IF(tableName = 'links') THEN
    select count(inode) into versionsCount from links where identifier = ident;
  END IF;
  IF(tableName = 'containers') THEN
    select count(inode) into versionsCount from containers where identifier = ident;
  END IF;
  IF(tableName = 'template') THEN
    select count(inode) into versionsCount from template where identifier = ident;
  END IF;
  IF(tableName = 'contentlet') THEN
    select count(inode) into versionsCount from contentlet where identifier = ident;
  END IF;
  IF(tableName = 'folder') THEN
    select count(inode) into versionsCount from folder where identifier = ident;
  END IF;
END
#

DROP TRIGGER IF EXISTS check_htmlpage_versions;
CREATE TRIGGER check_htmlpage_versions AFTER DELETE
on htmlpage
FOR EACH ROW
BEGIN
DECLARE tableName VARCHAR(20);
DECLARE count INT;
SET tableName = 'htmlpage';
CALL checkVersions(OLD.identifier,tableName,count);
IF(count = 0)THEN
 delete from identifier where id = OLD.identifier;
END IF;
END
#

DROP TRIGGER IF EXISTS check_file_versions;
CREATE TRIGGER check_file_versions AFTER DELETE
on file_asset
FOR EACH ROW
BEGIN
DECLARE tableName VARCHAR(20);
DECLARE count INT;
SET tableName = 'file_asset';
CALL checkVersions(OLD.identifier,tableName,count);
IF(count = 0)THEN
 delete from identifier where id = OLD.identifier;
END IF;
END
#
DROP TRIGGER IF EXISTS check_links_versions;
CREATE TRIGGER check_links_versions AFTER DELETE
on links
FOR EACH ROW
BEGIN
DECLARE tableName VARCHAR(20);
DECLARE count INT;
SET tableName = 'links';
CALL checkVersions(OLD.identifier,tableName,count);
IF(count = 0)THEN
 delete from identifier where id = OLD.identifier;
END IF;
END
#
DROP TRIGGER IF EXISTS check_container_versions;
CREATE TRIGGER check_container_versions AFTER DELETE
on containers
FOR EACH ROW
BEGIN
DECLARE tableName VARCHAR(20);
DECLARE count INT;
SET tableName = 'containers';
CALL checkVersions(OLD.identifier,tableName,count);
IF(count = 0)THEN
 delete from identifier where id = OLD.identifier;
END IF;
END
#
DROP TRIGGER IF EXISTS check_template_versions;
CREATE TRIGGER check_template_versions AFTER DELETE
on template
FOR EACH ROW
BEGIN
DECLARE tableName VARCHAR(20);
DECLARE count INT;
SET tableName = 'template';
CALL checkVersions(OLD.identifier,tableName,count);
IF(count = 0)THEN
 delete from identifier where id = OLD.identifier;
END IF;
END
#
DROP TRIGGER IF EXISTS check_content_versions;
CREATE TRIGGER check_content_versions AFTER DELETE
on contentlet
FOR EACH ROW
BEGIN
DECLARE tableName VARCHAR(20);
DECLARE count INT;
SET tableName = 'contentlet';
CALL checkVersions(OLD.identifier,tableName,count);
IF(count = 0)THEN
 delete from identifier where id = OLD.identifier;
END IF;
END
#
alter table structure add constraint fk_structure_host foreign key (host) references identifier(id);

create index idx_template3 on template (title);

CREATE INDEX idx_contentlet_4 ON contentlet (structure_inode);

ALTER TABLE Folder add constraint folder_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE containers add constraint structure_fk foreign key (structure_inode) references structure(inode);
ALTER TABLE htmlpage add constraint template_id_fk foreign key (template_id) references identifier(id);

DROP TRIGGER IF EXISTS check_templateId_when_insert;
CREATE TRIGGER check_templateId_when_insert BEFORE INSERT
on htmlpage
FOR EACH ROW
BEGIN
DECLARE identCount INT;
select count(id) into identCount from identifier where id = NEW.template_id and asset_type='template';
IF(identCount = 0) THEN
delete from Template_Id_should_be_the_identifier_of_a_template;
END IF;
END
#
DROP TRIGGER IF EXISTS check_templateId_when_update;
CREATE TRIGGER check_templateId_when_update  BEFORE UPDATE
on htmlpage
FOR EACH ROW
BEGIN
DECLARE identCount INT;
select count(id)into identCount from identifier where id = NEW.template_id and asset_type='template';
IF(identCount = 0) THEN
delete from Template_Id_should_be_the_identifier_of_a_template;
END IF;
END
#
DROP TRIGGER IF EXISTS folder_identifier_check;
CREATE TRIGGER folder_identifier_check BEFORE DELETE
on folder
FOR EACH ROW
BEGIN
DECLARE tableName VARCHAR(20);
DECLARE count INT;
SET tableName = 'folder';
CALL checkVersions(OLD.identifier,tableName,count);
IF(count = 0)THEN
delete from identifier where id = OLD.identifier;
END IF;
END
#
alter table contentlet add constraint fk_user_contentlet foreign key (mod_user) references user_(userid);
alter table htmlpage add constraint fk_user_htmlpage foreign key (mod_user) references user_(userid);
alter table containers add constraint fk_user_containers foreign key (mod_user) references user_(userid);
alter table template add constraint fk_user_template foreign key (mod_user) references user_(userid);
alter table file_asset add constraint fk_user_file_asset foreign key (mod_user) references user_(userid);
alter table links add constraint fk_user_links foreign key (mod_user) references user_(userid);

create index idx_template_id on template_containers(template_id);
alter table template_containers add constraint FK_template_id foreign key (template_id) references identifier(id);
alter table template_containers add constraint FK_container_id foreign key (container_id) references identifier(id);

DROP TRIGGER IF EXISTS check_child_assets;
CREATE TRIGGER check_child_assets BEFORE DELETE
ON IDENTIFIER
FOR EACH ROW
BEGIN
  DECLARE pathCount INT;
    IF(OLD.asset_type ='folder') THEN
      select count(*) into pathCount from identifier where parent_path = CONCAT(OLD.parent_path,OLD.asset_name,'/') and host_inode = OLD.host_inode;
    END IF;
    IF(OLD.asset_type ='contentlet') THEN
	 select count(*) into pathCount from identifier where host_inode = OLD.id;
    END IF;
 IF(pathCount > 0) THEN
   delete from Cannot_delete_as_this_path_has_children;
 END IF;
END
#
DROP PROCEDURE IF EXISTS renameFolderChildren;
CREATE PROCEDURE renameFolderChildren(IN old_path varchar(100),IN new_path varchar(100),IN hostInode varchar(100))
BEGIN
 DECLARE new_folder_path varchar(100);
 DECLARE old_folder_path varchar(100);
 DECLARE assetName varchar(100);
 DECLARE no_more_rows boolean;
 DECLARE cur1 CURSOR FOR select asset_name from identifier where asset_type='folder' and parent_path = new_path and host_inode = hostInode;
 DECLARE CONTINUE HANDLER FOR NOT FOUND
 SET no_more_rows := TRUE;
 SET max_sp_recursion_depth=255;
 SET @disable_trigger = 1;
 UPDATE identifier SET  parent_path  = new_path where parent_path = old_path and host_inode = hostInode;
 SET @disable_trigger = NULL;
 OPEN cur1;
 cur1_loop:LOOP
 FETCH cur1 INTO assetName;
 IF no_more_rows THEN
   LEAVE cur1_loop;
 END IF;
 select CONCAT(new_path,assetName,'/')INTO new_folder_path;
 select CONCAT(old_path,assetName,'/')INTO old_folder_path;
 CALL renameFolderChildren(old_folder_path,new_folder_path,hostInode);
END LOOP;
CLOSE cur1;
END
#
DROP TRIGGER IF EXISTS rename_folder_assets_trigger;
CREATE TRIGGER rename_folder_assets_trigger AFTER UPDATE
on Folder
FOR EACH ROW
BEGIN
DECLARE old_parent_path varchar(100);
DECLARE old_path varchar(100);
DECLARE new_path varchar(100);
DECLARE old_name varchar(100);
DECLARE hostInode varchar(100);
IF @disable_trigger <> 1 THEN
	select asset_name,parent_path,host_inode INTO old_name,old_parent_path,hostInode from identifier where id = NEW.identifier;
	SELECT CONCAT(old_parent_path,old_name,'/')INTO old_path;
	SELECT CONCAT(old_parent_path,NEW.name,'/')INTO new_path;
	SET @disable_trigger = 1;
	UPDATE identifier SET asset_name = NEW.name where id = NEW.identifier;
	SET @disable_trigger = NULL;
	CALL renameFolderChildren(old_path,new_path,hostInode);
END IF;
END
#

DROP FUNCTION IF EXISTS dotFolderPath;
CREATE FUNCTION dotFolderPath (parent_path char(255), asset_name char(255)) RETURNS char(255)
BEGIN
IF (parent_path='/System folder') THEN
  RETURN '/';
ELSE
  RETURN CONCAT(parent_path,asset_name,'/');
END IF;
END
#
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
alter table workflowtask_files add constraint FK_task_file_inode foreign key (file_inode) references file_asset(inode);
alter table workflow_comment add constraint workflowtask_id_comment_FK foreign key (workflowtask_id) references workflow_task(id);
alter table workflow_history add constraint workflowtask_id_history_FK foreign key (workflowtask_id) references workflow_task(id);

alter table contentlet add constraint fk_contentlet_lang foreign key (language_id) references language(id);

create table workflow_scheme(
    id varchar(36) primary key,
    name varchar(255) not null,
    description varchar(255),
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
    resolved boolean default false

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
SET sql_mode='ANSI_QUOTES';
create table workflow_action_class_pars(
    id varchar(36) primary key,
    workflow_action_class_id char(36) references workflow_action_class(id),
    "key" varchar(255) not null,
    value text
);
create index workflow_idx_action_class_param_action on
    workflow_action_class_pars(workflow_action_class_id);


create table workflow_scheme_x_structure(
    id varchar(36) primary key,
    scheme_id varchar(36) not null references workflow_scheme(id),
    structure_id varchar(36) not null references structure(inode)
);
create index workflow_idx_scheme_structure_1 on
    workflow_scheme_x_structure(structure_id);

create unique index workflow_idx_scheme_structure_2 on
    workflow_scheme_x_structure(structure_id);

alter table contentlet_version_info add constraint FK_con_ver_lockedby foreign key (locked_by) references user_(userid);
alter table container_version_info  add constraint FK_tainer_ver_info_lockedby  foreign key (locked_by) references user_(userid);
alter table template_version_info   add constraint FK_temp_ver_info_lockedby   foreign key (locked_by) references user_(userid);
alter table htmlpage_version_info   add constraint FK_page_ver_info_lockedby   foreign key (locked_by) references user_(userid);
alter table fileasset_version_info  add constraint FK_fil_ver_info_lockedby  foreign key (locked_by) references user_(userid);
alter table link_version_info       add constraint FK_link_ver_info_lockedby       foreign key (locked_by) references user_(userid);

ALTER TABLE tag ALTER COLUMN host_id set default 'SYSTEM_HOST';
alter table tag add constraint tag_tagname_host unique (tagname, host_id);
alter table tag_inode add constraint fk_tag_inode_tagid foreign key (tag_id) references tag (tag_id);

-- ****** Indicies Data Storage *******
create table indicies (
  index_name varchar(30) primary key,
  index_type varchar(16) not null unique
);
-- ****** Log Console Table *******
  CREATE TABLE log_mapper (
    enabled   	 bigint(1,0) not null,
    log_name 	 varchar(30) not null,
    description  varchar(50) not null,
    primary key (log_name)
  );
  