-- Roles del sistema
INSERT INTO ROL (nombre, descripcion) VALUES
    ('ADMIN',    'Administrador del sistema KidCare'),
    ('TUTOR',    'Padre, madre o cuidador principal del menor'),
    ('DELEGADO', 'Cuidador secundario invitado por el tutor')
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

-- Permisos del sistema
INSERT INTO PERMISO (nombre_permiso, descripcion) VALUES
    ('REGISTRAR_MENOR',    'Crear y gestionar perfiles de menores'),
    ('VER_HISTORIAL',      'Visualizar el historial de síntomas del menor'),
    ('USAR_CHATBOT',       'Interactuar con el chatbot de registro de síntomas'),
    ('GENERAR_TOKEN',      'Generar enlace temporal de acceso para médico'),
    ('GESTIONAR_USUARIOS', 'Habilitar, deshabilitar y modificar usuarios'),
    ('VER_AUDITORIA',      'Consultar registros de auditoría del sistema')
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

-- ADMIN: todos los permisos
INSERT IGNORE INTO ROL_PERMISO (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso FROM ROL r, PERMISO p WHERE r.nombre = 'ADMIN';

-- TUTOR: registrar menores, ver historial, chatbot y generar token médico
INSERT IGNORE INTO ROL_PERMISO (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso FROM ROL r, PERMISO p
WHERE r.nombre = 'TUTOR'
  AND p.nombre_permiso IN ('REGISTRAR_MENOR','VER_HISTORIAL','USAR_CHATBOT','GENERAR_TOKEN');

-- DELEGADO: solo ver historial e interactuar con chatbot
INSERT IGNORE INTO ROL_PERMISO (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso FROM ROL r, PERMISO p
WHERE r.nombre = 'DELEGADO'
  AND p.nombre_permiso IN ('VER_HISTORIAL','USAR_CHATBOT');

-- ============================================================
-- USUARIO ADMINISTRADOR POR DEFECTO (para evaluación)
-- Email:      admin@kidcare.cl
-- Contraseña: Admin@2024!
-- ============================================================
UPDATE USUARIO SET eliminado = false WHERE eliminado IS NULL;

INSERT INTO USUARIO (nombre_completo, email, password_hash, id_rol, activo, eliminado, fechaCreacion)
SELECT 'Administrador KidCare', 'admin@kidcare.cl',
       '$2a$10$5hosKANT9lfxX/Rkm3X6dORed/BoBaWD/fY4Ay7yNrEeTclBcWfGG',
       r.id_rol, true, false, CURDATE()
FROM ROL r
WHERE r.nombre = 'ADMIN'
ON DUPLICATE KEY UPDATE nombre_completo = VALUES(nombre_completo);
