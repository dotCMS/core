DROP TABLE file_asset;
CREATE TABLE file_asset (
        inode bigint NOT NULL PRIMARY KEY REFERENCES inode 
              ON DELETE CASCADE, 
  file_name varchar(255) default NULL,
  size int default 0,
  mime_type varchar(255),
  live boolean default false,
  working boolean default false,
  deleted boolean default false,
  locked boolean default false,
  show_on_menu boolean default false,
  title varchar(255) NOT NULL,
  friendly_name varchar(255) NOT NULL,
  mod_date timestamp DEFAULT 'now()' NOT NULL,
  mod_user varchar(100),
  sort_order int default 0
) ;
 
