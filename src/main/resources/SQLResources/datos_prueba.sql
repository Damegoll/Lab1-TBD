-- Datos de demostracion.
-- Ejecutar despues de schema.sql y triggers_y_procedures.sql.
-- Todos los usuarios de demostracion usan la contrasena Prueba123!QA2026.
-- Re-ejecutable: no duplica usuarios, estaciones, tarifas ni vehiculos.

BEGIN;

INSERT INTO usuarios (username, nombre, email, password_hash, rol)
VALUES
    ('UsuarioConDinero', 'Usuario con dinero', 'usuario.con.dinero@example.test',
     '$2a$10$m/nYFYzIp.JsMLNnuu5l.exPARHUA3r7FeHmVZWsJF/U0YDzDEEoa', 'USUARIO'),
    ('UsuarioSinDinero', 'Usuario sin dinero', 'usuario.sin.dinero@example.test',
     '$2a$10$m/nYFYzIp.JsMLNnuu5l.exPARHUA3r7FeHmVZWsJF/U0YDzDEEoa', 'USUARIO'),
    ('UsuarioSaldoNegativo', 'Usuario con saldo negativo',
     'usuario.saldo.negativo@example.test',
     '$2a$10$m/nYFYzIp.JsMLNnuu5l.exPARHUA3r7FeHmVZWsJF/U0YDzDEEoa', 'USUARIO'),
    ('OperadorFlota', 'Operador de flota', 'operador.flota@example.test',
     '$2a$10$m/nYFYzIp.JsMLNnuu5l.exPARHUA3r7FeHmVZWsJF/U0YDzDEEoa', 'OPERADOR')
ON CONFLICT (username) DO NOTHING;

INSERT INTO estaciones (
    nombre, capacidad, objetivo_bicicletas, objetivo_scooters
)
SELECT 'DemoCentro', 10, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM estaciones WHERE nombre = 'DemoCentro');

INSERT INTO estaciones (
    nombre, capacidad, objetivo_bicicletas, objetivo_scooters
)
SELECT 'DemoNorte', 10, 1, 2
WHERE NOT EXISTS (SELECT 1 FROM estaciones WHERE nombre = 'DemoNorte');

INSERT INTO estaciones (
    nombre, capacidad, objetivo_bicicletas, objetivo_scooters
)
SELECT 'DemoSur', 10, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM estaciones WHERE nombre = 'DemoSur');

INSERT INTO estaciones (
    nombre, capacidad, objetivo_bicicletas, objetivo_scooters
)
SELECT 'DemoVacia', 10, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM estaciones WHERE nombre = 'DemoVacia');

INSERT INTO tarifas (tipo_vehiculo, precio_por_minuto, vigente_desde, vigente_hasta)
SELECT 'BICICLETA', 0.15, NOW() - INTERVAL '1 day', NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM tarifas
    WHERE tipo_vehiculo = 'BICICLETA'
      AND activa = TRUE
      AND vigente_desde <= NOW()
      AND (vigente_hasta IS NULL OR vigente_hasta > NOW())
);

INSERT INTO tarifas (tipo_vehiculo, precio_por_minuto, vigente_desde, vigente_hasta)
SELECT 'SCOOTER', 0.25, NOW() - INTERVAL '1 day', NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM tarifas
    WHERE tipo_vehiculo = 'SCOOTER'
      AND activa = TRUE
      AND vigente_desde <= NOW()
      AND (vigente_hasta IS NULL OR vigente_hasta > NOW())
);

INSERT INTO vehiculos (codigo, tipo, estado, bateria, estacion_id)
SELECT 'BICI-CENTRO-01', 'BICICLETA', 'DISPONIBLE', 85, e.id
FROM estaciones e
WHERE e.nombre = 'DemoCentro'
  AND NOT EXISTS (SELECT 1 FROM vehiculos WHERE codigo = 'BICI-CENTRO-01');

INSERT INTO vehiculos (codigo, tipo, estado, bateria, estacion_id)
SELECT 'BICI-CENTRO-02', 'BICICLETA', 'DISPONIBLE', 64, e.id
FROM estaciones e
WHERE e.nombre = 'DemoCentro'
  AND NOT EXISTS (SELECT 1 FROM vehiculos WHERE codigo = 'BICI-CENTRO-02');

INSERT INTO vehiculos (codigo, tipo, estado, bateria, estacion_id)
SELECT 'SCOOTER-CENTRO-01', 'SCOOTER', 'DISPONIBLE', 80, e.id
FROM estaciones e
WHERE e.nombre = 'DemoCentro'
  AND NOT EXISTS (SELECT 1 FROM vehiculos WHERE codigo = 'SCOOTER-CENTRO-01');

INSERT INTO vehiculos (codigo, tipo, estado, bateria, estacion_id)
SELECT 'SCOOTER-CENTRO-02', 'SCOOTER', 'DISPONIBLE', 19, e.id
FROM estaciones e
WHERE e.nombre = 'DemoCentro'
  AND NOT EXISTS (SELECT 1 FROM vehiculos WHERE codigo = 'SCOOTER-CENTRO-02');

INSERT INTO vehiculos (codigo, tipo, estado, bateria, estacion_id)
SELECT 'SCOOTER-CENTRO-03', 'SCOOTER', 'DISPONIBLE', 55, e.id
FROM estaciones e
WHERE e.nombre = 'DemoCentro'
  AND NOT EXISTS (SELECT 1 FROM vehiculos WHERE codigo = 'SCOOTER-CENTRO-03');

