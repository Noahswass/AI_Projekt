-- Parts and minifigs from Rebrickable (PB-06). Same pattern as lego_set:
-- generated id plus a unique natural key from Rebrickable.
CREATE TABLE part (
    id INT AUTO_INCREMENT PRIMARY KEY,
    part_num VARCHAR(50) NOT NULL,
    name VARCHAR(500) NOT NULL,
    part_cat_id INT,
    part_img_url VARCHAR(1000),
    part_url VARCHAR(1000),
    last_sync_dt TIMESTAMP NULL,
    constraint uq_part_num UNIQUE (part_num)
);

CREATE TABLE minifig (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fig_num VARCHAR(50) NOT NULL,
    name VARCHAR(500) NOT NULL,
    num_parts INT,
    img_url VARCHAR(1000),
    last_sync_dt TIMESTAMP NULL,
    constraint uq_minifig_fig_num UNIQUE (fig_num)
);
