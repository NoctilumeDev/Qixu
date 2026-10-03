CREATE TABLE recovery_generation (
 id TINYINT PRIMARY KEY,
 generation CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 baseline_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 adopted_existing BOOLEAN NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT chk_single_recovery_generation CHECK (id=1)
);
CREATE TABLE recovery_marker (
 transaction_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 generation CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_marker_generation FOREIGN KEY(generation) REFERENCES recovery_generation(generation)
);
