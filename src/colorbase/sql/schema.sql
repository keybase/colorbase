-- :name drop-users-table :!
drop table if exists users;

-- :name drop-keybase-proofs-table :!
drop table if exists keybase_proofs;

-- :name create-users-table :!
create table users (
    username        text    primary key,
    password_hash   blob    not null
)

-- :name create-keybase-proofs-table :!
create table keybase_proofs (
    username            text,
    keybase_username    text,
    sig_hash            text                    not null,
    is_live             numeric     default 0   not null,
    primary key (username, keybase_username),
    foreign key (username) references users(username)
)

-- :name create-user :! :n
insert into users
values (:username, :password-hash)

-- :name create-keybase-proof :! :n
replace into keybase_proofs
values (:username, :keybase-username, :sig-hash, :is-live)

-- :name delete-keybase-proof :! :n
delete from keybase_proofs
where username=:username
and keybase_username=:keybase_username

-- :name enliven-keybase-proof :! :n
update keybase_proofs
set is_live=1
where username=:username
and keybase_username=:keybase-username

-- :name kill-keybase-proof :! :n
update keybase_proofs
set is_live=0
where username=:username
and keybase_username=:keybase-username

-- :name get-user :? :1
select username
from users
where username=:username

-- :name get-user-for-auth :? :1
select username, password_hash
from users
where username=:username

-- :name get-keybase-proofs :? :*
select keybase_username, sig_hash
from keybase_proofs
inner join users
using (username)
where username=:username
and is_live=1

-- :name get-users-with-live-keybase-proof-count :? :*
--; also check live
select username,
       (select count(*)
        from keybase_proofs
        where keybase_proofs.username=users.username
        and is_live=1) as keybase_proof_count
from users
order by keybase_proof_count desc
