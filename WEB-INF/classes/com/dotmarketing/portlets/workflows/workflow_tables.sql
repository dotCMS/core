create table workflow_task (
   inode bigint,
   creation_date timestamp not null, 
   mod_date timestamp not null,
   due_date timestamp,
   created_by varchar(50), -- liferay user id
   assigned_to varchar(50), -- liferay user id
   belongs_to int,  -- liferay group id
   title varchar(255) not null,
   description text,
   status varchar(50) not null,
   webasset bigint not null
);

alter table workflow_task add constraint wt_pk primary key (inode);
alter table workflow_task add constraint wt_fk_inode foreign key (inode) references inode (inode);

create table workflow_comment (
   inode bigint not null,
   posted_by varchar(50) not null,
   creation_date timestamp not null,
   comment text not null
);

alter table workflow_comment add constraint wc_pk primary key (inode);
alter table workflow_comment add constraint wc_fk_inode foreign key (inode) references inode (inode);

create table workflow_history (
   inode bigint not null,
   made_by varchar (50), -- liferay user id
   change_desc text,
   creation_date timestamp
);
alter table workflow_history add constraint wh_pk primary key (inode);
alter table workflow_history add constraint wh_fk_inode foreign key (inode) references inode (inode);


