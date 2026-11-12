CREATE TABLE theme (
id INT PRIMARY KEY,
name VARCHAR(300) NOT NULL,
parent_id INT
);

CREATE TABLE lego_colour (
id INT PRIMARY KEY,
name VARCHAR(300) NOT NULL,
rgb VARCHAR(6)
);

CREATE TABLE lego_set (
id INT AUTO_INCREMENT PRIMARY KEY,
set_num VARCHAR(50) NOT NULL,
name VARCHAR(300) NOT NULL,
release_year INT,
theme_id INT,
num_parts INT,
set_img_url VARCHAR(1000),
set_url VARCHAR(1000),
last_modified_dt TIMESTAMP,
last_sync_dt TIMESTAMP,
    constraint uq_lego_set_num UNIQUE (set_num),
    constraint fk_set_theme_id foreign key (theme_id) REFERENCES theme (id)
);