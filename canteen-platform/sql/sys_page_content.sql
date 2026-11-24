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
