-- MySQL dump 10.13  Distrib 5.5.12, for Win32 (x86)
--
-- Host: localhost    Database: rpa
-- ------------------------------------------------------
-- Server version	5.5.12

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

--
-- Table structure for table `element_configs`
--

DROP TABLE IF EXISTS `element_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `element_configs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `context` varchar(100) NOT NULL,
  `image_data` mediumblob,
  `image_path` varchar(255) DEFAULT NULL,
  `xpath` varchar(500) DEFAULT NULL,
  `css_selector` varchar(500) DEFAULT NULL,
  `locator_type` varchar(50) DEFAULT 'image',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_context_name` (`context`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `element_configs`
--

LOCK TABLES `element_configs` WRITE;
/*!40000 ALTER TABLE `element_configs` DISABLE KEYS */;
/*!40000 ALTER TABLE `element_configs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_case_steps`
--

DROP TABLE IF EXISTS `test_case_steps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `test_case_steps` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `test_case_id` bigint(20) NOT NULL,
  `step_order` int(11) NOT NULL,
  `step_type` varchar(50) NOT NULL,
  `content` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_case_order` (`test_case_id`,`step_order`),
  CONSTRAINT `test_case_steps_ibfk_1` FOREIGN KEY (`test_case_id`) REFERENCES `test_cases` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=159 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_case_steps`
--

LOCK TABLES `test_case_steps` WRITE;
/*!40000 ALTER TABLE `test_case_steps` DISABLE KEYS */;
INSERT INTO `test_case_steps` VALUES (123,5,0,'访问','www.zwdoctor.com:44502/#/login'),(124,5,1,'等待','3秒'),(125,5,2,'在','test的account input输入\"123\"'),(126,5,3,'在','test的password input输入\"123\"'),(127,5,4,'如果','test的login button存在，则点击test的login button'),(128,5,5,'如果','test的success icon出现，则输出success'),(150,6,0,'访问','login.taobao.com'),(151,6,1,'点击','taobao的login button'),(152,6,2,'如果','taobao的accounterror label出现，则在taobao的account input输入\"123456789\"'),(153,6,3,'在','taobao的password input输入\"123456789\"'),(154,6,4,'点击','taobao的login button'),(155,6,5,'如果','taobao的passworderror label出现，则输出success'),(158,9,0,'点击','zhihu的download button');
/*!40000 ALTER TABLE `test_case_steps` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_cases`
--

DROP TABLE IF EXISTS `test_cases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `test_cases` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_cases`
--

LOCK TABLES `test_cases` WRITE;
/*!40000 ALTER TABLE `test_cases` DISABLE KEYS */;
INSERT INTO `test_cases` VALUES (5,'测试',NULL),(6,'test',NULL),(9,'1',NULL);
/*!40000 ALTER TABLE `test_cases` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-05-25 23:51:51
