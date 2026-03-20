-- MySQL seed script for categories (2-level) and products
-- Safe to run multiple times (insert-if-missing + update existing)

-- =========================
-- CLEANUP DUPLICATE DATA
-- =========================
-- 1) Gộp category cấp 1 bị trùng tên (giữ id nhỏ nhất)
DROP TEMPORARY TABLE IF EXISTS tmp_root_keep;
CREATE TEMPORARY TABLE tmp_root_keep AS
SELECT LOWER(name) AS name_key, MIN(id) AS keep_id
FROM categories
WHERE parent_id IS NULL
GROUP BY LOWER(name)
HAVING COUNT(*) > 1;

UPDATE categories child
JOIN categories root_dup ON child.parent_id = root_dup.id
JOIN tmp_root_keep k ON LOWER(root_dup.name) = k.name_key
SET child.parent_id = k.keep_id
WHERE root_dup.parent_id IS NULL
  AND root_dup.id <> k.keep_id;

UPDATE products p
JOIN categories root_dup ON p.category_id = root_dup.id
JOIN tmp_root_keep k ON LOWER(root_dup.name) = k.name_key
SET p.category_id = k.keep_id
WHERE root_dup.parent_id IS NULL
  AND root_dup.id <> k.keep_id;

DELETE c
FROM categories c
JOIN tmp_root_keep k ON LOWER(c.name) = k.name_key
WHERE c.parent_id IS NULL
  AND c.id <> k.keep_id;

DROP TEMPORARY TABLE IF EXISTS tmp_root_keep;

-- 2) Gộp category cấp 2 bị trùng trong cùng parent (giữ id nhỏ nhất)
DROP TEMPORARY TABLE IF EXISTS tmp_child_keep;
CREATE TEMPORARY TABLE tmp_child_keep AS
SELECT LOWER(name) AS name_key, parent_id, MIN(id) AS keep_id
FROM categories
WHERE parent_id IS NOT NULL
GROUP BY LOWER(name), parent_id
HAVING COUNT(*) > 1;

UPDATE products p
JOIN categories child_dup ON p.category_id = child_dup.id
JOIN tmp_child_keep k ON LOWER(child_dup.name) = k.name_key AND child_dup.parent_id = k.parent_id
SET p.category_id = k.keep_id
WHERE child_dup.id <> k.keep_id;

DELETE c
FROM categories c
JOIN tmp_child_keep k ON LOWER(c.name) = k.name_key AND c.parent_id = k.parent_id
WHERE c.id <> k.keep_id;

DROP TEMPORARY TABLE IF EXISTS tmp_child_keep;

-- =========================
-- ROOT CATEGORIES (LEVEL 1)
-- =========================
INSERT INTO categories (name, icon, image, parent_id)
SELECT 'Điện thoại', 'Điện thoại', 'https://cdn.tgdd.vn/content/phonne-24x24.png', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE LOWER(name) = LOWER('Điện thoại') AND parent_id IS NULL
);

INSERT INTO categories (name, icon, image, parent_id)
SELECT 'Laptop', 'Laptop', 'https://cdn.tgdd.vn/content/laptop-24x24.png', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE LOWER(name) = LOWER('Laptop') AND parent_id IS NULL
);

INSERT INTO categories (name, icon, image, parent_id)
SELECT 'Tablet', 'Tablet', 'https://cdn.tgdd.vn/content/tablet-24x24.png', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE LOWER(name) = LOWER('Tablet') AND parent_id IS NULL
);

INSERT INTO categories (name, icon, image, parent_id)
SELECT 'Phụ kiện', 'Phụ kiện', 'https://cdn.tgdd.vn/content/phu-kien-24x24.png', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE LOWER(name) = LOWER('Phụ kiện') AND parent_id IS NULL
);

INSERT INTO categories (name, icon, image, parent_id)
SELECT 'Smartwatch', 'Smartwatch', 'https://cdn.tgdd.vn/content/smartwatch-24x24.png', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE LOWER(name) = LOWER('Smartwatch') AND parent_id IS NULL
);

INSERT INTO categories (name, icon, image, parent_id)
SELECT 'Đồng hồ', 'Đồng hồ', 'https://cdn.tgdd.vn/content/watch-24x24.png', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE LOWER(name) = LOWER('Đồng hồ') AND parent_id IS NULL
);

-- Keep root category metadata consistent
UPDATE categories
SET icon = 'Điện thoại', image = 'https://cdn.tgdd.vn/content/phonne-24x24.png', parent_id = NULL
WHERE LOWER(name) = LOWER('Điện thoại') AND parent_id IS NULL;

