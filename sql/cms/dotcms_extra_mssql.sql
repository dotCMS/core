--mssql
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

alter table chain_state add constraint fk_state_chain foreign key (chain_id) references chain(id);
alter table chain_state add constraint fk_state_code foreign key (link_code_id) references chain_link_code(id);
alter table chain_state_parameter add constraint fk_parameter_state foreign key (chain_state_id) references chain_state(id);

alter table permission add constraint permission_role_fk foreign key (roleid) references cms_role(id);

alter table contentlet add constraint FK_structure_inode foreign key (structure_inode) references structure(inode);

ALTER TABLE structure ALTER COLUMN fixed tinyint NOT NULL;
alter table structure add CONSTRAINT [DF_structure_fixed]  DEFAULT ((0)) for fixed;

ALTER TABLE field ALTER COLUMN fixed tinyint NOT NULL;
ALTER TABLE field ALTER COLUMN read_only tinyint  NOT NULL;
ALTER TABLE campaign ALTER COLUMN active tinyint NOT NULL;
alter table field add CONSTRAINT [DF_field_fixed]  DEFAULT ((0)) for fixed;
alter table field add CONSTRAINT [DF_field_read_only]  DEFAULT ((0)) for read_only;

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('dotcms.org.default', 'default', GetDate(), 'password', '0', '0', '', '', '', '1', '19700101', 'default@dotcms.org', '01', '0', '0', 'Welcome!', '', GetDate(), 0, '0', '1');

create index addres_userid_index on address(userid);
create index tag_user_id_index on tag(user_id);
create index tag_inode_tagid on tag_inode(tag_id);
create index tag_inode_inode on tag_inode(inode);
CREATE TABLE dist_journal
	   (
       id bigint NOT NULL IDENTITY (1, 1),
       object_to_index VARCHAR(1024) NOT NULL,
       serverid varchar(64) NOT NULL,
       journal_type int NOT NULL,
       time_entered datetime NOT NULL
       ) ;

ALTER TABLE dist_journal ADD CONSTRAINT
       PK_dist_journal PRIMARY KEY CLUSTERED
       (
       id
       );


ALTER TABLE dist_journal ADD CONSTRAINT
       IX_dist_journal UNIQUE NONCLUSTERED
       (
       object_to_index,
       serverid,
       journal_type
       );
CREATE TABLE dist_process ( id bigint NOT NULL IDENTITY (1, 1), object_to_index varchar(1024) NOT NULL, serverid varchar(64) NOT NULL, journal_type int NOT NULL, time_entered datetime NOT NULL ) ;
ALTER TABLE dist_process ADD CONSTRAINT PK_dist_process PRIMARY KEY CLUSTERED ( id);

create table plugin_property (
   plugin_id varchar(255) not null,
   propkey varchar(255) not null,
   original_value varchar(255) not null,
   current_value varchar(255) not null,
   primary key (plugin_id, propkey)
);

alter table plugin_property add constraint fk_plugin_plugin_property foreign key (plugin_id) references plugin(id);

CREATE TABLE dist_reindex_journal ( id bigint NOT NULL IDENTITY (1, 1), inode_to_index varchar(100) NOT NULL,ident_to_index varchar(100) NOT NULL, serverid varchar(64), priority int NOT NULL, time_entered datetime DEFAULT getDate(), index_val varchar(325),dist_action integer NOT NULL DEFAULT 1);

CREATE INDEX dist_reindex_index1 on dist_reindex_journal (inode_to_index);
CREATE INDEX dist_reindex_index2 on dist_reindex_journal (dist_action);
CREATE INDEX dist_reindex_index3 on dist_reindex_journal (serverid);
CREATE INDEX dist_reindex_index4 on dist_reindex_journal (ident_to_index,serverid);
CREATE INDEX dist_reindex_index on dist_reindex_journal (serverid,dist_action);
CREATE INDEX dist_reindex_index5 ON dist_reindex_journal (priority, time_entered);
CREATE INDEX dist_reindex_index6 ON dist_reindex_journal (priority);

ALTER TABLE dist_reindex_journal ADD CONSTRAINT PK_dist_reindex_journal PRIMARY KEY CLUSTERED ( id);

