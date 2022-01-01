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
DROP TABLE IF EXISTS `user`;

CREATE TABLE IF NOT EXISTS 	`board` (
	`creator` varchar(50),
	`number` int,
	`name` varchar(50),
    PRIMARY KEY (`number`),
    KEY(`name`)
);
INSERT INTO board(`creator`, `number`, `name`) VALUES("", 1, "Everything Else");
INSERT INTO board(`creator`, `number`, `name`) VALUES("", 2, "SAS4");
INSERT INTO board(`creator`, `number`, `name`) VALUES("", 3, "Skyblock");

CREATE TABLE IF NOT EXISTS `user` (
  `username` varchar(50) ,
  `password` varchar(50) ,
  `id` int ,
  `pfp` varchar(100),
  `boards` text,
  PRIMARY KEY (`id`),
  KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
INSERT INTO user(`username`, `password`, `id`, `pfp`, `boards`) VALUES("klin_", "catfish2001", 0, "hydar2.png", " 1, 2, 3, ");
INSERT INTO user(`username`, `password`, `id`, `pfp`, `boards`) VALUES("Glenn M", "password", 1, "hydar2.png", " 1, 2, 3, ");

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
DROP TABLE IF EXISTS `post`;
CREATE TABLE IF NOT EXISTS `post` (
  `contents` varchar(500),
  `id` int ,
  `board` int,
  `created_date` long,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
#INSERT INTO post(`contents`, `id`, `board`, `created_date`)
	#VALUES ("Hello", 0, 1, 0);

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `posts` (
  `user` int ,
  `post` int ,
  PRIMARY KEY (`user`, `post`),
  KEY `user_posts_post` (`user`),
  CONSTRAINT `user_posts_post` FOREIGN KEY (`user`) REFERENCES `user` (`id`),
  CONSTRAINT `post_posted` FOREIGN KEY (`post`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
#INSERT INTO posts(`user`, `post`)
#	VALUES (0, 0);
    
#SELECT * FROM posts

#INSERT INTO admin(`id`, `phone`,  `name`, `ssn`) VALUES(134, "7327321234", "Admin1", 25235252);
#INSERT INTO user(`username`, `password`, `id`) VALUES("Admin1",  "password", 134);
#INSERT INTO customer_rep(`id`, `phone`,  `name`, `ssn`) VALUES(14, "7327321234", "customerrep1", 25235252);
#INSERT INTO user(`username`, `password`, `id`) VALUES("cr1",  "password", 14);

#DELETE FROM customer_rep WHERE id=13;
#SELECT * FROM QUESTION;
#DELETE FROM is_for WHERE item = 123;
#SELECT bid.amount, item.id, item.name, item.highest_bid, item.location, item.close_date FROM bid, user, creates, is_for, item 
#	WHERE user.id=creates.user AND creates.bid = bid.id
#		AND bid.id = is_for.bid AND is_for.item = item.id AND user.id = 7 ORDER BY item.id;
