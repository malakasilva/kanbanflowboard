-- MySQL dump 10.13  Distrib 5.7.20, for Linux (x86_64)
--
-- Host: localhost    Database: kabana
-- ------------------------------------------------------
-- Server version	5.7.20-0ubuntu0.16.04.1

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
-- Table structure for table `Assignee`
--

DROP TABLE IF EXISTS `Assignee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Assignee` (
  `AssigneeID` char(8) NOT NULL,
  `Login` varchar(45) DEFAULT NULL,
  `Profile` varchar(55) DEFAULT NULL,
  `Lastmodifiedtimestamp` DATETIME DEFAULT NULL,

  PRIMARY KEY (`AssigneeID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `Cards`
--

DROP TABLE IF EXISTS `Cards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Cards` (
  `CardID` char(8) NOT NULL,
  `ColumnName` varchar(45) DEFAULT NULL,
  `ProjectId` char(8) DEFAULT NULL,
  `Note` varchar(255) DEFAULT NULL,
  `IssueId` varchar(45) DEFAULT NULL,
  `Title` varchar(255) DEFAULT NULL,
  `AssigneeId` char(8) DEFAULT NULL,
  `Backlog` datetime DEFAULT NULL,
  `Planned` datetime DEFAULT NULL,
  `USReady` datetime DEFAULT NULL,
  `USReviewed` datetime DEFAULT NULL,
  `DesignReviewed` datetime DEFAULT NULL,
  `InProgress` datetime DEFAULT NULL,
  `Blocked` datetime DEFAULT NULL,
  `CodeReviewed` datetime DEFAULT NULL,
  `SamplesDone` datetime DEFAULT NULL,
  `TestsAutomated` datetime DEFAULT NULL,
  `Done` datetime DEFAULT NULL,
  `IssueLink` VARCHAR(255) NULL,
  `Lastmodifiedtimestamp` DATETIME DEFAULT NULL,
  PRIMARY KEY (`CardID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Projects`
--

DROP TABLE IF EXISTS `Projects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Projects` (
  `ProjectID` char(8) NOT NULL,
  `Name` varchar(45) DEFAULT NULL,
  `State` varchar(45) DEFAULT NULL,
  `Lastmodifiedtimestamp` DATETIME DEFAULT NULL,
  PRIMARY KEY (`ProjectID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

