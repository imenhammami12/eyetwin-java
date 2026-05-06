-- Migration: Fix guide_video table AUTO_INCREMENT
-- Issue: Field 'id' doesn't have a default value (Error Code: 1364)
-- Solution: Add AUTO_INCREMENT to the id column (which is already PRIMARY KEY)
-- 
-- Run this migration using:
-- mysql -u root -p eyetwin_platform < database_migrations/001_fix_guide_video_auto_increment.sql
-- 
-- Or run in MySQL client:
-- USE eyetwin_platform;
-- [Paste the ALTER TABLE commands below]

-- ============================================================================
-- 1. Fix guide_video table - Add AUTO_INCREMENT
-- ============================================================================
-- Modify the guide_video table to add AUTO_INCREMENT to id column
-- Note: id is already PRIMARY KEY, so we only add AUTO_INCREMENT attribute
ALTER TABLE guide_video
MODIFY id INT AUTO_INCREMENT;

-- Verify the fix worked:
DESCRIBE guide_video;

-- You should see: id | int(11) | NO | PRI | NULL | auto_increment

-- ============================================================================
-- 2. guide_video_like table
-- ============================================================================
-- If guide_video_like table exists and has the same issue, run:
-- ALTER TABLE guide_video_like MODIFY id INT AUTO_INCREMENT;
-- 
-- To create guide_video_like table if it doesn't exist yet:
-- CREATE TABLE guide_video_like (
--   id INT AUTO_INCREMENT PRIMARY KEY,
--   guide_video_id INT NOT NULL,
--   user_id INT NOT NULL,
--   UNIQUE KEY unique_like (guide_video_id, user_id),
--   FOREIGN KEY (guide_video_id) REFERENCES guide_video(id) ON DELETE CASCADE,
--   FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
-- );

-- ============================================================================
-- Test the fix (optional - create a test record)
-- ============================================================================
-- INSERT INTO guide_video 
--   (title, description, video_url, status, created_at, uploaded_by_id, game_id)
-- VALUES 
--   ('Test Guide', 'Test Description', 'https://example.com/video.mp4', 'pending', NOW(), 1, 1);

-- SELECT * FROM guide_video WHERE title = 'Test Guide';
-- DELETE FROM guide_video WHERE title = 'Test Guide';  -- Clean up test record

