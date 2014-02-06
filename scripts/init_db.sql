DROP DATABASE IF EXISTS Juliet;
CREATE DATABASE Juliet DEFAULT CHARACTER SET UTF8 COLLATE utf8_general_ci;
USE Juliet;

DROP TABLE IF EXISTS symbol;
CREATE TABLE IF NOT EXISTS symbol (
  symbol_id int(10) unsigned NOT NULL,
  symbol varchar(16) NOT NULL,
  company_name varchar(100) NOT NULL,
  price_scale int(10) unsigned NOT NULL,
  open_price int(10) unsigned NOT NULL,
  PRIMARY KEY(symbol_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=0;

DROP TABLE IF EXISTS buffer;
CREATE TABLE IF NOT EXISTS buffer (
  id int(10) unsigned NOT NULL AUTO_INCREMENT,
  symbol_id int(10) unsigned NOT NULL,
  order_id int(10) unsigned NOT NULL,
  operation_code varchar(255) NOT NULL,
  price int(10) unsigned NOT NULL,
  volume int(10) unsigned NOT NULL,
  awaiting_order_id int(10) unsigned NOT NULL,
  placed_s int(10) unsigned NOT NULL,
  placed_seq_num int(10) unsigned NOT NULL,
  PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=0;

DROP TABLE IF EXISTS stock_summary;
CREATE TABLE IF NOT EXISTS stock_summary (
  id int(10) unsigned NOT NULL AUTO_INCREMENT,
  symbol_id int(10) unsigned NOT NULL,
  high_price int(10) unsigned NOT NULL,
  low_price int(10) unsigned NOT NULL,
  total_volume int(10) unsigned NOT NULL,
  updated_s int(10) unsigned NOT NULL,
  updated_seq_num int(10) unsigned NOT NULL,
  PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=0;

DROP TABLE IF EXISTS trade;
CREATE TABLE IF NOT EXISTS trade (
  id int(10) unsigned NOT NULL AUTO_INCREMENT,
  symbol_id int(10) unsigned NOT NULL,
  price int(10) unsigned NOT NULL,
  volume int(10) unsigned NOT NULL,
  offered_s int(10) unsigned NOT NULL,
  offered_seq_num int(10) unsigned NOT NULL,
  completed_s int(10) unsigned NOT NULL,
  completed_seq_num int(10) unsigned NOT NULL,
  PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=0;

DROP TABLE IF EXISTS order_book;
CREATE TABLE IF NOT EXISTS order_book (
  order_id int(10) unsigned NOT NULL,
  symbol_id int(10) unsigned NOT NULL,
  price int(10) unsigned NOT NULL,
  volume int(10) unsigned NOT NULL,
  is_ask bit(1) NOT NULL, 
  placed_s int(10) unsigned NOT NULL,
  placed_seq_num int(10) unsigned NOT NULL,
  updated_s int(10) unsigned NOT NULL,
  updated_seq_num int(10) unsigned NOT NULL,
  PRIMARY KEY(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=0;
