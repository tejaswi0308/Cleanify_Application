-- Migration: Switch from Supabase Auth to direct DB credential storage
-- Run this in Supabase SQL Editor (https://supabase.com/dashboard → SQL Editor)

-- 1. Add password_hash and password_salt columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_salt TEXT;

-- 2. Remove foreign key constraint on id column (if it references auth.users)
-- First, find and drop the foreign key constraint
-- Run this to see the constraint name:
-- SELECT conname FROM pg_constraint WHERE conrelid = 'users'::regclass AND contype = 'f';

-- Drop the foreign key (replace with actual constraint name if different)
-- Common constraint names: users_id_fkey, users_pkey (if id is FK to auth.users)
DO $$
DECLARE
    fk_record RECORD;
BEGIN
    FOR fk_record IN 
        SELECT conname 
        FROM pg_constraint 
        WHERE conrelid = 'users'::regclass 
        AND contype = 'f'
    LOOP
        EXECUTE 'ALTER TABLE users DROP CONSTRAINT ' || fk_record.conname;
        RAISE NOTICE 'Dropped foreign key constraint: %', fk_record.conname;
    END LOOP;
END $$;

-- 3. Disable RLS on users table (or add policy for anon access)
-- Option A: Disable RLS entirely (simpler, use anon key for all operations)
ALTER TABLE users DISABLE ROW LEVEL SECURITY;

-- Option B (alternative): If you want to keep RLS, add a policy for anon key access:
-- CREATE POLICY "Allow anon access to users" ON users
--     FOR ALL USING (true) WITH CHECK (true);

-- 4. Also disable RLS on cleaning_requests table if needed
ALTER TABLE cleaning_requests DISABLE ROW LEVEL SECURITY;

-- 5. Make sure the id column allows any UUID (not just auth.users references)
-- If id is currently a foreign key type, alter it:
ALTER TABLE users ALTER COLUMN id SET DEFAULT gen_random_uuid();

-- Verify the changes
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'users' 
ORDER BY ordinal_position;
