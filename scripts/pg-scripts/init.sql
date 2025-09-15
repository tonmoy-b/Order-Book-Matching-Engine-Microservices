--CREATE DATABASE db01;
\c db01;

DROP TABLE IF EXISTS users;

CREATE TABLE users (
        id serial primary key,
        name varchar(255),
        email varchar (100)
);

insert into users (name, email) values ('customer1', 'cust1@gmail.com');
insert into users (name, email) values ('customer2', 'cust2@gmail.com');