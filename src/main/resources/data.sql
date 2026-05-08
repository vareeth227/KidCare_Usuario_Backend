-- Datos iniciales de roles (INSERT IGNORE evita error si ya existen)
INSERT IGNORE INTO rol (nombre, descripcion) VALUES ('ADMIN',    'Administrador del sistema KidCare');
INSERT IGNORE INTO rol (nombre, descripcion) VALUES ('TUTOR',    'Padre, madre o cuidador principal del menor');
INSERT IGNORE INTO rol (nombre, descripcion) VALUES ('DELEGADO', 'Cuidador secundario invitado por el tutor');
