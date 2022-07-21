use master; 
go 
drop database dotcms;
go
create database dotcms; 
go 
ALTER DATABASE dotcms SET READ_COMMITTED_SNAPSHOT ON; 
go 
ALTER DATABASE dotcms SET ALLOW_SNAPSHOT_ISOLATION ON; 
go 

