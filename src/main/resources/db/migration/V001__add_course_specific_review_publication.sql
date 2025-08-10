-- Add table for course-specific review publication tracking
CREATE TABLE review_course_publication (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id UUID NOT NULL,
    course_id UUID NOT NULL,
    published_by_id VARCHAR(255) NOT NULL,
    published_at TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (review_id) REFERENCES review(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    FOREIGN KEY (published_by_id) REFERENCES "user"(id) ON DELETE CASCADE,
    UNIQUE(review_id, course_id)
);

-- Remove the global publication columns from review table  
ALTER TABLE review DROP COLUMN IF EXISTS is_published;
ALTER TABLE review DROP COLUMN IF EXISTS published_at;

-- Create indexes for better performance
CREATE INDEX idx_review_course_publication_review_id ON review_course_publication(review_id);
CREATE INDEX idx_review_course_publication_course_id ON review_course_publication(course_id);
CREATE INDEX idx_review_course_publication_published_by_id ON review_course_publication(published_by_id);
