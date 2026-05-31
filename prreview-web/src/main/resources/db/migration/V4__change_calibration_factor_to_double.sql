-- V4__change_calibration_factor_to_double.sql
-- Change calibration_factor column from DECIMAL(4,3) to DOUBLE
-- This ensures consistency across all numeric fields

ALTER TABLE feedback_calibrations MODIFY COLUMN calibration_factor DOUBLE NOT NULL DEFAULT 1.0;
