--
ALTER TABLE `dpu_instance` ADD COLUMN `use_template_config` SMALLINT NOT NULL DEFAULT 0;

-- Update version.
UPDATE `properties` SET `value` = '001.004.001' WHERE `key` = 'UV.Core.version';
UPDATE `properties` SET `value` = '001.000.001' WHERE `key` = 'UV.Plugin-DevEnv.version';
