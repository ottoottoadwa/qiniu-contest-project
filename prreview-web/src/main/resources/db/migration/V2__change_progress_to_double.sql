-- V2__change_progress_to_double.sql
-- Change progress column from DECIMAL(4,3) to DOUBLE
-- This aligns with the domain model which uses primitive double

ALTER TABLE reviews MODIFY COLUMN progress DOUBLE NOT NULL DEFAULT 0;
