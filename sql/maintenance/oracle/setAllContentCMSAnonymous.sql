
CREATE GLOBAL TEMPORARY TABLE TEMP_PERM
(
"ID" NUMBER(19),
"INODE_ID" NUMBER(19),
"ROLEID" NUMBER(19),
"PERMISSION" NUMBER(10)
) ON COMMIT DELETE ROWS;




insert into TEMP_PERM (inode_id, roleid, permission)
(
select DISTINCT  inode.identifier,
(select roleid from role_ r where r.name like 'CMS Anonymous' and
companyid = 'dotcms.org'), 1
from contentlet, inode
where contentlet.inode = inode.inode and inode.identifier not in (
select inode_id from permission where permission = 1 and roleid =
(select roleid from role_ r where r.name like 'CMS Anonymous' and
companyid = 'dotcms.org'))
);

insert into TEMP_PERM (inode_id, roleid, permission)
(
select DISTINCT structure.inode,
(select roleid from role_ r where r.name like 'CMS Anonymous' and
companyid = 'dotcms.org'), 1
from structure
where structure.inode not in (
select inode_id from permission where permission = 1 and roleid =
(select roleid from role_ r where r.name like 'CMS Anonymous' and
companyid = 'dotcms.org'))
);


insert into TEMP_PERM (inode_id, roleid, permission)
(
select distinct category.inode,
(select roleid from role_ r where r.name like 'CMS Anonymous' and
companyid = 'dotcms.org'), 1
from category
where category.inode  not in (
select inode_id from permission where permission = 1 and roleid =
(select roleid from role_ r where r.name like 'CMS Anonymous' and
companyid = 'dotcms.org'))
);

UPDATE TEMP_PERM SET ID=PERMISSION_SEQ.NEXTVAL;

INSERT INTO PERMISSION (SELECT * FROM TEMP_PERM);

DROP TABLE TEMP_PERM;
