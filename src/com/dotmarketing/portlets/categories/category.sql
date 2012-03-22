DROP table category;
CREATE TABLE category (
        inode integer NOT NULL Primary Key REFERENCES inode 
              ON UPDATE CASCADE  
              ON DELETE CASCADE, 
        category_name varchar(255) NOT NULL,
      	sort_order int default 0
);
create index idx_category1 on category (sort_order);
create index idx_category2 on category (category_name);