INSERT INTO vehiculos (codigo, tipo, estado, bateria, estacion_id)
SELECT 'BICI-NORTE-01', 'BICICLETA', 'DISPONIBLE', 72, e.id
FROM estaciones e
WHERE e.nombre = 'DemoNorte'
  AND NOT EXISTS (SELECT 1 FROM vehiculos WHERE codigo = 'BICI-NORTE-01');

INSERT INTO vehiculos (codigo, tipo, estado, bateria, estacion_id)
SELECT 'BICI-NORTE-02', 'BICICLETA', 'DISPONIBLE', 91, e.id
FROM estaciones e
WHERE e.nombre = 'DemoNorte'
  AND NOT EXISTS (SELECT 1 FROM vehiculos WHERE codigo = 'BICI-NORTE-02');

INSERT INTO vehiculos (codigo, tipo, estado, bateria, estacion_id)
SELECT 'SCOOTER-NORTE-01', 'SCOOTER', 'DISPONIBLE', 68, e.id
FROM estaciones e
WHERE e.nombre = 'DemoNorte'
  AND NOT EXISTS (SELECT 1 FROM vehiculos WHERE codigo = 'SCOOTER-NORTE-01');

INSERT INTO vehiculos (codigo, tipo, estado, bateria, estacion_id)
SELECT 'BICI-SUR-01', 'BICICLETA', 'DISPONIBLE', 77, e.id
FROM estaciones e
WHERE e.nombre = 'DemoSur'
  AND NOT EXISTS (SELECT 1 FROM vehiculos WHERE codigo = 'BICI-SUR-01');

INSERT INTO vehiculos (codigo, tipo, estado, bateria, estacion_id)
SELECT 'BICI-SUR-02', 'BICICLETA', 'DISPONIBLE', 58, e.id
FROM estaciones e
WHERE e.nombre = 'DemoSur'
  AND NOT EXISTS (SELECT 1 FROM vehiculos WHERE codigo = 'BICI-SUR-02');

INSERT INTO vehiculos (codigo, tipo, estado, bateria, estacion_id)
SELECT 'SCOOTER-SUR-01', 'SCOOTER', 'DISPONIBLE', 96, e.id
FROM estaciones e
WHERE e.nombre = 'DemoSur'
  AND NOT EXISTS (SELECT 1 FROM vehiculos WHERE codigo = 'SCOOTER-SUR-01');

-- Inicializa las billeteras de demostracion sin sobrescribir saldos que ya tengan movimientos.
UPDATE billeteras b
SET saldo = CASE u.username
    WHEN 'UsuarioConDinero' THEN 100.00
    WHEN 'UsuarioSaldoNegativo' THEN -1.00
    ELSE 0.00
END
FROM usuarios u
WHERE b.usuario_id = u.id
  AND u.username IN (
      'UsuarioConDinero', 'UsuarioSinDinero',
      'UsuarioSaldoNegativo', 'OperadorFlota'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM movimientos_billetera m
      WHERE m.billetera_id = b.id
  );

COMMIT;

-- Comprobacion rapida del inventario de demostracion:
-- SELECT username, rol FROM usuarios
-- WHERE username IN ('UsuarioConDinero', 'UsuarioSinDinero',
--                    'UsuarioSaldoNegativo', 'OperadorFlota')
-- ORDER BY username;
-- SELECT e.nombre, e.capacidad, COUNT(v.id) AS vehiculos
-- FROM estaciones e LEFT JOIN vehiculos v ON v.estacion_id = e.id
-- WHERE e.nombre IN ('DemoCentro', 'DemoNorte', 'DemoSur', 'DemoVacia')
-- GROUP BY e.id ORDER BY e.nombre;
-- SELECT codigo, tipo, estado, bateria, estacion_id
-- FROM vehiculos WHERE codigo LIKE '%-CENTRO-%'
--    OR codigo LIKE '%-NORTE-%' OR codigo LIKE '%-SUR-%'
-- ORDER BY codigo;
-- SELECT u.username, b.saldo, b.activa
-- FROM usuarios u JOIN billeteras b ON b.usuario_id = u.id
-- WHERE u.username IN ('UsuarioConDinero', 'UsuarioSinDinero',
--                      'UsuarioSaldoNegativo', 'OperadorFlota')
-- ORDER BY u.username;
--
-- Pruebas recomendadas:
-- 1. Iniciar un viaje con UsuarioConDinero y BICI-CENTRO-01.
-- 2. Intentar iniciar otro viaje de UsuarioConDinero antes de finalizar el primero;
--    debe rechazarse por viaje activo.
-- 3. Intentar iniciar con UsuarioSaldoNegativo y BICI-CENTRO-01;
--    debe rechazarse por saldo negativo.
-- 4. Intentar iniciar con UsuarioConDinero y SCOOTER-CENTRO-02;
--    debe rechazarse por bateria inferior a 20%.
-- 5. Finalizar el viaje activo indicando DemoNorte; debe cobrar y anclar el vehiculo.
-- 6. Como OperadorFlota, rebalancear un scooter desde DemoCentro hacia DemoNorte;
--    hay exceso, deficit y capacidad para trasladar una unidad.
-- 7. Cambiar BICI-CENTRO-01 a EN_MANTENCION con un motivo como operador;
--    debe aparecer en auditoria_estado_vehiculo.
