-- Run only after migrating any legacy guide relations out of phu_hieu / huong_dan_phu_hieu.
-- Current runtime no longer depends on these tables, but old DB rows may still exist.
DROP TABLE IF EXISTS huong_dan_phu_hieu;
DROP TABLE IF EXISTS phu_hieu;
