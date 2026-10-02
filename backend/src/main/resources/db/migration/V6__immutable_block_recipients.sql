CREATE TABLE block_recipient (
 block_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 PRIMARY KEY(block_id,user_id),
 FOREIGN KEY(block_id) REFERENCES space_block(id),
 FOREIGN KEY(user_id) REFERENCES identity_user(id)
) ENGINE=InnoDB;
CREATE TABLE block_batch (
 block_id BIGINT NOT NULL,
 batch_id BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 PRIMARY KEY(block_id,batch_id),
 FOREIGN KEY(block_id) REFERENCES space_block(id),
 FOREIGN KEY(batch_id) REFERENCES preparation_batch(id)
) ENGINE=InnoDB;

-- Backfill only retained original evidence, never infer recipients from current occupancy.
INSERT INTO block_recipient(block_id,user_id,created_at)
 SELECT k.id,n.recipient_id,MIN(n.created_at)
 FROM space_block k JOIN notification_outbox n ON n.event_key=CONCAT('block:',k.id,':created')
 GROUP BY k.id,n.recipient_id;
INSERT IGNORE INTO block_batch(block_id,batch_id,created_at)
 SELECT i.block_id,t.id,k.created_at
 FROM block_impact i JOIN space_block k ON k.id=i.block_id
 JOIN preparation_batch t ON t.id=CAST(JSON_UNQUOTE(JSON_EXTRACT(i.detail_json,'$.batchId')) AS UNSIGNED)
 WHERE i.kind='POOL';
INSERT IGNORE INTO block_batch(block_id,batch_id,created_at)
 SELECT a.block_id,o.batch_id,k.created_at FROM long_temporary_arrangement a
 JOIN long_offer o ON o.id=a.offer_id JOIN space_block k ON k.id=a.block_id;
