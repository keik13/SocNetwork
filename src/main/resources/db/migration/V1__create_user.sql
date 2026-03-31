create table soc_user
(
	id uuid primary key,
    first_name varchar(50) not null,
    second_name varchar(50) not null,
    birthdate date not null,
    biography text not null,
    city varchar(50) not null,
    password varchar(100) not null
);