

create table relationship (
	inode bigint not null,
	parent_relation_name varchar(256),
	child_relation_name varchar(256),
	relation_type_value varchar(50),
	parent_structure_inode bigint not null,
	child_structure_inode bigint not null,
	cardinality int not null default 0,
	required bool not null default false
);

alter table relationship add constraint relationship_pk primary key (inode);

alter table relationship add constraint relationship_inode_fk foreign key (inode) references inode (inode); 