UPDATE categories
SET icon = 'Laptop', image = 'https://cdn.tgdd.vn/content/laptop-24x24.png', parent_id = NULL
WHERE LOWER(name) = LOWER('Laptop') AND parent_id IS NULL;

UPDATE categories
SET icon = 'Tablet', image = 'https://cdn.tgdd.vn/content/tablet-24x24.png', parent_id = NULL
WHERE LOWER(name) = LOWER('Tablet') AND parent_id IS NULL;

UPDATE categories
SET icon = 'Phụ kiện', image = 'https://cdn.tgdd.vn/content/phu-kien-24x24.png', parent_id = NULL
WHERE LOWER(name) = LOWER('Phụ kiện') AND parent_id IS NULL;

UPDATE categories
SET icon = 'Smartwatch', image = 'https://cdn.tgdd.vn/content/smartwatch-24x24.png', parent_id = NULL
WHERE LOWER(name) = LOWER('Smartwatch') AND parent_id IS NULL;

UPDATE categories
SET icon = 'Đồng hồ', image = 'https://cdn.tgdd.vn/content/watch-24x24.png', parent_id = NULL
WHERE LOWER(name) = LOWER('Đồng hồ') AND parent_id IS NULL;

-- ========================
-- CHILD CATEGORIES (LEVEL 2)
-- ========================
INSERT INTO categories (name, icon, image, parent_id)
SELECT 'Sạc dự phòng', NULL, 'https://cdn.tgdd.vn/content/sac-du-phong-24x24.png', p.id
FROM categories p
WHERE LOWER(p.name) = LOWER('Phụ kiện') AND p.parent_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM categories c
    WHERE LOWER(c.name) = LOWER('Sạc dự phòng') AND c.parent_id = p.id
  );

INSERT INTO categories (name, icon, image, parent_id)
SELECT 'Sạc, cáp', NULL, 'https://cdn.tgdd.vn/content/sac-cap-24x24.png', p.id
FROM categories p
WHERE LOWER(p.name) = LOWER('Phụ kiện') AND p.parent_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM categories c
    WHERE LOWER(c.name) = LOWER('Sạc, cáp') AND c.parent_id = p.id
  );

INSERT INTO categories (name, icon, image, parent_id)
SELECT 'Tai nghe Bluetooth', NULL, 'https://cdn.tgdd.vn/content/bluetooth-24x24.png', p.id
FROM categories p
WHERE LOWER(p.name) = LOWER('Phụ kiện') AND p.parent_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM categories c
    WHERE LOWER(c.name) = LOWER('Tai nghe Bluetooth') AND c.parent_id = p.id
  );

INSERT INTO categories (name, icon, image, parent_id)
SELECT 'Ốp lưng điện thoại', NULL, 'https://cdn.tgdd.vn/content/op-lung-24x24.png', p.id
FROM categories p
WHERE LOWER(p.name) = LOWER('Phụ kiện') AND p.parent_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM categories c
    WHERE LOWER(c.name) = LOWER('Ốp lưng điện thoại') AND c.parent_id = p.id
  );

INSERT INTO categories (name, icon, image, parent_id)
SELECT 'iPhone', NULL, 'https://cdn.tgdd.vn/content/iphone-24x24.png', p.id
FROM categories p
WHERE LOWER(p.name) = LOWER('Điện thoại') AND p.parent_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM categories c
    WHERE LOWER(c.name) = LOWER('iPhone') AND c.parent_id = p.id
  );

INSERT INTO categories (name, icon, image, parent_id)
SELECT 'Samsung', NULL, 'https://cdn.tgdd.vn/content/samsung-24x24.png', p.id
FROM categories p
WHERE LOWER(p.name) = LOWER('Điện thoại') AND p.parent_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM categories c
    WHERE LOWER(c.name) = LOWER('Samsung') AND c.parent_id = p.id
  );

-- Fix parent mapping for existing child categories
UPDATE categories c
JOIN categories p ON LOWER(p.name) = LOWER('Phụ kiện') AND p.parent_id IS NULL
SET c.parent_id = p.id, c.image = 'https://cdn.tgdd.vn/content/sac-du-phong-24x24.png'
WHERE LOWER(c.name) = LOWER('Sạc dự phòng');

UPDATE categories c
JOIN categories p ON LOWER(p.name) = LOWER('Phụ kiện') AND p.parent_id IS NULL
SET c.parent_id = p.id, c.image = 'https://cdn.tgdd.vn/content/sac-cap-24x24.png'
WHERE LOWER(c.name) = LOWER('Sạc, cáp');

UPDATE categories c
JOIN categories p ON LOWER(p.name) = LOWER('Phụ kiện') AND p.parent_id IS NULL
SET c.parent_id = p.id, c.image = 'https://cdn.tgdd.vn/content/bluetooth-24x24.png'
WHERE LOWER(c.name) = LOWER('Tai nghe Bluetooth');

