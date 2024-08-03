CREATE DATABASE IF NOT EXISTS `chatRoom`;
USE `chatRoom`;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
DROP TABLE IF EXISTS `board`;

CREATE TABLE IF NOT EXISTS 	`board` (
	`creator` int not null,
	`number` int NOT NULL AUTO_INCREMENT,
	`name` varchar(50) not null,
    `image` varchar(20) not null,
	`channelof` int not null default -1,
    `public` tinyint not null default 0,
	`dm` tinyint not null default 0,
    `readonly` tinyint not null default 0,
    PRIMARY KEY (`number`),
    KEY(`name`)
);
CREATE INDEX get_channels ON `board`(`channelof`) USING HASH;
INSERT INTO board(`number`,`creator`, `name`, `public`, `image`,`channelof`,`dm`,`readonly`) VALUES(-1, -1, "Default Board", 0, "misc.png",-1,0,0);
INSERT INTO board(`creator`, `name`, `public`, `image`,`channelof`,`dm`,`readonly`) VALUES(-1, "Everything Else", 1, "everythingelse.png",-1,0,0);
INSERT INTO board(`creator`, `name`, `public`, `image`,`channelof`,`dm`,`readonly`) VALUES(-1, "SAS4", 1, "sas4.png",-1,0,0);
INSERT INTO board(`creator`, `name`, `public`, `image`,`channelof`,`dm`,`readonly`) VALUES(-1, "Skyblock", 1, "skyblock.png",-1,0,0);
INSERT INTO board(`creator`, `name`, `public`, `image`,`channelof`,`dm`,`readonly`) VALUES(1, "Hydar", 0, "misc.png",-1,0,0);

DROP TABLE IF EXISTS `user`;
CREATE TABLE IF NOT EXISTS `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) not null,
  `password` varbinary(32) not null,
  `addr` varbinary(16) not null DEFAULT x'7f000001',
  `pfp` varchar(100) not null,
  `permission_level` enum("water_hydar","great_white","yeti","skeleton","reserved2") not null,
  `created_date` bigint not null default 0,
  `pings` tinyint not null,
  `volume` tinyint not null,
  `pingvolume` tinyint not null,
  `vcvolume` tinyint not null,
  PRIMARY KEY (`id`),
  KEY (`username`),
  KEY (`addr`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
DROP TABLE IF EXISTS `ban`;
CREATE TABLE IF NOT EXISTS `ban`(
	`id` int,
	`type` enum("user","message","addr"),
	`addr` varbinary(16) not null,
	PRIMARY KEY(`addr`),
    KEY(`id`)
);
/*!40101 SET character_set_client = @saved_cs_client */;
INSERT INTO user(`username`, `id`,  `password`, `pfp`, `permission_level`, `pings`, `volume`, `pingvolume`, `vcvolume`) VALUES("Deleted User",-1,"hydarhydar", "images/hydar2.png", "water_hydar", 0, 50, 50, 50);
INSERT INTO user(`username`, `id`,  `password`, `pfp`, `permission_level`, `pings`, `volume`, `pingvolume`, `vcvolume`) VALUES("Raye",2,"raye", "images/r.png", "water_hydar", 0, 50, 50, 50);
INSERT INTO user(`username`, `id`,  `password`, `pfp`, `permission_level`, `pings`, `volume`, `pingvolume`, `vcvolume`) VALUES("Guest",3,"skeleton", "images/emp.png", "skeleton", 0, 50, 50, 50);
SELECT * FROM user;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
DROP TABLE IF EXISTS `post`;
CREATE TABLE IF NOT EXISTS `post` (
  `contents` varchar(3000) not null,
  `id` int NOT NULL AUTO_INCREMENT not null,
  `board` int not null,
  `created_date` bigint not null,
  `addr` varbinary(16),/*nullable*/
  CONSTRAINT `board_posts_post` FOREIGN KEY (`board`) REFERENCES `board` (`number`) ON DELETE CASCADE,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
ALTER TABLE `post` AUTO_INCREMENT = 0;


/*!40101 SET character_set_client = @saved_cs_client */;
INSERT INTO post(`contents`, `id`, `board`, `created_date`) VALUES ("Hydar", -1, -1, 0);

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
DROP TABlE IF EXISTS `posts`;
CREATE TABLE IF NOT EXISTS `posts` (
  `user` int not null,
  `post` int not null,
  PRIMARY KEY (`user`, `post`),
  CONSTRAINT `user_posts_post` FOREIGN KEY (`user`) REFERENCES `user` (`id`),
  CONSTRAINT `post_posted` FOREIGN KEY (`post`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
INSERT INTO posts(`user`, `post`) VALUES (-1, -1);

DROP TABLE IF EXISTS `isin`;
CREATE TABLE IF NOT EXISTS `isin` (
	`user` int not null,
	`board` int not null,
    `lastvisited` bigint not null,
    PRIMARY KEY (`user`, `board`),
	CONSTRAINT `user_is_in` FOREIGN KEY (`user`) REFERENCES `user` (`id`) ON DELETE CASCADE,
	CONSTRAINT `is_in_board` FOREIGN KEY (`board`) REFERENCES `board` (`number`) ON DELETE CASCADE
);
INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (-1, -1, 0);
INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (-1, 1, 0);
INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (-1, 2, 0);
INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (-1, 3, 0);
INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (2, 1, 0);
INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (2, 2, 0);
INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (2, 3, 0);
INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (2, 4, 0);
INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (3, 1, 0);
INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (3, 2, 0);
INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (3, 3, 0);
DROP TABLE IF EXISTS `invitedto`;
CREATE TABLE IF NOT EXISTS `invitedto` (
	`user` int not null,
	`board` int not null,
    PRIMARY KEY (`user`, `board`),
	CONSTRAINT `user_invited_to` FOREIGN KEY (`user`) REFERENCES `user` (`id`) ON DELETE CASCADE,
	CONSTRAINT `invited_to_board` FOREIGN KEY (`board`) REFERENCES `board` (`number`) ON DELETE CASCADE
);
DROP TABLE IF EXISTS `file`;
CREATE TABLE IF NOT EXISTS `file` (
    `path` CHAR(16) not null,
    `filename` VARCHAR(64) not null,
	`user` int not null,
	`board` int not null,
    `post` int not null,
	`size` bigint not null,
    `date` bigint not null,
    PRIMARY KEY (`path`),
    CONSTRAINT `file_attached_to` FOREIGN KEY (`post`) REFERENCES `post` (`id`) ON DELETE CASCADE,
	CONSTRAINT `file_uploaded_by` FOREIGN KEY (`user`) REFERENCES `user` (`id`) ON DELETE CASCADE,
	CONSTRAINT `file_in_board` FOREIGN KEY (`board`) REFERENCES `board` (`number`) ON DELETE CASCADE
);
ALTER TABLE posts DROP FOREIGN KEY `user_posts_post`;
ALTER TABLE posts ADD FOREIGN KEY `user_posts_post` (`user`) REFERENCES `user` (`id`);
SELECT * FROM invitedto;
SELECT * FROM user;
SELECT * FROM board;
SELECT * FROM isin;
SELECT * FROM post;
SELECT * FROM `file`;
SELECT * FROM posts;