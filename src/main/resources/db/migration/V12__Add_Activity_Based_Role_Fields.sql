-- Add activity-based role detection fields to users table
ALTER TABLE users
ADD COLUMN inferred_role VARCHAR(50) DEFAULT 'new_user',
ADD COLUMN first_job_view TIMESTAMP,
ADD COLUMN first_job_post TIMESTAMP,
ADD COLUMN first_portfolio_post TIMESTAMP,
ADD COLUMN activity_score INTEGER DEFAULT 0,
ADD COLUMN last_activity TIMESTAMP;