CREATE TABLE quartz_log ( id bigint NOT NULL IDENTITY (1, 1), JOB_NAME varchar(255) NOT NULL, serverid varchar(64) , time_started datetime NOT NULL, primary key (id));

CREATE TRIGGER check_role_key_uniqueness
ON cms_role
FOR INSERT, UPDATE
AS
DECLARE @c varchar(100)
SELECT @c = count(*)
FROM cms_role e INNER JOIN inserted i ON i.role_key = e.role_key WHERE i.role_key IS NOT NULL AND i.id <> e.id
IF (@c > 0)
BEGIN
   RAISERROR ('Duplicated role key.', 16, 1)
   ROLLBACK TRANSACTION
END;

CREATE TRIGGER check_identifier_host_inode
ON identifier
FOR INSERT, UPDATE AS
DECLARE @assetType varchar(10)
DECLARE @hostInode varchar(50)
DECLARE cur_Inserted cursor
LOCAL FAST_FORWARD for
 Select asset_type, host_inode
 from inserted
 for Read Only
open cur_Inserted
fetch next from cur_Inserted into @assetType,@hostInode
while @@FETCH_STATUS <> -1
BEGIN
 IF(@assetType <> 'content' AND (@hostInode is null OR @hostInode = ''))
 BEGIN
	RAISERROR (N'Cannot insert/update a null or empty host inode for this kind of identifier', 10, 1)
	ROLLBACK WORK
 END
fetch next from cur_Inserted into @assetType,@hostInode
END;

ALTER TABLE cms_role ADD CONSTRAINT IX_cms_role2 UNIQUE NONCLUSTERED (db_fqn);
alter table cms_role add constraint fkcms_role_parent foreign key (parent) references cms_role;

ALTER TABLE users_cms_roles ADD CONSTRAINT IX_cms_role UNIQUE NONCLUSTERED (role_id, user_id);
alter table users_cms_roles add constraint fkusers_cms_roles1 foreign key (role_id) references cms_role;
alter table users_cms_roles add constraint fkusers_cms_roles2 foreign key (user_id) references user_;

ALTER TABLE cms_layout ADD CONSTRAINT IX_cms_layout UNIQUE NONCLUSTERED (layout_name);

ALTER TABLE portlet ADD CONSTRAINT IX_portletid UNIQUE NONCLUSTERED (portletid);

ALTER TABLE cms_layouts_portlets ADD CONSTRAINT IX_cms_layouts_portlets UNIQUE NONCLUSTERED (portlet_id, layout_id);
alter table cms_layouts_portlets add constraint fklcms_layouts_portlets foreign key (layout_id) references cms_layout;

ALTER TABLE layouts_cms_roles ADD CONSTRAINT IX_layouts_cms_roles UNIQUE NONCLUSTERED (role_id, layout_id);
alter table layouts_cms_roles add constraint fklayouts_cms_roles1 foreign key (role_id) references cms_role;
alter table layouts_cms_roles add constraint fklayouts_cms_roles2 foreign key (layout_id) references cms_layout;

ALTER TABLE containers add constraint containers_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE template add constraint template_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE htmlpage add constraint htmlpage_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE file_asset add constraint file_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE contentlet add constraint content_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE links add constraint links_identifier_fk foreign key (identifier) references identifier(id);


create table dist_reindex_lock (dummy int);
create table dist_lock (dummy int);
insert into dist_reindex_lock (dummy) values (1);
insert into dist_lock (dummy) values (1);

create table import_audit (
	id bigint not null,
	start_date datetime,
	userid varchar(255),
	filename varchar(512),
	status int,
	last_inode varchar(100),
	records_to_import bigint,
	serverid varchar(255),
	primary key (id)
	);

alter table category alter column category_velocity_var_name varchar(255) not null;

alter table import_audit add warnings text,
	errors text,
	results text,
	messages text;

