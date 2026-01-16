ALTER TABLE users
ADD COLUMN email_verification_token VARCHAR(255),
ADD COLUMN email_verification_expires_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN password_reset_token VARCHAR(255),
ADD COLUMN password_reset_expires_at TIMESTAMP WITH TIME ZONE;
