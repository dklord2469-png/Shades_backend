-- ============================================================
-- Seed data for Shades_world Sunglass Store
--
-- NOT executed automatically against MySQL: spring.sql.init.mode
-- is unset, and Spring Boot's default ("embedded") only runs this
-- file on an in-memory database — the test profile then disables
-- even that. Run it by hand when preparing a database:
--
--     mysql -u<user> -p -D <schema> < src/main/resources/data.sql
--
-- Idempotent throughout: INSERT IGNORE against the tables' unique
-- keys, so re-running changes nothing.
--
-- Every statement matches the REAL schema and the live catalogue
-- vocabulary. An earlier version of this file had drifted: it
-- named columns that do not exist (TAX_RATES.state_code,
-- CONFIG.config_key) and a permission vocabulary
-- (PRODUCT_CREATE, ORDER_VIEW_ALL, ...) that no database ever
-- held — so running it failed with error 1054 halfway through,
-- which is why TAX_RATES and CONFIG sat empty everywhere.
-- ============================================================

-- Roles
INSERT IGNORE INTO ROLES (role_name, description) VALUES ('CUSTOMER', 'Default customer role');
INSERT IGNORE INTO ROLES (role_name, description) VALUES ('ADMIN', 'Full administrative access');
INSERT IGNORE INTO ROLES (role_name, description) VALUES ('SUPPORT', 'Customer support team');
INSERT IGNORE INTO ROLES (role_name, description) VALUES ('INVENTORY_MANAGER', 'Manages product inventory');

-- Permissions — the vocabulary the live databases hold. Nothing in the code checks these by name
-- today (authorisation is role-based), but they load into each principal's authorities, so the
-- names must stay consistent across environments.
INSERT IGNORE INTO PERMISSIONS (permission_name, description) VALUES ('PRODUCT_READ', 'View products');
INSERT IGNORE INTO PERMISSIONS (permission_name, description) VALUES ('PRODUCT_WRITE', 'Create and update products');
INSERT IGNORE INTO PERMISSIONS (permission_name, description) VALUES ('PRODUCT_DELETE', 'Delete products');
INSERT IGNORE INTO PERMISSIONS (permission_name, description) VALUES ('ORDER_CREATE', 'Place an order');
INSERT IGNORE INTO PERMISSIONS (permission_name, description) VALUES ('ORDER_READ_OWN', 'View own orders');
INSERT IGNORE INTO PERMISSIONS (permission_name, description) VALUES ('ORDER_READ_ALL', 'View every order');
INSERT IGNORE INTO PERMISSIONS (permission_name, description) VALUES ('ORDER_UPDATE', 'Update order status');
INSERT IGNORE INTO PERMISSIONS (permission_name, description) VALUES ('USER_READ', 'View user details');
INSERT IGNORE INTO PERMISSIONS (permission_name, description) VALUES ('USER_UPDATE', 'Update user details');
INSERT IGNORE INTO PERMISSIONS (permission_name, description) VALUES ('RETURN_CREATE', 'Request a return');
INSERT IGNORE INTO PERMISSIONS (permission_name, description) VALUES ('RETURN_MANAGE', 'Approve or reject returns');
INSERT IGNORE INTO PERMISSIONS (permission_name, description) VALUES ('INVENTORY_MANAGE', 'Manage inventory');

-- Role-Permission mappings, reproducing the live assignments exactly.
-- ADMIN gets everything.
INSERT IGNORE INTO ROLE_PERMISSIONS (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM ROLES r, PERMISSIONS p WHERE r.role_name = 'ADMIN';

-- CUSTOMER: the self-service permissions.
INSERT IGNORE INTO ROLE_PERMISSIONS (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM ROLES r, PERMISSIONS p
WHERE r.role_name = 'CUSTOMER' AND p.permission_name IN (
    'PRODUCT_READ', 'ORDER_CREATE', 'ORDER_READ_OWN', 'RETURN_CREATE', 'USER_UPDATE'
);

-- SUPPORT: order handling and returns across all customers.
INSERT IGNORE INTO ROLE_PERMISSIONS (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM ROLES r, PERMISSIONS p
WHERE r.role_name = 'SUPPORT' AND p.permission_name IN (
    'PRODUCT_READ', 'ORDER_READ_ALL', 'ORDER_UPDATE', 'RETURN_MANAGE', 'USER_READ'
);

-- INVENTORY_MANAGER: catalogue and stock.
INSERT IGNORE INTO ROLE_PERMISSIONS (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM ROLES r, PERMISSIONS p
WHERE r.role_name = 'INVENTORY_MANAGER' AND p.permission_name IN (
    'PRODUCT_READ', 'PRODUCT_WRITE', 'INVENTORY_MANAGE'
);

-- Storefront categories — the same four StorefrontCategoryBootstrap upserts at startup (it matches
-- by name, so these coexist with it). The old sample style categories (Aviator, Wayfarer, ...) are
-- gone: ProductServiceImpl.validateStorefrontCategory refuses anything but these four, so seeding
-- the others only created categories no product could ever use.
INSERT IGNORE INTO CATEGORIES (category_name, description, is_active) VALUES ('Men', 'Eyewear designed for men', true);
INSERT IGNORE INTO CATEGORIES (category_name, description, is_active) VALUES ('Women', 'Eyewear designed for women', true);
INSERT IGNORE INTO CATEGORIES (category_name, description, is_active) VALUES ('Unisex', 'Eyewear designed for everyone', true);
INSERT IGNORE INTO CATEGORIES (category_name, description, is_active) VALUES ('Accessory', 'Eyewear accessories and care products', true);

-- Tax rates, matching the REAL columns (TAX_NAME/COUNTRY/STATE/RATE_PERCENT). The application
-- currently charges GST through code, not this table — PRODUCTS.TAX_RATE_ID may reference a row
-- here, so the standard rate is provided for that use.
-- NOT EXISTS rather than INSERT IGNORE: TAX_RATES has no unique key, so IGNORE would insert a
-- duplicate row on every re-run instead of skipping.
INSERT INTO TAX_RATES (tax_name, country, state, rate_percent, is_active, valid_from)
SELECT 'GST 18%', 'India', NULL, 18.0000, true, '2026-01-01 00:00:00' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM TAX_RATES WHERE TAX_NAME = 'GST 18%');

-- App config, matching the REAL columns (CONFIG_SHORT_CODE, not config_key). Documentation of the
-- operating values: the application reads these from constants today (see offer/OrderTotals.java),
-- so editing a row does not change behaviour — change the constant too.
INSERT IGNORE INTO CONFIG (config_short_code, config_value, description)
VALUES ('FREE_SHIPPING_THRESHOLD', '500.00', 'Minimum order amount for free shipping');
INSERT IGNORE INTO CONFIG (config_short_code, config_value, description)
VALUES ('STANDARD_SHIPPING_RATE', '49.00', 'Standard shipping cost');
INSERT IGNORE INTO CONFIG (config_short_code, config_value, description)
VALUES ('MAX_CART_ITEMS', '20', 'Maximum items allowed in cart');
INSERT IGNORE INTO CONFIG (config_short_code, config_value, description)
VALUES ('RETURN_WINDOW_DAYS', '15', 'Number of days allowed for returns');
