ALTER TABLE identity_user ADD COLUMN local_login_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE external_identity
 ADD COLUMN active BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN version BIGINT NOT NULL DEFAULT 1,
 ADD COLUMN issuer_hash CHAR(64) NULL;
ALTER TABLE auth_session
 ADD COLUMN external_binding_id BIGINT NULL,
 ADD COLUMN external_binding_version BIGINT NULL,
 ADD COLUMN external_issuer_hash CHAR(64) NULL,
 ADD COLUMN external_subject VARCHAR(128) NULL,
 ADD COLUMN external_ticket_cipher TEXT NULL,
 ADD CONSTRAINT fk_session_external_binding FOREIGN KEY(external_binding_id) REFERENCES external_identity(id),
 ADD CONSTRAINT ck_external_session_coordinates CHECK (
 (external_binding_id IS NULL AND external_binding_version IS NULL AND external_issuer_hash IS NULL AND external_subject IS NULL AND external_ticket_cipher IS NULL)
 OR (external_binding_id IS NOT NULL AND external_binding_version IS NOT NULL AND external_issuer_hash IS NOT NULL AND external_subject IS NOT NULL AND external_ticket_cipher IS NOT NULL));
