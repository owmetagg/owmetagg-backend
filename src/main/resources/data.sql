-- data.sql - Sample heroes data for H2 database
-- This file should be in src/main/resources/data.sql

-- Clear any existing data (just in case)
DELETE FROM heroes;

-- Insert sample heroes
INSERT INTO heroes (name, role, description) VALUES
                                                 ('Tracer', 'DAMAGE', 'High-mobility time-manipulating fighter'),
                                                 ('Reinhardt', 'TANK', 'Barrier-wielding melee fighter'),
                                                 ('Mercy', 'SUPPORT', 'Guardian angel healer'),
                                                 ('Genji', 'DAMAGE', 'Cybernetic ninja'),
                                                 ('D.Va', 'TANK', 'Mobile mech pilot'),
                                                 ('Lucio', 'SUPPORT', 'Sound-based speed and healing support'),
                                                 ('Widowmaker', 'DAMAGE', 'Long-range sniper'),
                                                 ('Winston', 'TANK', 'Tesla cannon-wielding scientist'),
                                                 ('Ana', 'SUPPORT', 'Sniper rifle healer'),
                                                 ('Soldier: 76', 'DAMAGE', 'Heavy pulse rifle veteran'),
                                                 ('Pharah', 'DAMAGE', 'Rocket-launcher aerial combatant'),
                                                 ('Zarya', 'TANK', 'Energy-based tank with barriers'),
                                                 ('Zenyatta', 'SUPPORT', 'Orb-throwing omnic monk');