ALTER TABLE email_verifications
    ADD COLUMN used_at TIMESTAMPTZ;

CREATE INDEX idx_email_verifications_email_purpose_used
    ON email_verifications(email, purpose, used_at, created_at DESC);
