DROP DATABASE IF EXISTS juliet;
CREATE DATABASE juliet DEFAULT CHARACTER SET UTF8 COLLATE utf8_general_ci;
USE juliet;

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
  updated_ns int(10) unsigned NOT NULL,
  PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=0;

DROP TABLE IF EXISTS trade;
CREATE TABLE IF NOT EXISTS trade (
  trade_id int(10) unsigned NOT NULL,
  symbol_id int(10) unsigned NOT NULL,
  price int(10) unsigned NOT NULL,
  volume int(10) unsigned NOT NULL,
  offered_s int(10) unsigned NOT NULL,
  offered_seq_num int(10) unsigned NOT NULL,
  PRIMARY KEY(trade_id, symbol_id)
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
  added bit(1) NOT NULL,
  deleted bit(1) NOT NULL,
  PRIMARY KEY(order_id, symbol_id),
  INDEX name (order_id,symbol_id)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 AUTO_INCREMENT=0;


DROP PROCEDURE IF EXISTS addOrder;

DELIMITER //
CREATE PROCEDURE addOrder(IN p_order_id int(10) unsigned,
                          IN p_symbol_id int(10) unsigned,
                          IN p_price int(10) unsigned, 
                          IN p_volume int(10) unsigned,
                          IN p_is_ask bit(1),
                          IN p_placed_s int(10) unsigned,
                          IN p_placed_seq_num int(10) unsigned,
                          IN p_updated_s int(10) unsigned,
                          IN p_updated_seq_num int(10) unsigned)
BEGIN
    DECLARE last_updated_s int(10) unsigned;
    DECLARE last_updated_seq_num int(10) unsigned;
    SELECT updated_s, updated_seq_num FROM order_book
                     WHERE order_id = p_order_id AND symbol_id = p_symbol_id
                     INTO last_updated_s, last_updated_seq_num;
    
    IF last_updated_s IS NULL THEN
        INSERT INTO order_book VALUES (p_order_id, p_symbol_id, p_price,
                                       p_volume, p_is_ask, p_placed_s,
                                       p_placed_seq_num, p_updated_s,
                                       p_updated_seq_num, 1, 0);
    ELSEIF last_updated_s < p_updated_s OR (last_updated_s = p_updated_s
                 AND last_updated_seq_num < p_updated_seq_num) THEN
        UPDATE order_book SET price = p_price, volume = p_volume,
                              is_ask = p_is_ask, placed_s = p_placed_s,
                              placed_seq_num = p_placed_seq_num,
                              updated_s = p_updated_s,
                              updated_seq_num = p_updated_seq_num,
                              added = 1, deleted = 0
                           WHERE (order_id = p_order_id) AND
                                 (symbol_id = p_symbol_id);
    ELSE
        UPDATE order_book SET is_ask = p_is_ask, placed_s = p_placed_s,
                              placed_seq_num = p_placed_seq_num, added = 1
                          WHERE (order_id = p_order_id) AND
                                (symbol_id = p_symbol_id);
    END IF;
END //
DELIMITER ;


DROP PROCEDURE IF EXISTS deleteOrder;

DELIMITER //
CREATE PROCEDURE deleteOrder(IN p_order_id int(10) unsigned,
                             IN p_symbol_id int(10) unsigned,
                             IN p_s int(10) unsigned,
                             IN p_seq_num int(10) unsigned)
BEGIN
    DECLARE last_updated_s int(10) unsigned;
    DECLARE last_updated_seq_num int(10) unsigned;
    SELECT updated_s, updated_seq_num FROM order_book
                     WHERE order_id = p_order_id AND symbol_id = p_symbol_id
                     INTO last_updated_s, last_updated_seq_num;

    IF last_updated_s IS NULL THEN
        INSERT INTO order_book VALUES (p_order_id, p_symbol_id, 0, 0, 0,
                                       0, 0, p_s, p_seq_num, 0, 1);
    ELSEIF last_updated_s < p_s OR (last_updated_s = p_s
                 AND last_updated_seq_num < p_seq_num) THEN
        UPDATE order_book SET updated_s = p_s, updated_seq_num = p_seq_num,
                              deleted = 1
                          WHERE (order_id = p_order_id) AND
                                (symbol_id = p_symbol_id);
    END IF;
END //
DELIMITER ;


DROP PROCEDURE IF EXISTS modifyOrder;

DELIMITER //
CREATE PROCEDURE modifyOrder(IN p_order_id int(10) unsigned,
                             IN p_symbol_id int(10) unsigned,
                             IN p_price int(10) unsigned,
                             IN p_volume int(10) unsigned,
                             IN p_s int(10) unsigned,
                             IN p_seq_num int(10) unsigned)
BEGIN
    DECLARE last_updated_s int(10) unsigned;
    DECLARE last_updated_seq_num int(10) unsigned;
    SELECT updated_s, updated_seq_num FROM order_book
                     WHERE order_id = p_order_id AND symbol_id = p_symbol_id
                     INTO last_updated_s, last_updated_seq_num;
    
    IF last_updated_s IS NULL THEN
        INSERT INTO order_book VALUES (p_order_id, p_symbol_id, p_price,
                                       p_volume, 0, 0, 0, p_s, p_seq_num,
                                       0, 0);
    ELSEIF last_updated_s < p_s OR (last_updated_s = p_s
                 AND last_updated_seq_num < p_seq_num) THEN
        UPDATE order_book SET price = p_price, volume = p_volume,
                              updated_s = p_s,
                              updated_seq_num = p_seq_num;
    END IF;
END //
DELIMITER ;
