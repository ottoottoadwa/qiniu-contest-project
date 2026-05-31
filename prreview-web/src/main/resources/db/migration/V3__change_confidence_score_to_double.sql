-- V3__change_confidence_score_to_double.sql
-- Change confidence_score column from DECIMAL(4,3) to DOUBLE
-- This aligns with the domain model which uses primitive double

ALTER TABLE risk_items MODIFY COLUMN confidence_score DOUBLE;
