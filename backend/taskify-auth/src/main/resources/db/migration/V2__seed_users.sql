-- 预置 5 名用户（1 产品经理 + 4 工程师），角色仅为身份标签，不引入差异化权限。
-- 统一密码：taskify123（bcrypt 哈希）。
INSERT INTO taskify_user (id, username, password_hash, created_at, updated_at) VALUES
  ('00000000-0000-7000-0000-000000000001', 'pm_user',    '$2b$10$febR4OgwPpnHwmJ/PRR85evmcwzR.wi2UbzaZdZxfwsAU9cNeJC.K', now(), now()),
  ('00000000-0000-7000-0000-000000000002', 'engineer1',  '$2b$10$febR4OgwPpnHwmJ/PRR85evmcwzR.wi2UbzaZdZxfwsAU9cNeJC.K', now(), now()),
  ('00000000-0000-7000-0000-000000000003', 'engineer2',  '$2b$10$febR4OgwPpnHwmJ/PRR85evmcwzR.wi2UbzaZdZxfwsAU9cNeJC.K', now(), now()),
  ('00000000-0000-7000-0000-000000000004', 'engineer3',  '$2b$10$febR4OgwPpnHwmJ/PRR85evmcwzR.wi2UbzaZdZxfwsAU9cNeJC.K', now(), now()),
  ('00000000-0000-7000-0000-000000000005', 'engineer4',  '$2b$10$febR4OgwPpnHwmJ/PRR85evmcwzR.wi2UbzaZdZxfwsAU9cNeJC.K', now(), now());