alter table structure add CONSTRAINT [DF_structure_host] DEFAULT 'SYSTEM_HOST' for host;
alter table structure add CONSTRAINT [DF_structure_folder] DEFAULT 'SYSTEM_FOLDER' for folder;
alter table structure add CONSTRAINT [CK_structure_host] CHECK(host <> '' AND host IS NOT NULL)
alter table structure add constraint fk_structure_folder foreign key (folder) references folder(inode);
alter table structure alter column velocity_var_name varchar(255) not null;
alter table structure add constraint unique_struct_vel_var_name unique (velocity_var_name);

CREATE TRIGGER structure_host_folder_trigger
ON structure
FOR INSERT, UPDATE AS
DECLARE @newFolder varchar(100)
DECLARE @newHost varchar(100)
DECLARE @folderInode varchar(100)
DECLARE @hostInode varchar(100)
DECLARE cur_Inserted cursor
LOCAL FAST_FORWARD for
 Select folder, host
 from inserted
 for Read Only
open cur_Inserted
fetch next from cur_Inserted into @newFolder,@newHost
while @@FETCH_STATUS <> -1
BEGIN
   IF (@newHost <> 'SYSTEM_HOST' AND @newFolder <> 'SYSTEM_FOLDER')
   BEGIN
	  SELECT @hostInode = identifier.host_inode, @folderInode = folder.inode from folder,identifier where folder.identifier = identifier.id and folder.inode = @newFolder
      IF (@folderInode IS NULL OR @folderInode = '' OR @newHost <> @hostInode)
      BEGIN
	    RAISERROR (N'Cannot assign host/folder to structure, folder does not belong to given host', 10, 1)
	    ROLLBACK WORK
	  END
  END
fetch next from cur_Inserted into @newFolder,@newHost
END;

CREATE PROCEDURE load_records_to_index(@server_id VARCHAR(100), @records_to_fetch INT)
AS
BEGIN
WITH cte AS (
  SELECT TOP(@records_to_fetch) *
  FROM dist_reindex_journal WITH (ROWLOCK, READPAST, UPDLOCK)
  WHERE serverid IS NULL
  ORDER BY priority ASC)
UPDATE cte
  SET serverid=@server_id
OUTPUT
  INSERTED.*
END;

CREATE Trigger check_file_versions
ON file_asset
FOR DELETE AS
 DECLARE @totalCount int
 DECLARE @identifier varchar(100)
 DECLARE file_cur_Deleted cursor LOCAL FAST_FORWARD for
 Select identifier
  from deleted
  for Read Only
 open file_cur_Deleted
 fetch next from file_cur_Deleted into @identifier
 while @@FETCH_STATUS <> -1
 BEGIN
 select @totalCount = count(*) from file_asset where identifier = @identifier
 IF (@totalCount = 0)
  BEGIN
    DELETE from identifier where id = @identifier
  END
fetch next from file_cur_Deleted into @identifier
END;
CREATE Trigger check_content_versions
ON contentlet
FOR DELETE AS
 DECLARE @totalCount int
 DECLARE @identifier varchar(100)
 DECLARE content_cur_Deleted cursor LOCAL FAST_FORWARD for
 Select identifier
  from deleted
  for Read Only
 open content_cur_Deleted
 fetch next from content_cur_Deleted into @identifier
 while @@FETCH_STATUS <> -1
 BEGIN
 select @totalCount = count(*) from contentlet where identifier = @identifier
 IF (@totalCount = 0)
  BEGIN
   DELETE from identifier where id = @identifier
  END
fetch next from content_cur_Deleted into @identifier
END;

CREATE Trigger check_link_versions
ON links
FOR DELETE AS
 DECLARE @totalCount int
 DECLARE @identifier varchar(100)
 DECLARE link_cur_Deleted cursor LOCAL FAST_FORWARD for
 Select identifier
  from deleted
  for Read Only
 open link_cur_Deleted
 fetch next from link_cur_Deleted into @identifier
 while @@FETCH_STATUS <> -1
 BEGIN
 select @totalCount = count(*) from links where identifier = @identifier
 IF (@totalCount = 0)
  BEGIN
   DELETE from identifier where id = @identifier
  END
fetch next from link_cur_Deleted into @identifier
END;

