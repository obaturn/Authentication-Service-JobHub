CREATE TABLE IF NOT EXISTS educations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    institution_name VARCHAR(200) NOT NULL,
    degree VARCHAR(100) NOT NULL,
    field_of_study VARCHAR(200),
    location VARCHAR(200),
    start_date TIMESTAMP WITH TIME ZONE,
    end_date TIMESTAMP WITH TIME ZONE,
    is_current BOOLEAN DEFAULT FALSE,
    description TEXT,
    gpa DOUBLE PRECISION,
    honors VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_education_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_educations_user_id ON educations(user_id);
