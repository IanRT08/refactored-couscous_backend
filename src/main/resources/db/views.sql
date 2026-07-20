-- Vista de usuarios sin la columna fotoPerfil (MEDIUMBLOB).
-- Ejecutar una sola vez en la base de datos.
-- Usar esta vista en gestores de BD (DBeaver, Workbench, etc.)
-- para navegar los usuarios sin que el blob ocupe toda la pantalla.

CREATE OR REPLACE VIEW v_usuarios AS
SELECT
    id_usuario,
    nombre_usario,
    correo,
    nombre_completo,
    estado,
    fecha_registro,
    intentos_fallidos,
    fecha_bloqueo,
    preferencias_alertas
FROM usuario;
