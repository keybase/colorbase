-- :name drop-users-table :!
drop table if exists users;

-- :name drop-keybase-users-table :!
drop table if exists keybase_users;

-- :name create-users-table :!
create table users (
    username        text    primary key,
    password_hash   blob    not null,
    salt            blob    not null
)

-- :name create-keybase-users-table :!
create table keybase_users (
    username            text,
    keybase_username    text,
    sig_hash            text                    not null,
    is_verified         numeric     default 0   not null,
    primary key (username, keybase_username),
    foreign key (username) references users(username)
)

-- :name insert-user :! :n
insert or ignore
into users (username, password_hash, salt)
values (:username, :password-hash, :salt)

-- :name set-keybase-username :! :n
insert or ignore
into keybase_users (username, keybase_username, sig_hash)
values (:username, :keybase-username, :sig-hash)

-- :name unset-keybase-username :! :n
delete
from keybase_users
where username=:username
and keybase_username=:keybase-username

-- :name get-user-by-username :? :1
select username
from users
where username=:username

-- :name get-keybase-users-by-username :? :*
select keybase_username, sig_hash from keybase_users
inner join users
using (username)
where username=:username

-- :name get-all-users :? :*
select username
from users
