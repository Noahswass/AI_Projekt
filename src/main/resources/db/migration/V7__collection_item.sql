CREATE TABLE collection_item
(
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT          NOT NULL,
    item_type VARCHAR(10) NOT NULL,
    item_ref VARCHAR(50) NOT NULL,
    color_id INT NOT NULL,
    quantity  INT NOT NULL,
    notes VARCHAR(200),
    constraint uq_collection_item UNIQUE (user_id, item_type, item_ref, color_id),
    constraint fk_collection_item_user
        FOREIGN KEY (user_id)
            REFERENCES app_user (id)
            ON DELETE CASCADE
);