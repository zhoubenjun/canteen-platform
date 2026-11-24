-- =============================================================
-- 食堂管理数据平台 - 数据库初始化脚本
-- 适用 MySQL 8.0+
-- 使用:  mysql -uroot -p < init.sql
-- =============================================================
CREATE DATABASE IF NOT EXISTS canteen DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE canteen;

-- ---------- 系统设置 ----------
CREATE TABLE IF NOT EXISTS sys_settings (
  id INT PRIMARY KEY AUTO_INCREMENT,
  setting_key VARCHAR(60) NOT NULL UNIQUE,
  setting_value VARCHAR(500),
  remark VARCHAR(200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 食材类别 ----------
CREATE TABLE IF NOT EXISTS ingredient_category (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL UNIQUE,
  sort INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 供应商 ----------
CREATE TABLE IF NOT EXISTS supplier (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 食材 ----------
CREATE TABLE IF NOT EXISTS ingredient (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  category_id INT,
  supplier_id INT,
  spec VARCHAR(120) COMMENT '规格，仅参考',
  price DECIMAL(12,4) COMMENT '单价',
  unit VARCHAR(20) COMMENT '采购单位，默认KG',
  price_unit VARCHAR(20) COMMENT '计价单位，如 元/kg',
  weight_per_unit DECIMAL(12,4) COMMENT '按规格折算单件重量(克)',
  updated_at DATETIME,
  KEY idx_ing_name (name),
  KEY idx_ing_cat (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 食材价格日记 ----------
CREATE TABLE IF NOT EXISTS price_log (
  id INT PRIMARY KEY AUTO_INCREMENT,
  ingredient_id INT,
  ingredient_name VARCHAR(100),
  category_name VARCHAR(50),
  supplier_name VARCHAR(100),
  old_price DECIMAL(12,4),
  new_price DECIMAL(12,4),
  change_pct DECIMAL(10,2) COMMENT '涨幅%',
  change_type VARCHAR(20) COMMENT '新增/修改/删除',
  created_at DATETIME,
  KEY idx_log_ing (ingredient_id),
  KEY idx_log_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 食材价格历史版本快照 ----------
CREATE TABLE IF NOT EXISTS price_history (
  id INT PRIMARY KEY AUTO_INCREMENT,
  version_name VARCHAR(200),
  snapshot_date DATETIME,
  data_json LONGTEXT,
  KEY idx_ph_date (snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 菜品 ----------
CREATE TABLE IF NOT EXISTS dish (
  id INT PRIMARY KEY AUTO_INCREMENT,
  code INT NOT NULL COMMENT '编号，从1自动排列',
  name VARCHAR(100) NOT NULL,
  category VARCHAR(20) COMMENT '鲜荤/冻荤/素菜/主食',
  image VARCHAR(255),
  exclude_seasoning TINYINT DEFAULT 0 COMMENT '是否排除调料费(煮蛋豆浆馒头等)',
  seasoning_cost DECIMAL(10,2) DEFAULT 0.50,
  ingredient_cost DECIMAL(12,2) DEFAULT 0 COMMENT '食材成本',
  labor_fee DECIMAL(12,2) DEFAULT 0 COMMENT '劳务费',
  other_fee DECIMAL(12,2) DEFAULT 0 COMMENT '其他费用',
  suggest_price DECIMAL(12,2) DEFAULT 0 COMMENT '建议售价',
  audit_price DECIMAL(12,2) DEFAULT 0 COMMENT '审核价格',
  created_at DATETIME,
  UNIQUE KEY uk_dish_code (code),
  KEY idx_dish_cat (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 菜品配料 ----------
CREATE TABLE IF NOT EXISTS dish_ingredient (
  id INT PRIMARY KEY AUTO_INCREMENT,
  dish_id INT NOT NULL,
  ingredient_id INT,
  ingredient_name VARCHAR(100),
  weight_g DECIMAL(12,2) COMMENT '克重',
  KEY idx_di_dish (dish_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 周食谱 ----------
CREATE TABLE IF NOT EXISTS weekly_recipe (
  id INT PRIMARY KEY AUTO_INCREMENT,
  week_key VARCHAR(20) COMMENT '周一日期 如2026-09-28',
  day_index INT COMMENT '0=周一..6=周日',
  meal_type VARCHAR(10) COMMENT '早餐/中餐/晚餐',
  dish_id INT,
  dish_name VARCHAR(100),
  category VARCHAR(20),
  people_count INT COMMENT '每餐人数',
  portions INT COMMENT '份数',
  seq INT DEFAULT 0,
  KEY idx_wr_week (week_key),
  KEY idx_wr_day (day_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 周食谱模板 ----------
CREATE TABLE IF NOT EXISTS recipe_template (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100),
  data_json LONGTEXT,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 采购计划 ----------
CREATE TABLE IF NOT EXISTS purchase_plan (
  id INT PRIMARY KEY AUTO_INCREMENT,
  plan_key VARCHAR(30) COMMENT '生成批次',
  week_key VARCHAR(20),
  supplier_id INT,
  supplier_name VARCHAR(100),
  ingredient_id INT,
  ingredient_name VARCHAR(100),
  category_name VARCHAR(50),
  total_weight DECIMAL(12,2) COMMENT '总重KG',
  unit VARCHAR(20),
  price DECIMAL(12,4),
  amount DECIMAL(12,2) COMMENT '金额',
  meal_type VARCHAR(20) COMMENT '早餐/中餐/晚餐/合并',
  created_at DATETIME,
  KEY idx_pp_plan (plan_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================
-- 种子数据
-- =============================================================
INSERT INTO sys_settings (setting_key, setting_value, remark) VALUES
 ('entrance_rate','0.90','食材入口率(0-1,两位小数)'),
 ('labor_rate','0.10','劳务费比例(0-1)'),
 ('other_rate','0.05','其他费用比例(0-1)'),
 ('seasoning_cost','0.50','油盐酱醋等调料费(元/份)'),
 ('canteen_name','郧西县职业技术学校食堂','食堂名称'),
 ('publicity_title','郧西县职业技术学校每日食谱','公示标题'),
 ('semester_mode','0','周显示模式 0公历 1学期'),
 ('semester1_start','2026-09-01','第一学期开学日期'),
 ('semester2_start','2027-02-22','第二学期开学日期'),
 ('bigscreen_per_page','6','大屏每页数量'),
 ('bigscreen_show_image','1','大屏显示图片');

INSERT INTO ingredient_category (name, sort) VALUES
 ('粮油类',1),('干调类',2),('鲜肉类',3),('果蔬类',4),
 ('冻货类',5),('禽肉类',6),('烤肠类',7),('面条类',8);

INSERT INTO supplier (name) VALUES
 ('十堰农贸批发市场'),('郧西绿源配送'),('宏发冷冻食品'),('本地蔬菜基地');

-- 示例食材
INSERT INTO ingredient (name, category_id, supplier_id, spec, price, unit, price_unit, weight_per_unit, updated_at) VALUES
 ('猪肉(五花)', 3, 1, '整箱20kg', 26.00, 'KG', '元/kg', 1000, NOW()),
 ('猪肉(里脊)', 3, 1, '整箱20kg', 30.00, 'KG', '元/kg', 1000, NOW()),
 ('土豆', 4, 4, '袋装50kg', 3.60, 'KG', '元/kg', 1000, NOW()),
 ('西红柿', 4, 4, '筐装25kg', 5.20, 'KG', '元/kg', 1000, NOW()),
 ('鸡蛋', 3, 1, '60枚/箱 约3.6kg', 9.80, 'KG', '元/kg', 1000, NOW()),
 ('面粉', 1, 2, '袋装25kg', 4.50, 'KG', '元/kg', 1000, NOW()),
 ('大米', 1, 2, '袋装25kg', 5.20, 'KG', '元/kg', 1000, NOW()),
 ('豆腐', 4, 4, '板装 约1kg/板', 4.00, 'KG', '元/kg', 1000, NOW()),
 ('白菜', 4, 4, '袋装40kg', 2.80, 'KG', '元/kg', 1000, NOW()),
 ('鸡腿肉', 3, 1, '冻品20kg', 18.50, 'KG', '元/kg', 1000, NOW()),
 ('食用油', 1, 2, '10L/桶', 14.00, 'KG', '元/L', 910, NOW()),
 ('生抽', 2, 2, '1.8L/瓶', 6.00, 'KG', '元/瓶', 1800, NOW()),
 ('冻虾仁', 5, 3, '袋装10kg', 42.00, 'KG', '元/kg', 1000, NOW()),
 ('烤肠', 7, 3, '70g50根4包/件', 130.00, 'KG', '元/kg', 1000, NOW()),
 ('面条', 8, 2, '袋装10kg', 5.80, 'KG', '元/kg', 1000, NOW()),
 ('馒头', 1, 2, '个', 0.80, 'KG', '元/个', 100, NOW());

-- 示例菜品
INSERT INTO dish (code, name, category, image, exclude_seasoning, seasoning_cost, ingredient_cost, labor_fee, other_fee, suggest_price, audit_price, created_at) VALUES
 (1,'红烧肉','鲜荤',NULL,0,0.50,8.00,1.60,0.80,11.00,12.00,NOW()),
 (2,'西红柿炒鸡蛋','鲜荤',NULL,0,0.50,4.00,0.80,0.40,6.00,6.50,NOW()),
 (3,'清炒土豆丝','素菜',NULL,0,0.50,1.80,0.40,0.20,3.00,3.00,NOW()),
 (4,'白菜炖豆腐','素菜',NULL,0,0.50,2.20,0.50,0.25,3.50,4.00,NOW()),
 (5,'馒头','主食',NULL,1,0.00,0.80,0.10,0.05,1.00,1.00,NOW()),
 (6,'白米饭','主食',NULL,1,0.00,0.60,0.10,0.05,0.80,0.80,NOW()),
 (7,'面条','主食',NULL,0,0.50,2.50,0.50,0.25,4.00,4.50,NOW()),
 (8,'水煮鸡蛋','鲜荤',NULL,1,0.00,1.00,0.10,0.05,1.20,1.20,NOW());

-- 示例配料
INSERT INTO dish_ingredient (dish_id, ingredient_id, ingredient_name, weight_g) VALUES
 (1,1,'猪肉(五花)',200),(1,2,'猪肉(里脊)',100),(1,4,'西红柿',50),
 (2,4,'西红柿',100),(2,5,'鸡蛋',60),
 (3,3,'土豆',200),
 (4,9,'白菜',150),(4,8,'豆腐',100),
 (5,16,'馒头',100),
 (7,15,'面条',200),
 (8,5,'鸡蛋',55);

-- 示例周食谱(本周一为2026-09-28)
INSERT INTO weekly_recipe (week_key, day_index, meal_type, dish_id, dish_name, category, people_count, portions, seq) VALUES
 ('2026-09-28',0,'早餐',5,'馒头','主食',1200,1200,0),
 ('2026-09-28',0,'早餐',8,'水煮鸡蛋','鲜荤',1200,0,1),
 ('2026-09-28',0,'早餐',7,'面条','主食',1200,600,2),
 ('2026-09-28',0,'中餐',1,'红烧肉','鲜荤',1100,550,0),
 ('2026-09-28',0,'中餐',2,'西红柿炒鸡蛋','鲜荤',1100,550,1),
 ('2026-09-28',0,'中餐',3,'清炒土豆丝','素菜',1100,550,2),
 ('2026-09-28',0,'中餐',4,'白菜炖豆腐','素菜',1100,550,3),
 ('2026-09-28',0,'晚餐',3,'清炒土豆丝','素菜',900,450,0),
 ('2026-09-28',0,'晚餐',4,'白菜炖豆腐','素菜',900,450,1);

-- 页面内容表：用于在后台维护法律声明等静态页面的标题与正文
-- 正文格式：每行一个段落；以 "- " 开头的行按二级缩进（子项）渲染
CREATE TABLE IF NOT EXISTS sys_page_content (
  id INT AUTO_INCREMENT PRIMARY KEY,
  page_key VARCHAR(64) NOT NULL COMMENT '页面标识，如 legal',
  page_title VARCHAR(128) NOT NULL COMMENT '页面标题',
  content LONGTEXT COMMENT '正文，每行一段，"- " 开头为子项',
  remark VARCHAR(255) DEFAULT NULL COMMENT '后台说明',
  updated_by VARCHAR(64) DEFAULT NULL,
  updated_at DATETIME DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_page_key (page_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='静态页面内容';

INSERT INTO sys_page_content(page_key, page_title, content, remark, updated_by, updated_at)
VALUES ('legal', '法律声明',
'郧西县职业技术学校数字食堂及其注册用户及本系统的资料提供者拥有本系统所有资料的版权。未经郧西县职业技术学校的明确书面许可，任何人不得复制或在非“郧西县职业技术学校”所属的服务器上做镜像。
郧西县职业技术学校数字食堂（以下简称“本系统”）是郧西县职业技术学校主办，郧西县职业技术学校对其实施管理、指导、协调和监督的学校系统。
特此说明本系统对系统资料所采取的政策，请您务必仔细阅读：
1、资料保护
使用本系统的用户，享有本系统的使用权以及查询数据的权利，但未经郧西县职业技术学校管理中心的书面许可，任何人不得复制或在非“郧西县职业技术学校数字”所属的服务器上做镜像。
2、免 责
下列情况时本系统毋需承担任何责任：
- 1) 任何由于计算机2000年问题、黑客攻击、计算机病毒侵入或发作、因学校管制而造成的暂时性关闭等影响网络正常经营之不可抗力而造成的资料泄露、丢失、被盗用或被篡改等。
- 2) 由于与本系统链接的其他网站所造成之个人资料泄露及由此而导致的任何法律争议和后果。',
'登录页“法律声明”链接打开的页面', 'system', NOW())
ON DUPLICATE KEY UPDATE page_key = page_key;
