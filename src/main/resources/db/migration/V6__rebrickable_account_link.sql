-- App Users Rebrickable account link (PB-13).
CREATE TABLE rebrickable_account_link (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    encrypted_token VARCHAR(250) NOT NULL ,
    linked_at TIMESTAMP NOT NULL,
    last_import_at TIMESTAMP NULL,
      constraint uq_rebrick_acc_user_id UNIQUE (user_id),
      constraint del_app_user_rebrick_acc_link
       FOREIGN KEY (user_id)
        REFERENCES app_user(id)
         ON DELETE CASCADE
);