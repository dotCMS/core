alter table permission alter column id set default nextval('permission_seq');

insert into permission (inode_id, roleid, permission)
(
select distinct inode.identifier, 
cast((select roleid from role_ r where r.name like 'CMS Anonymous' and companyid = 'dotcms.org') as bigint), 1
from contentlet, inode
where contentlet.inode = inode.inode and inode.identifier not in (
select inode_id from permission where permission = 1 and roleid = 
(select roleid from role_ r where r.name like 'CMS Anonymous' and companyid = 'dotcms.org'))
);


insert into permission (inode_id, roleid, permission)
(
select distinct structure.inode, 
cast((select roleid from role_ r where r.name like 'CMS Anonymous' and companyid = 'dotcms.org') as bigint), 1
from structure
where structure.inode  not in (
select inode_id from permission where permission = 1 and roleid = 
(select roleid from role_ r where r.name like 'CMS Anonymous' and companyid = 'dotcms.org'))
);


insert into permission (inode_id, roleid, permission)
(
select distinct category.inode, 
cast((select roleid from role_ r where r.name like 'CMS Anonymous' and companyid = 'dotcms.org') as bigint), 1
from category
where category.inode  not in (
select inode_id from permission where permission = 1 and roleid = 
(select roleid from role_ r where r.name like 'CMS Anonymous' and companyid = 'dotcms.org'))
);