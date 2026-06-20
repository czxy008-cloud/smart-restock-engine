-- ============================================================
-- 智能补货决策支持系统 - 数据库初始化脚本
-- 适用数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS smart_restock DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE smart_restock;

-- ============================================================
-- 1. 商品基础信息表
-- 存储门店商品的主数据，包括SKU、品类、供应商关联及价格信息
-- ============================================================
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sku_code`        VARCHAR(64)  NOT NULL                COMMENT '商品SKU编码，唯一标识商品',
    `product_name`    VARCHAR(200) NOT NULL                COMMENT '商品名称，如"康师傅红烧牛肉面"',
    `category_code`   VARCHAR(32)  NOT NULL                COMMENT '品类编码，如"FOOD"食品/"DRINK"饮料/"DAILY"日用',
    `category_name`   VARCHAR(64)  NOT NULL                COMMENT '品类名称，用于前端展示',
    `unit`            VARCHAR(16)  NOT NULL DEFAULT '件'    COMMENT '计量单位，如件/箱/瓶/公斤',
    `purchase_price`  DECIMAL(10,2) NOT NULL DEFAULT 0.00  COMMENT '采购价（元），供应商供货价',
    `retail_price`    DECIMAL(10,2) NOT NULL DEFAULT 0.00  COMMENT '零售价（元），门店销售价',
    `supplier_code`   VARCHAR(64)  NOT NULL                COMMENT '默认供应商编码，关联供应商',
    `supplier_name`   VARCHAR(200) NOT NULL                COMMENT '供应商名称，冗余存储方便展示',
    `store_code`      VARCHAR(32)  NOT NULL                COMMENT '所属门店编码，支持多门店隔离',
    `store_name`      VARCHAR(200) NOT NULL                COMMENT '门店名称，冗余存储方便展示',
    `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '商品状态: 1-在售 0-停售',
    `created_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_store` (`sku_code`, `store_code`),
    KEY `idx_category` (`category_code`),
    KEY `idx_supplier` (`supplier_code`),
    KEY `idx_store` (`store_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品基础信息表-存储SKU主数据及供应商关联';

-- ============================================================
-- 2. 库存流水表
-- 记录每次库存变动的明细流水，支持入库/出库/调拨/盘点等类型
-- ============================================================
DROP TABLE IF EXISTS `t_inventory_transaction`;
CREATE TABLE `t_inventory_transaction` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `transaction_no`  VARCHAR(64)   NOT NULL                COMMENT '流水单号，唯一标识本次库存变动',
    `sku_code`        VARCHAR(64)   NOT NULL                COMMENT '商品SKU编码',
    `store_code`      VARCHAR(32)   NOT NULL                COMMENT '门店编码',
    `transaction_type` TINYINT      NOT NULL                COMMENT '变动类型: 1-采购入库 2-销售出库 3-调拨入 4-调拨出 5-盘盈 6-盘亏',
    `quantity`        INT           NOT NULL                COMMENT '变动数量，正数表示入库，负数表示出库',
    `before_quantity` INT           NOT NULL                COMMENT '变动前库存数量',
    `after_quantity`  INT           NOT NULL                COMMENT '变动后库存数量',
    `reference_no`    VARCHAR(64)   DEFAULT NULL            COMMENT '关联单号，如采购单号/销售单号',
    `operator`        VARCHAR(64)   DEFAULT NULL            COMMENT '操作人',
    `remark`          VARCHAR(500)  DEFAULT NULL            COMMENT '备注信息',
    `created_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_transaction_no` (`transaction_no`),
    KEY `idx_sku_store` (`sku_code`, `store_code`),
    KEY `idx_type` (`transaction_type`),
    KEY `idx_created` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表-记录每次库存变动明细';

-- ============================================================
-- 3. 销售记录表
-- 记录门店每日商品销售汇总数据，用于销售预测引擎的输入
-- ============================================================
DROP TABLE IF EXISTS `t_sales_record`;
CREATE TABLE `t_sales_record` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sku_code`        VARCHAR(64)   NOT NULL                COMMENT '商品SKU编码',
    `product_name`    VARCHAR(200)  NOT NULL                COMMENT '商品名称，冗余存储',
    `store_code`      VARCHAR(32)   NOT NULL                COMMENT '门店编码',
    `store_name`      VARCHAR(200)  NOT NULL                COMMENT '门店名称，冗余存储',
    `category_code`   VARCHAR(32)   NOT NULL                COMMENT '品类编码',
    `sales_date`      DATE          NOT NULL                COMMENT '销售日期',
    `sales_quantity`  INT           NOT NULL DEFAULT 0      COMMENT '销售数量（件）',
    `sales_amount`    DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '销售金额（元）',
    `created_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_store_date` (`sku_code`, `store_code`, `sales_date`),
    KEY `idx_store_date` (`store_code`, `sales_date`),
    KEY `idx_category_date` (`category_code`, `sales_date`),
    KEY `idx_date` (`sales_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售记录表-每日商品销售汇总，用于预测引擎输入';

-- ============================================================
-- 4. 补货规则表
-- 定义每个商品在指定门店的补货参数，控制预警阈值和补货量计算
-- ============================================================
DROP TABLE IF EXISTS `t_restock_rule`;
CREATE TABLE `t_restock_rule` (
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sku_code`               VARCHAR(64)  NOT NULL                COMMENT '商品SKU编码',
    `store_code`             VARCHAR(32)  NOT NULL                COMMENT '门店编码',
    `safety_stock_days`      INT          NOT NULL DEFAULT 7      COMMENT '安全库存天数，库存需覆盖的天数',
    `reorder_point`          INT          NOT NULL DEFAULT 0      COMMENT '补货点（件），低于此值触发预警',
    `max_stock_days`         INT          NOT NULL DEFAULT 30     COMMENT '最大库存天数，防止过量补货',
    `order_cycle_days`       INT          NOT NULL DEFAULT 7      COMMENT '补货周期（天），两次补货间隔',
    `moving_avg_window`      INT          NOT NULL DEFAULT 14     COMMENT '移动平均窗口期（天），用于销量预测的历史天数',
    `seasonal_factor`        DECIMAL(5,2) NOT NULL DEFAULT 1.00   COMMENT '季节性因子，1.0为正常>1为旺季<1为淡季',
    `seasonal_factor_month`  TINYINT      DEFAULT NULL            COMMENT '季节性因子适用月份(1-12)，NULL表示全年通用',
    `min_order_quantity`     INT          NOT NULL DEFAULT 1      COMMENT '最小起订量（件）',
    `order_quantity_multiple` INT         NOT NULL DEFAULT 1      COMMENT '订货倍数，如整箱订货时为每箱数量',
    `lead_time_days`         INT          NOT NULL DEFAULT 3      COMMENT '供应商交货提前期（天）',
    `status`                 TINYINT      NOT NULL DEFAULT 1      COMMENT '规则状态: 1-启用 0-停用',
    `created_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_store` (`sku_code`, `store_code`),
    KEY `idx_store` (`store_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补货规则表-定义商品补货参数，控制预警阈值与补货量';

-- ============================================================
-- 5. 实时库存汇总表
-- 由库存流水表聚合得出，记录每个商品在每家门店的当前库存
-- ============================================================
DROP TABLE IF EXISTS `t_inventory_summary`;
CREATE TABLE `t_inventory_summary` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sku_code`        VARCHAR(64)  NOT NULL                COMMENT '商品SKU编码',
    `product_name`    VARCHAR(200) NOT NULL                COMMENT '商品名称，冗余存储',
    `store_code`      VARCHAR(32)  NOT NULL                COMMENT '门店编码',
    `store_name`      VARCHAR(200) NOT NULL                COMMENT '门店名称，冗余存储',
    `category_code`   VARCHAR(32)  NOT NULL                COMMENT '品类编码',
    `current_quantity` INT         NOT NULL DEFAULT 0      COMMENT '当前库存数量（件）',
    `safety_quantity`  INT         NOT NULL DEFAULT 0      COMMENT '安全库存量（件），从补货规则计算得出',
    `alert_status`    TINYINT      NOT NULL DEFAULT 0      COMMENT '预警状态: 0-正常(绿灯) 1-预警(黄灯) 2-紧急(红灯)',
    `last_restock_date` DATE       DEFAULT NULL            COMMENT '最近补货日期',
    `updated_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_store` (`sku_code`, `store_code`),
    KEY `idx_store` (`store_code`),
    KEY `idx_category` (`category_code`),
    KEY `idx_alert` (`alert_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时库存汇总表-记录商品当前库存与预警状态';

-- ============================================================
-- 6. 采购订单主表
-- 记录采购订单的基本信息，支持按供应商维度合并
-- ============================================================
DROP TABLE IF EXISTS `t_purchase_order`;
CREATE TABLE `t_purchase_order` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`        VARCHAR(64)   NOT NULL                COMMENT '采购订单号，系统自动生成',
    `supplier_code`   VARCHAR(64)   NOT NULL                COMMENT '供应商编码',
    `supplier_name`   VARCHAR(200)  NOT NULL                COMMENT '供应商名称',
    `store_code`      VARCHAR(32)   NOT NULL                COMMENT '收货门店编码',
    `store_name`      VARCHAR(200)  NOT NULL                COMMENT '收货门店名称',
    `order_status`    TINYINT       NOT NULL DEFAULT 0      COMMENT '订单状态: 0-草稿 1-已提交 2-部分到货 3-已完成 4-已取消',
    `total_amount`    DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '订单总金额（元）',
    `total_quantity`  INT           NOT NULL DEFAULT 0      COMMENT '订单总数量（件）',
    `order_date`      DATE          NOT NULL                COMMENT '下单日期',
    `expected_date`   DATE          DEFAULT NULL            COMMENT '预计到货日期',
    `buyer`           VARCHAR(64)   DEFAULT NULL            COMMENT '采购员',
    `remark`          VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
    `created_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_supplier` (`supplier_code`),
    KEY `idx_store` (`store_code`),
    KEY `idx_status` (`order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单主表-记录采购订单基本信息';

-- ============================================================
-- 7. 采购订单明细表
-- 记录采购订单中的商品明细，关联采购订单主表
-- ============================================================
DROP TABLE IF EXISTS `t_purchase_order_item`;
CREATE TABLE `t_purchase_order_item` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`        VARCHAR(64)   NOT NULL                COMMENT '采购订单号，关联主表',
    `sku_code`        VARCHAR(64)   NOT NULL                COMMENT '商品SKU编码',
    `product_name`    VARCHAR(200)  NOT NULL                COMMENT '商品名称',
    `category_code`   VARCHAR(32)   NOT NULL                COMMENT '品类编码',
    `quantity`        INT           NOT NULL DEFAULT 0      COMMENT '订购数量（件）',
    `purchase_price`  DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '采购单价（元）',
    `amount`          DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '采购金额（元）= 数量 * 单价',
    `received_quantity` INT         NOT NULL DEFAULT 0      COMMENT '已收货数量（件）',
    `remark`          VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
    `created_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_sku` (`sku_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单明细表-记录采购订单中的商品明细';

-- ============================================================
-- 插入示例数据 - 门店
-- ============================================================
INSERT INTO `t_product` (`sku_code`, `product_name`, `category_code`, `category_name`, `unit`, `purchase_price`, `retail_price`, `supplier_code`, `supplier_name`, `store_code`, `store_name`) VALUES
('SKU001', '康师傅红烧牛肉面5连包', 'FOOD', '食品', '件', 9.50, 14.90, 'SUP001', '顶新国际供应链', 'STORE001', '朝阳路旗舰店'),
('SKU002', '农夫山泉550ml', 'DRINK', '饮料', '瓶', 1.20, 2.00, 'SUP002', '农夫山泉华东分公司', 'STORE001', '朝阳路旗舰店'),
('SKU003', '蓝月亮洗衣液1kg', 'DAILY', '日用', '件', 15.00, 29.90, 'SUP003', '蓝月亮商贸有限公司', 'STORE001', '朝阳路旗舰店'),
('SKU004', '蒙牛纯牛奶250ml*12', 'DRINK', '饮料', '箱', 32.00, 49.90, 'SUP002', '农夫山泉华东分公司', 'STORE001', '朝阳路旗舰店'),
('SKU005', '奥利奥饼干97g', 'FOOD', '食品', '件', 4.50, 7.90, 'SUP001', '顶新国际供应链', 'STORE001', '朝阳路旗舰店'),
('SKU001', '康师傅红烧牛肉面5连包', 'FOOD', '食品', '件', 9.50, 14.90, 'SUP001', '顶新国际供应链', 'STORE002', '望京社区店'),
('SKU002', '农夫山泉550ml', 'DRINK', '饮料', '瓶', 1.20, 2.00, 'SUP002', '农夫山泉华东分公司', 'STORE002', '望京社区店'),
('SKU003', '蓝月亮洗衣液1kg', 'DAILY', '日用', '件', 15.00, 29.90, 'SUP003', '蓝月亮商贸有限公司', 'STORE002', '望京社区店'),
('SKU006', '维达抽纸3连包', 'DAILY', '日用', '件', 8.00, 12.90, 'SUP003', '蓝月亮商贸有限公司', 'STORE002', '望京社区店'),
('SKU007', '三只松鼠坚果礼包', 'FOOD', '食品', '件', 45.00, 69.90, 'SUP001', '顶新国际供应链', 'STORE002', '望京社区店');

-- ============================================================
-- 插入示例数据 - 补货规则
-- ============================================================
INSERT INTO `t_restock_rule` (`sku_code`, `store_code`, `safety_stock_days`, `reorder_point`, `max_stock_days`, `order_cycle_days`, `moving_avg_window`, `seasonal_factor`, `seasonal_factor_month`, `min_order_quantity`, `order_quantity_multiple`, `lead_time_days`) VALUES
('SKU001', 'STORE001', 7, 50, 30, 7, 14, 1.00, NULL, 10, 5, 3),
('SKU002', 'STORE001', 5, 200, 20, 5, 14, 1.20, 7, 50, 24, 2),
('SKU003', 'STORE001', 10, 20, 45, 14, 14, 0.90, 2, 5, 1, 5),
('SKU004', 'STORE001', 7, 30, 30, 7, 14, 1.10, NULL, 6, 1, 3),
('SKU005', 'STORE001', 5, 40, 20, 7, 14, 1.00, NULL, 20, 10, 2),
('SKU001', 'STORE002', 7, 30, 30, 7, 14, 1.00, NULL, 10, 5, 3),
('SKU002', 'STORE002', 5, 100, 20, 5, 14, 1.20, 7, 50, 24, 2),
('SKU003', 'STORE002', 10, 15, 45, 14, 14, 0.90, 2, 5, 1, 5),
('SKU006', 'STORE002', 7, 25, 30, 7, 14, 1.00, NULL, 10, 1, 3),
('SKU007', 'STORE002', 10, 10, 45, 14, 14, 1.50, 1, 5, 1, 5);

-- ============================================================
-- 插入示例数据 - 实时库存汇总
-- ============================================================
INSERT INTO `t_inventory_summary` (`sku_code`, `product_name`, `store_code`, `store_name`, `category_code`, `current_quantity`, `safety_quantity`, `alert_status`) VALUES
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', 45, 50, 2),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', 180, 200, 1),
('SKU003', '蓝月亮洗衣液1kg', 'STORE001', '朝阳路旗舰店', 'DAILY', 35, 20, 0),
('SKU004', '蒙牛纯牛奶250ml*12', 'STORE001', '朝阳路旗舰店', 'DRINK', 25, 30, 2),
('SKU005', '奥利奥饼干97g', 'STORE001', '朝阳路旗舰店', 'FOOD', 60, 40, 0),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE002', '望京社区店', 'FOOD', 28, 30, 1),
('SKU002', '农夫山泉550ml', 'STORE002', '望京社区店', 'DRINK', 150, 100, 0),
('SKU003', '蓝月亮洗衣液1kg', 'STORE002', '望京社区店', 'DAILY', 12, 15, 2),
('SKU006', '维达抽纸3连包', 'STORE002', '望京社区店', 'DAILY', 40, 25, 0),
('SKU007', '三只松鼠坚果礼包', 'STORE002', '望京社区店', 'FOOD', 8, 10, 2);

-- ============================================================
-- 插入示例数据 - 销售记录（近30天历史数据，用于移动平均预测）
-- ============================================================
INSERT INTO `t_sales_record` (`sku_code`, `product_name`, `store_code`, `store_name`, `category_code`, `sales_date`, `sales_quantity`, `sales_amount`) VALUES
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-05-22', 12, 178.80),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-05-23', 15, 223.50),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-05-24', 18, 268.20),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-05-25', 20, 298.00),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-05-26', 14, 208.60),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-05-27', 11, 163.90),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-05-28', 13, 193.70),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-05-29', 16, 238.40),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-05-30', 19, 283.10),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-05-31', 22, 327.80),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-01', 17, 253.30),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-02', 10, 149.00),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-03', 14, 208.60),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-04', 16, 238.40),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-05', 21, 312.90),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-06', 24, 357.60),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-07', 19, 283.10),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-08', 15, 223.50),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-09', 12, 178.80),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-10', 18, 268.20),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-11', 20, 298.00),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-12', 23, 342.70),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-13', 17, 253.30),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-14', 14, 208.60),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-15', 11, 163.90),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-16', 16, 238.40),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-17', 19, 283.10),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-18', 22, 327.80),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-19', 15, 223.50),
('SKU001', '康师傅红烧牛肉面5连包', 'STORE001', '朝阳路旗舰店', 'FOOD', '2026-06-20', 18, 268.20),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', '2026-06-07', 85, 170.00),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', '2026-06-08', 92, 184.00),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', '2026-06-09', 78, 156.00),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', '2026-06-10', 95, 190.00),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', '2026-06-11', 88, 176.00),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', '2026-06-12', 102, 204.00),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', '2026-06-13', 90, 180.00),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', '2026-06-14', 82, 164.00),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', '2026-06-15', 75, 150.00),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', '2026-06-16', 98, 196.00),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', '2026-06-17', 105, 210.00),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', '2026-06-18', 110, 220.00),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', '2026-06-19', 88, 176.00),
('SKU002', '农夫山泉550ml', 'STORE001', '朝阳路旗舰店', 'DRINK', '2026-06-20', 95, 190.00);