UPDATE categories c
JOIN categories p ON LOWER(p.name) = LOWER('Phụ kiện') AND p.parent_id IS NULL
SET c.parent_id = p.id, c.image = 'https://cdn.tgdd.vn/content/op-lung-24x24.png'
WHERE LOWER(c.name) = LOWER('Ốp lưng điện thoại');

UPDATE categories c
JOIN categories p ON LOWER(p.name) = LOWER('Điện thoại') AND p.parent_id IS NULL
SET c.parent_id = p.id, c.image = 'https://cdn.tgdd.vn/content/iphone-24x24.png'
WHERE LOWER(c.name) = LOWER('iPhone');

UPDATE categories c
JOIN categories p ON LOWER(p.name) = LOWER('Điện thoại') AND p.parent_id IS NULL
SET c.parent_id = p.id, c.image = 'https://cdn.tgdd.vn/content/samsung-24x24.png'
WHERE LOWER(c.name) = LOWER('Samsung');

-- =========
-- PRODUCTS
-- =========
INSERT INTO products (name, price, description, image, is_promotional, category_id)
SELECT 'Samsung Galaxy S24 Ultra 12GB/256GB', 29990000,
       'Điện thoại cao cấp với camera 200MP, màn hình Dynamic AMOLED 2X 6.8 inch, chip Snapdragon 8 Gen 3',
       'https://cdn.tgdd.vn/Products/Images/42/307174/samsung-galaxy-s24-ultra-grey-thumbnew-600x600.jpg',
       1, c.id
FROM categories c
WHERE LOWER(c.name) = LOWER('Điện thoại') AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM products p WHERE LOWER(p.name) = LOWER('Samsung Galaxy S24 Ultra 12GB/256GB'));

INSERT INTO products (name, price, description, image, is_promotional, category_id)
SELECT 'iPhone 15 Pro Max 256GB', 33990000,
       'iPhone cao cấp nhất với chip A17 Pro, khung Titan, camera 48MP, màn hình Super Retina XDR 6.7 inch',
       'https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-pro-max-blue-thumbnew-600x600.jpg',
       1, c.id
FROM categories c
WHERE LOWER(c.name) = LOWER('Điện thoại') AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM products p WHERE LOWER(p.name) = LOWER('iPhone 15 Pro Max 256GB'));

INSERT INTO products (name, price, description, image, is_promotional, category_id)
SELECT 'MacBook Air 13 inch M3 8GB/256GB', 28990000,
       'Laptop siêu mỏng nhẹ với chip M3 mạnh mẽ, màn hình Liquid Retina 13.6 inch, pin 18 giờ',
       'https://cdn.tgdd.vn/Products/Images/44/329030/macbook-air-13-inch-m3-2024-gray-thumb-600x600.jpg',
       0, c.id
FROM categories c
WHERE LOWER(c.name) = LOWER('Laptop') AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM products p WHERE LOWER(p.name) = LOWER('MacBook Air 13 inch M3 8GB/256GB'));

INSERT INTO products (name, price, description, image, is_promotional, category_id)
SELECT 'Dell Inspiron 15 3520 i5 1235U/16GB/512GB', 15690000,
       'Laptop văn phòng hiệu suất cao với Intel Core i5 Gen 12, RAM 16GB, SSD 512GB, màn hình 15.6 inch Full HD',
       'https://cdn.tgdd.vn/Products/Images/44/309016/dell-inspiron-15-3520-i5-n5i5052w1-thumb-600x600.jpg',
       0, c.id
FROM categories c
WHERE LOWER(c.name) = LOWER('Laptop') AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM products p WHERE LOWER(p.name) = LOWER('Dell Inspiron 15 3520 i5 1235U/16GB/512GB'));

INSERT INTO products (name, price, description, image, is_promotional, category_id)
SELECT 'iPad Pro M4 11 inch WiFi 256GB', 28990000,
       'Tablet cao cấp với chip M4, màn hình Ultra Retina XDR 11 inch, hỗ trợ Apple Pencil Pro',
       'https://cdn.tgdd.vn/Products/Images/522/329034/ipad-pro-m4-11-inch-wifi-256gb-thumb-600x600.jpg',
       0, c.id
FROM categories c
WHERE LOWER(c.name) = LOWER('Tablet') AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM products p WHERE LOWER(p.name) = LOWER('iPad Pro M4 11 inch WiFi 256GB'));

