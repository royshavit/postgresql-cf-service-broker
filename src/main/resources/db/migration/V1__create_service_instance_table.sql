CREATE TABLE "aladin".service (
    serviceinstanceid varchar(36) primary key, 
    servicedefinitionid varchar(128) not null,
    planid varchar(128) not null,
    organizationguid varchar(36) not null,
    spaceguid varchar(36) not null);