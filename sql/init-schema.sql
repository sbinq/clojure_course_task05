create table feed (
       id int not null auto_increment,
       title varchar(255) not null,
       link varchar(255) not null,
       parse_url varchar(255) not null,

       primary key (id),
       unique key feed_parse_url_unique (parse_url)
) character set utf8 collate utf8_general_ci;

create table article (
       id int not null auto_increment,
       feed_id int not null,
       link varchar(255) not null,
       published_date datetime not null,
       title varchar(255),
       description_type varchar(255),
       description_value text,
       
       primary key (id),
       constraint foreign key article_feed_fk (feed_id) references feed (id),
       unique key article_feed_link_unique (feed_id, link)
) character set utf8 collate utf8_general_ci;

create table user (
       id int not null auto_increment,

       primary key (id)
) character set utf8 collate utf8_general_ci;

create table user_feed (
       id int not null auto_increment,
       user_id int not null,
       feed_id int not null,

       primary key (id),
       constraint foreign key user_feed_user_fk (user_id) references user (id),
       constraint foreign key user_feed_feed_fk (feed_id) references feed (id),
       unique key user_feed_unique (user_id, feed_id)
) character set utf8 collate utf8_general_ci;

create table user_article_status (
       id int not null auto_increment,
       user_id int not null,
       article_id int not null,
       status varchar(255) not null,

       primary key (id),
       constraint foreign key user_article_status_user_fk (user_id) references user (id),
       constraint foreign key user_article_status_article_fk (article_id) references article (id),
       unique key user_article_unique (user_id, article_id)
) character set utf8 collate utf8_general_ci;