INSERT INTO products (name, price, description, image, is_promotional, category_id)
SELECT 'Samsung Galaxy Tab S9 FE 5G 8GB/256GB', 12990000,
       'Tablet tầm trung với màn hình 10.9 inch, bút S Pen đi kèm, pin 8000mAh',
       'https://cdn.tgdd.vn/Products/Images/522/309816/samsung-galaxy-tab-s9-fe-5g-gray-thumb-600x600.jpg',
       1, c.id
FROM categories c
WHERE LOWER(c.name) = LOWER('Tablet') AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM products p WHERE LOWER(p.name) = LOWER('Samsung Galaxy Tab S9 FE 5G 8GB/256GB'));

INSERT INTO products (name, price, description, image, is_promotional, category_id)
SELECT 'Tai nghe Bluetooth AirPods Pro 2023', 6290000,
       'Tai nghe chống ồn chủ động, âm thanh Adaptive Audio, hộp sạc MagSafe, kháng nước IP54',
       'https://cdn.tgdd.vn/Products/Images/54/289780/tai-nghe-bluetooth-airpods-pro-2nd-gen-usb-c-charge-apple-thumb-1-600x600.jpg',
       1, c.id
FROM categories c
WHERE LOWER(c.name) = LOWER('Phụ kiện') AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM products p WHERE LOWER(p.name) = LOWER('Tai nghe Bluetooth AirPods Pro 2023'));

INSERT INTO products (name, price, description, image, is_promotional, category_id)
SELECT 'Sạc dự phòng 20000mAh Type-C PD 20W Anker PowerCore', 1090000,
       'Pin sạc dự phòng 20000mAh, hỗ trợ sạc nhanh PD 20W, 2 cổng output',
       'https://cdn.tgdd.vn/Products/Images/57/325416/sac-du-phong-20000mah-type-c-pd-20w-anker-powercore-essential-a1289-thumb-600x600.jpg',
       0, c.id
FROM categories c
WHERE LOWER(c.name) = LOWER('Phụ kiện') AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM products p WHERE LOWER(p.name) = LOWER('Sạc dự phòng 20000mAh Type-C PD 20W Anker PowerCore'));

INSERT INTO products (name, price, description, image, is_promotional, category_id)
SELECT 'Apple Watch Series 9 GPS 41mm', 10490000,
       'Đồng hồ thông minh với chip S9, màn hình Retina LTPO OLED, theo dõi sức khỏe toàn diện',
       'https://cdn.tgdd.vn/Products/Images/7077/313733/apple-watch-s9-lte-41mm-vien-thep-day-thep-thumb-600x600.jpg',
       0, c.id
FROM categories c
WHERE LOWER(c.name) = LOWER('Smartwatch') AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM products p WHERE LOWER(p.name) = LOWER('Apple Watch Series 9 GPS 41mm'));

INSERT INTO products (name, price, description, image, is_promotional, category_id)
SELECT 'Samsung Galaxy Watch 6 LTE 40mm', 7490000,
       'Đồng hồ thông minh Wear OS, màn hình Super AMOLED 1.3 inch, theo dõi giấc ngủ, nhịp tim',
       'https://cdn.tgdd.vn/Products/Images/7077/311627/samsung-galaxy-watch-6-lte-40mm-thumb-600x600.jpg',
       1, c.id
FROM categories c
WHERE LOWER(c.name) = LOWER('Smartwatch') AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM products p WHERE LOWER(p.name) = LOWER('Samsung Galaxy Watch 6 LTE 40mm'));

INSERT INTO products (name, price, description, image, is_promotional, category_id)
SELECT 'Đồng hồ nam Casio G-Shock GA-2100-1A1DR', 3291000,
       'Đồng hồ G-Shock thiết kế tối giản, chống nước 200m, chống sốc, pin 3 năm',
       'https://cdn.tgdd.vn/Products/Images/7264/239201/casio-g-shock-ga-2100-1a1dr-nam-thumb-1-600x600.jpg',
       0, c.id
FROM categories c
WHERE LOWER(c.name) = LOWER('Đồng hồ') AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM products p WHERE LOWER(p.name) = LOWER('Đồng hồ nam Casio G-Shock GA-2100-1A1DR'));

INSERT INTO products (name, price, description, image, is_promotional, category_id)
SELECT 'Đồng hồ nữ Casio LTP-V005D-7BUDF', 878000,
       'Đồng hồ nữ thanh lịch, mặt số trắng, dây da, chống nước 3ATM',
       'https://cdn.tgdd.vn/Products/Images/7264/235719/casio-ltp-v005d-7budf-nu-thumb-1-600x600.jpg',
       1, c.id
FROM categories c
WHERE LOWER(c.name) = LOWER('Đồng hồ') AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM products p WHERE LOWER(p.name) = LOWER('Đồng hồ nữ Casio LTP-V005D-7BUDF'));
