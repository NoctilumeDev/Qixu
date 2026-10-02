-- NULL preserves unknown historical requirements; never backfill from a later profile.
ALTER TABLE long_temporary_arrangement ADD required_features_json JSON NULL;
ALTER TABLE space_block ADD UNIQUE KEY uq_block_coordinates(id,space_id,floor_id);
CREATE TABLE repair_limit (
 repair_id BIGINT PRIMARY KEY,
 block_id BIGINT NOT NULL UNIQUE,
 space_id BIGINT NOT NULL,
 floor_id BIGINT NOT NULL,
 actor_id BIGINT NOT NULL,
 reason VARCHAR(500) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 FOREIGN KEY(repair_id,space_id,floor_id) REFERENCES repair_ticket(id,space_id,floor_id),
 FOREIGN KEY(block_id,space_id,floor_id) REFERENCES space_block(id,space_id,floor_id),
 FOREIGN KEY(actor_id) REFERENCES identity_user(id)
) ENGINE=InnoDB;