CREATE Trigger check_container_versions
ON containers
FOR DELETE AS
 DECLARE @totalCount int
 DECLARE @identifier varchar(100)
 DECLARE container_cur_Deleted cursor LOCAL FAST_FORWARD for
 Select identifier
  from deleted
  for Read Only
 open container_cur_Deleted
 fetch next from container_cur_Deleted into @identifier
 while @@FETCH_STATUS <> -1
 BEGIN
 select @totalCount = count(*) from containers where identifier = @identifier
 IF (@totalCount = 0)
  BEGIN
   DELETE from identifier where id = @identifier
  END
fetch next from container_cur_Deleted into @identifier
END;


CREATE Trigger check_template_versions
ON template
FOR DELETE AS
 DECLARE @totalCount int
 DECLARE @identifier varchar(100)
 DECLARE template_cur_Deleted cursor LOCAL FAST_FORWARD for
 Select identifier
  from deleted
  for Read Only
 open template_cur_Deleted
 fetch next from template_cur_Deleted into @identifier
 while @@FETCH_STATUS <> -1
 BEGIN
 select @totalCount = count(*) from template where identifier = @identifier
 IF (@totalCount = 0)
  BEGIN
   DELETE from identifier where id = @identifier
  END
fetch next from template_cur_Deleted into @identifier
END;

CREATE Trigger check_htmlpage_versions
ON htmlpage
FOR DELETE AS
 DECLARE @totalCount int
 DECLARE @identifier varchar(100)
 DECLARE htmlpage_cur_Deleted cursor LOCAL FAST_FORWARD for
 Select identifier
  from deleted
  for Read Only
 open htmlpage_cur_Deleted
 fetch next from htmlpage_cur_Deleted into @identifier
 while @@FETCH_STATUS <> -1
 BEGIN
 select @totalCount = count(*) from htmlpage where identifier = @identifier
 IF (@totalCount = 0)
  BEGIN
   DELETE from identifier where id = @identifier
  END
fetch next from htmlpage_cur_Deleted into @identifier
END;

CREATE Trigger check_identifier_parent_path
 ON identifier
 FOR INSERT,UPDATE AS
 DECLARE @folderId varchar(100)
 DECLARE @id varchar(100)
 DECLARE @assetType varchar(100)
 DECLARE @parentPath varchar(100)
 DECLARE @hostInode varchar(100)
 DECLARE cur_Inserted cursor LOCAL FAST_FORWARD for
 Select id,asset_type,parent_path,host_inode
  from inserted
  for Read Only
 open cur_Inserted
 fetch next from cur_Inserted into @id,@assetType,@parentPath,@hostInode
 while @@FETCH_STATUS <> -1
 BEGIN
  IF(@parentPath <>'/' AND @parentPath <>'/System folder')
  BEGIN
    select @folderId = id from identifier where asset_type='folder' and host_inode = @hostInode and parent_path+asset_name+'/' = @parentPath and id <>@id
    IF (@folderId IS NULL)
     BEGIN
       RAISERROR (N'Cannot insert/update for this path does not exist for the given host', 10, 1)
       ROLLBACK WORK
     END
  END
 fetch next from cur_Inserted into @id,@assetType,@parentPath,@hostInode
END;

alter table structure add constraint fk_structure_host foreign key (host) references identifier(id);

create index idx_template3 on template (title);

CREATE INDEX idx_contentlet_4 ON contentlet (structure_inode);

ALTER TABLE Folder add constraint folder_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE containers add constraint structure_fk foreign key (structure_inode) references structure(inode);
ALTER TABLE htmlpage add constraint template_id_fk foreign key (template_id) references identifier(id);

CREATE Trigger check_template_identifier
ON htmlpage
FOR INSERT,UPDATE AS
DECLARE @templateId varchar(100)
DECLARE @tempIdentifier varchar(100)
DECLARE htmlpage_cur_Inserted cursor LOCAL FAST_FORWARD for
 Select template_id
 from inserted
 for Read Only
open htmlpage_cur_Inserted
fetch next from htmlpage_cur_Inserted into @templateId
while @@FETCH_STATUS <> -1
BEGIN
 select @tempIdentifier = id from identifier where asset_type='template' and id = @templateId
 IF (@tempIdentifier IS NULL)
 BEGIN
   RAISERROR (N'Template Id should be the identifier of a template', 10, 1)
   ROLLBACK WORK
 END
