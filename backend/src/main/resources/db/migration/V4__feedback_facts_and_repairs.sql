CREATE TABLE feedback_report (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 user_id BIGINT NOT NULL,
 space_id BIGINT NOT NULL,
 floor_id BIGINT NOT NULL,
 category VARCHAR(20) NOT NULL,
 description VARCHAR(2000) NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'REPORTED',
 version BIGINT NOT NULL DEFAULT 1,
 decision_note VARCHAR(500) NOT NULL DEFAULT '',
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 FOREIGN KEY(user_id) REFERENCES identity_user(id),
 FOREIGN KEY(space_id,floor_id) REFERENCES space(id,floor_id),
 UNIQUE KEY uq_feedback_coordinates(id,space_id,floor_id),
 INDEX ix_feedback_scope(floor_id,status,created_at),
 INDEX ix_feedback_person(user_id,created_at),
 CHECK(category IN ('OUTLET','LIGHT','DESK','ENVIRONMENT','INFORMATION','OTHER')),
 CHECK(status IN ('REPORTED','ACKNOWLEDGED','VERIFIED','IN_REPAIR','RESOLVED','NOT_REPRODUCED','REJECTED'))
) ENGINE=InnoDB;
CREATE TABLE feedback_history (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 report_id BIGINT NOT NULL,
 actor_id BIGINT NOT NULL,
 action VARCHAR(24) NOT NULL,
 message VARCHAR(2000) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 FOREIGN KEY(report_id) REFERENCES feedback_report(id),
 FOREIGN KEY(actor_id) REFERENCES identity_user(id),
 INDEX ix_feedback_history(report_id,id),
 CHECK(action IN ('REPORTED','SUPPLEMENT','REOPENED','ACKNOWLEDGED','VERIFIED','NOT_REPRODUCED','REJECTED','REPAIR_LINKED','REPAIR_DONE','RESOLVED'))
) ENGINE=InnoDB;
CREATE TABLE feedback_attachment (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 report_id BIGINT NOT NULL,
 media_type VARCHAR(20) CHARACTER SET ascii NOT NULL,
 sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 content LONGBLOB NOT NULL,
 width INT NOT NULL,
 height INT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 FOREIGN KEY(report_id) REFERENCES feedback_report(id),
 CHECK(media_type IN ('image/png','image/jpeg')),
 CHECK(OCTET_LENGTH(content)>0 AND OCTET_LENGTH(content)<=1048576),
 CHECK(width BETWEEN 1 AND 8192 AND height BETWEEN 1 AND 8192 AND width*height<=4000000)
) ENGINE=InnoDB;
CREATE TABLE repair_ticket (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 space_id BIGINT NOT NULL,
 floor_id BIGINT NOT NULL,
 category VARCHAR(20) NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
 description VARCHAR(2000) NOT NULL,
 assignee VARCHAR(100) NOT NULL DEFAULT '',
 verification_note VARCHAR(500) NOT NULL DEFAULT '',
 version BIGINT NOT NULL DEFAULT 1,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 active_space BIGINT GENERATED ALWAYS AS (CASE WHEN status<>'VERIFIED_CLOSED' THEN space_id ELSE NULL END) STORED,
 UNIQUE KEY uq_open_repair(active_space,category),
 UNIQUE KEY uq_repair_coordinates(id,space_id,floor_id),
 FOREIGN KEY(space_id,floor_id) REFERENCES space(id,floor_id),
 INDEX ix_repair_scope(floor_id,status,created_at),
 CHECK(category IN ('OUTLET','LIGHT','DESK','ENVIRONMENT','INFORMATION','OTHER')),
 CHECK(status IN ('OPEN','ASSIGNED','WORK_DONE','VERIFIED_CLOSED'))
) ENGINE=InnoDB;
CREATE TABLE repair_report (
 repair_id BIGINT NOT NULL,
 report_id BIGINT NOT NULL,
 space_id BIGINT NOT NULL,
 floor_id BIGINT NOT NULL,
 PRIMARY KEY(repair_id,report_id),
 FOREIGN KEY(repair_id,space_id,floor_id) REFERENCES repair_ticket(id,space_id,floor_id),
 FOREIGN KEY(report_id,space_id,floor_id) REFERENCES feedback_report(id,space_id,floor_id)
) ENGINE=InnoDB;
CREATE TABLE repair_history (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 repair_id BIGINT NOT NULL,
 actor_id BIGINT NOT NULL,
 action VARCHAR(24) NOT NULL,
 message VARCHAR(2000) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 FOREIGN KEY(repair_id) REFERENCES repair_ticket(id),
 FOREIGN KEY(actor_id) REFERENCES identity_user(id),
 INDEX ix_repair_history(repair_id,id),
 CHECK(action IN ('OPENED','ASSIGNED','WORK_DONE','VERIFICATION_FAILED','VERIFIED_CLOSED','REPORT_LINKED'))
) ENGINE=InnoDB;
CREATE TABLE space_fact (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 space_id BIGINT NOT NULL,
 floor_id BIGINT NOT NULL,
 feature_key VARCHAR(24) NOT NULL,
 value_json JSON NOT NULL,
 report_id BIGINT NULL,
 repair_id BIGINT NULL,
 actor_id BIGINT NOT NULL,
 reason VARCHAR(500) NOT NULL,
 verified_at DATETIME(6) NOT NULL,
 FOREIGN KEY(space_id,floor_id) REFERENCES space(id,floor_id),
 FOREIGN KEY(report_id,space_id,floor_id) REFERENCES feedback_report(id,space_id,floor_id),
 FOREIGN KEY(repair_id,space_id,floor_id) REFERENCES repair_ticket(id,space_id,floor_id),
 FOREIGN KEY(actor_id) REFERENCES identity_user(id),
 INDEX ix_space_fact(space_id,feature_key,id),
 CHECK(feature_key IN ('window','outlet','quiet','accessible','outletCondition','lightCondition','deskCondition','environmentCondition')),
 CHECK((report_id IS NOT NULL AND repair_id IS NULL) OR (report_id IS NULL AND repair_id IS NOT NULL))
) ENGINE=InnoDB;
