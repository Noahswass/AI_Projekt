-- One row per catalog sync run (PB-06).
-- counts_json holds the row counts per table as JSON; it stays NULL when the run failed.
CREATE TABLE sync_run (
    id INT AUTO_INCREMENT PRIMARY KEY,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL,
    counts_json VARCHAR(1000),
    error_message VARCHAR(2000)
);