fetch next from htmlpage_cur_Inserted into @templateId
END;

CREATE Trigger folder_identifier_check
ON folder
FOR DELETE AS
DECLARE @totalCount int
DECLARE @identifier varchar(100)
DECLARE folder_cur_Deleted cursor LOCAL FAST_FORWARD for
 Select identifier
 from deleted
 for Read Only
 open folder_cur_Deleted
 fetch next from folder_cur_Deleted into @identifier
 while @@FETCH_STATUS <> -1
  BEGIN
   select @totalCount = count(*) from folder where identifier = @identifier
   IF (@totalCount = 0)
   BEGIN
     DELETE from identifier where id = @identifier
   END
   fetch next from folder_cur_Deleted into @identifier
END;

alter table contentlet add constraint fk_user_contentlet foreign key (mod_user) references user_(userid);
alter table htmlpage add constraint fk_user_htmlpage foreign key (mod_user) references user_(userid);
alter table containers add constraint fk_user_containers foreign key (mod_user) references user_(userid);
alter table template add constraint fk_user_template foreign key (mod_user) references user_(userid);
alter table file_asset add constraint fk_user_file_asset foreign key (mod_user) references user_(userid);
alter table links add constraint fk_user_links foreign key (mod_user) references user_(userid);

create index idx_template_id on template_containers(template_id);
alter table template_containers add constraint FK_template_id foreign key (template_id) references identifier(id);
alter table template_containers add constraint FK_container_id foreign key (container_id) references identifier(id);

CREATE Trigger check_child_assets
on identifier
FOR DELETE AS
DECLARE @pathCount int
DECLARE @identifier varchar(100)
DECLARE @assetType varchar(100)
DECLARE @assetName varchar(100)
DECLARE @parentPath varchar(100)
DECLARE @hostInode varchar(100)
DECLARE cur_Deleted cursor LOCAL FAST_FORWARD for
 Select id,asset_type,asset_name,parent_path,host_inode
  from deleted
  for Read Only
 open cur_Deleted
 fetch next from cur_Deleted into @identifier,@assetType,@assetName,@parentPath,@hostInode
 while @@FETCH_STATUS <> -1
 BEGIN
   IF(@assetType='folder')
   BEGIN
     select @pathCount = count(*) from identifier where parent_path= @parentPath+@assetName+'/'  and host_inode = @hostInode
   END
   IF(@assetType='contentlet')
   BEGIN
     select @pathCount = count(*) from identifier where host_inode = @identifier
   END
   IF (@pathCount > 0)
    BEGIN
     RAISERROR (N'Cannot delete as this path has children', 10, 1)
     ROLLBACK WORK
    END
fetch next from cur_Deleted into @identifier,@assetType,@assetName,@parentPath,@hostInode
END;

CREATE PROCEDURE renameFolderChildren @oldPath varchar(100),@newPath varchar(100),@hostInode varchar(100) AS
DECLARE @newFolderPath varchar(100)
DECLARE @oldFolderPath varchar(100)
DECLARE @assetName varchar(100)
   UPDATE identifier SET  parent_path  = @newPath where parent_path = @oldPath and host_inode = @hostInode
DECLARE folder_data_cursor CURSOR LOCAL FAST_FORWARD for
select asset_name from identifier where asset_type='folder' and parent_path = @newPath and host_inode = @hostInode
OPEN folder_data_cursor
FETCH NEXT FROM folder_data_cursor INTO @assetName
while @@FETCH_STATUS <> -1
BEGIN
     SET @newFolderPath = @newPath + @assetName + '/'
     SET @oldFolderPath = @oldPath + @assetName + '/'
     EXEC renameFolderChildren @oldFolderPath,@newFolderPath,@hostInode
fetch next from folder_data_cursor into @assetName
END;

CREATE Trigger rename_folder_assets_trigger
on Folder
FOR UPDATE AS
DECLARE @oldPath varchar(100)
DECLARE @newPath varchar(100)
DECLARE @newName varchar(100)
DECLARE @hostInode varchar(100)
DECLARE @ident varchar(100)
DECLARE folder_cur_Updated cursor LOCAL FAST_FORWARD for
 Select inserted.identifier,inserted.name
 from inserted join deleted on (inserted.inode=deleted.inode)
 where inserted.name<>deleted.name
 for Read Only
