CREATE TABLE "brokerdb".serviceinstance (
    id varchar(36) primary key, 
    service_definition_id varchar(128) not null,
    plan_id varchar(128) not null,
    org_id varchar(36) not null,
    space_id varchar(36) not null);