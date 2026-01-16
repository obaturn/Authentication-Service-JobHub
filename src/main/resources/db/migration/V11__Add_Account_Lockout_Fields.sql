-- Add account lockout and failed login tracking fields to users table
ALTER TABLE users
ADD COLUMN failed_login_attempts INTEGER DEFAULT 0,
ADD COLUMN account_locked_until TIMESTAMP,
ADD COLUMN last_failed_attempt_at TIMESTAMP;

-- Add index for efficient lockout queries
CREATE INDEX idx_users_account_locked_until ON users(account_locked_until) WHERE account_locked_until IS NOT NULL;

-- Add comment for documentation
COMMENT ON COLUMN users.failed_login_attempts IS 'Number of consecutive failed login attempts';
COMMENT ON COLUMN users.account_locked_until IS 'Timestamp until which the account is locked due to failed attempts';
COMMENT ON COLUMN users.last_failed_attempt_at IS 'Timestamp of the last failed login attempt';