open folder_cur_Updated
fetch next from folder_cur_Updated into @ident,@newName
while @@FETCH_STATUS <> -1
BEGIN
  SELECT @oldPath = parent_path+asset_name+'/',@newPath = parent_path +@newName+'/',@hostInode = host_inode from identifier where id = @ident
  UPDATE identifier SET asset_name = @newName where id = @ident
  EXEC renameFolderChildren @oldPath,@newPath,@hostInode
fetch next from folder_cur_Updated into @ident,@newName
END;

CREATE FUNCTION dbo.dotFolderPath(@parent_path varchar(36), @asset_name varchar(36))
RETURNS varchar(36)
BEGIN
  IF(@parent_path='/System folder')
  BEGIN
    RETURN '/'
  END
  RETURN @parent_path+@asset_name+'/'
END;

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
    description varchar(255),
    archived tinyint default 0,
    mandatory tinyint default 0,
    default_scheme tinyint default 0,
    entry_action_id varchar(36)
);

create table workflow_step(
    id varchar(36) primary key,
    name varchar(255) not null,
    scheme_id varchar(36) references workflow_scheme(id),
    my_order int default 0,
    resolved tinyint default 0,
    escalation_enable tinyint default 0,
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
    assignable tinyint default 0,
    commentable tinyint default 0,
    requires_checkout tinyint default 0,
    icon varchar(255) default 'defaultWfIcon',
    use_role_hierarchy_assign tinyint default 0
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
    workflow_action_class_id varchar(36) not null references workflow_action_class(id),
    "key" varchar(255) not null,
    value text
);
create index workflow_idx_action_class_param_action on
    workflow_action_class_pars(workflow_action_class_id);


create table workflow_scheme_x_structure(
    id varchar(36) primary key,
    scheme_id varchar(36) references workflow_scheme(id),
    structure_id varchar(36) references structure(inode)
);
create index workflow_idx_scheme_structure_1 on
    workflow_scheme_x_structure(structure_id);

create unique index workflow_idx_scheme_structure_2 on
    workflow_scheme_x_structure(structure_id);

alter table workflow_step add constraint fk_escalation_action foreign key (escalation_action) references workflow_action(id);

alter table contentlet_version_info add constraint FK_con_ver_lockedby foreign key (locked_by) references user_(userid);
alter table container_version_info  add constraint FK_tainer_ver_info_lockedby  foreign key (locked_by) references user_(userid);
alter table template_version_info   add constraint FK_temp_ver_info_lockedby   foreign key (locked_by) references user_(userid);
alter table htmlpage_version_info   add constraint FK_page_ver_info_lockedby   foreign key (locked_by) references user_(userid);
alter table fileasset_version_info  add constraint FK_fil_ver_info_lockedby  foreign key (locked_by) references user_(userid);
alter table link_version_info       add constraint FK_link_ver_info_lockedby       foreign key (locked_by) references user_(userid);

ALTER TABLE tag add CONSTRAINT [DF_tag_host] DEFAULT 'SYSTEM_HOST' for host_id;
alter table tag add constraint tag_tagname_host unique (tagname, host_id);
alter table tag_inode add constraint fk_tag_inode_tagid foreign key (tag_id) references tag (tag_id);

drop index tag_user_id_index on tag;
alter table tag alter column user_id text;


-- ****** Indicies Data Storage *******
create table indicies (
  index_name varchar(30) primary key,
  index_type varchar(16) not null unique
);
-- ****** Log Console Table *******
  CREATE TABLE log_mapper (
    enabled   	 numeric(1,0) not null,
    log_name 	 varchar(30) not null,
    description  varchar(50) not null,
    primary key (log_name)
  );

  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-userActivity.log','Log Users action on pages, structures, documents.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-security.log','Log users login activity into dotCMS.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-adminaudit.log','Log Admin activity on dotCMS.');


create index idx_identifier_perm on identifier (asset_type,host_inode);
alter table template add theme varchar(255);