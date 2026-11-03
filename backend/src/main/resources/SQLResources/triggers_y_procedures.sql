DROP MATERIALIZED VIEW IF EXISTS mv_demanda_estacion_horaria;

CREATE MATERIALIZED VIEW mv_demanda_estacion_horaria AS
SELECT
    e.id AS estacion_id,
    e.nombre AS estacion_nombre,
    EXTRACT(HOUR FROM eventos.fecha)::int AS hora_dia,
    SUM(eventos.iniciado) AS viajes_iniciados,
    SUM(eventos.finalizado) AS viajes_finalizados
FROM (
    SELECT estacion_origen_id AS estacion_id, iniciado_en AS fecha, 1 AS iniciado, 0 AS finalizado
    FROM viajes
    WHERE cancelado = FALSE
    UNION ALL
    SELECT estacion_destino_id AS estacion_id, finalizado_en AS fecha, 0 AS iniciado, 1 AS finalizado
    FROM viajes
    WHERE finalizado_en IS NOT NULL AND cancelado = FALSE
) eventos
JOIN estaciones e ON e.id = eventos.estacion_id
GROUP BY e.id, e.nombre, EXTRACT(HOUR FROM eventos.fecha);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_demanda_estacion_hora
    ON mv_demanda_estacion_horaria (estacion_id, hora_dia);

CREATE OR REPLACE FUNCTION fn_crear_billetera_usuario()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO billeteras (usuario_id) VALUES (NEW.id)
    ON CONFLICT (usuario_id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_crear_billetera_usuario ON usuarios;
CREATE TRIGGER trg_crear_billetera_usuario
    AFTER INSERT ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION fn_crear_billetera_usuario();

CREATE OR REPLACE FUNCTION fn_validar_configuracion_estacion()
RETURNS TRIGGER AS $$
DECLARE
    v_vehiculos INT;
BEGIN
    IF NEW.capacidad < 1
       OR NEW.objetivo_bicicletas < 0
       OR NEW.objetivo_scooters < 0
       OR NEW.objetivo_bicicletas + NEW.objetivo_scooters > NEW.capacidad THEN
        RAISE EXCEPTION 'Capacidad y objetivos de estación inválidos';
    END IF;

    SELECT COUNT(*) INTO v_vehiculos
    FROM vehiculos
    WHERE estacion_id = NEW.id;
    IF v_vehiculos > NEW.capacidad THEN
        RAISE EXCEPTION 'La capacidad no puede ser menor al inventario actual';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_configuracion_estacion ON estaciones;
CREATE TRIGGER trg_validar_configuracion_estacion
    BEFORE INSERT OR UPDATE OF capacidad, objetivo_bicicletas, objetivo_scooters ON estaciones
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_configuracion_estacion();

CREATE OR REPLACE FUNCTION fn_validar_estacion_vehiculo()
RETURNS TRIGGER AS $$
DECLARE
    v_capacidad INT;
    v_actuales INT;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        PERFORM id
        FROM estaciones
        WHERE id IN (OLD.estacion_id, NEW.estacion_id)
        ORDER BY id
        FOR UPDATE;
    ELSIF NEW.estacion_id IS NOT NULL THEN
        PERFORM id
        FROM estaciones
        WHERE id = NEW.estacion_id
        FOR UPDATE;
    END IF;

    IF NEW.estacion_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT capacidad INTO v_capacidad
    FROM estaciones
    WHERE id = NEW.estacion_id AND activa = TRUE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La estación no existe o está inactiva';
    END IF;

    IF TG_OP = 'INSERT' THEN
        SELECT COUNT(*) INTO v_actuales
        FROM vehiculos
        WHERE estacion_id = NEW.estacion_id;
    ELSE
        SELECT COUNT(*) INTO v_actuales
        FROM vehiculos
        WHERE estacion_id = NEW.estacion_id AND id <> NEW.id;
    END IF;
    IF v_actuales >= v_capacidad THEN
        RAISE EXCEPTION 'La estación alcanzó su capacidad máxima';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_estacion_vehiculo ON vehiculos;
CREATE TRIGGER trg_validar_estacion_vehiculo
    BEFORE INSERT OR UPDATE OF estacion_id ON vehiculos
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_estacion_vehiculo();

CREATE OR REPLACE FUNCTION fn_prevenir_cambio_vehiculo_en_viaje()
RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.estado IS DISTINCT FROM OLD.estado
            OR NEW.bateria IS DISTINCT FROM OLD.bateria
            OR NEW.codigo IS DISTINCT FROM OLD.codigo)
       AND EXISTS (
           SELECT 1 FROM viajes
           WHERE vehiculo_id = OLD.id AND finalizado_en IS NULL
       ) THEN
        RAISE EXCEPTION 'No se puede modificar un vehículo con un viaje activo';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevenir_cambio_vehiculo_en_viaje ON vehiculos;
CREATE TRIGGER trg_prevenir_cambio_vehiculo_en_viaje
    BEFORE UPDATE OF estado, bateria, codigo ON vehiculos
    FOR EACH ROW
    EXECUTE FUNCTION fn_prevenir_cambio_vehiculo_en_viaje();

CREATE OR REPLACE FUNCTION fn_validar_inicio_viaje()
RETURNS TRIGGER AS $$
DECLARE
    v_saldo NUMERIC(10, 2);
    v_bateria INT;
    v_estado VARCHAR(50);
    v_estacion_id BIGINT;
    v_tarifa_id BIGINT;
    v_precio NUMERIC(10, 2);
BEGIN
    SELECT saldo
    INTO v_saldo
    FROM billeteras
    WHERE usuario_id = NEW.usuario_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El usuario no tiene una billetera asociada';
    END IF;
    IF v_saldo < 0 THEN
        RAISE EXCEPTION 'No se puede iniciar el viaje: el saldo es negativo';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM billeteras
        WHERE usuario_id = NEW.usuario_id AND activa = TRUE
    ) THEN
        RAISE EXCEPTION 'La billetera del usuario está inactiva';
    END IF;
    IF EXISTS (
        SELECT 1 FROM viajes
        WHERE usuario_id = NEW.usuario_id AND finalizado_en IS NULL
    ) THEN
        RAISE EXCEPTION 'El usuario ya tiene un viaje activo';
    END IF;

    SELECT bateria, estado, estacion_id
    INTO v_bateria, v_estado, v_estacion_id
    FROM vehiculos
    WHERE id = NEW.vehiculo_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El vehículo no existe';
    END IF;
    IF v_bateria < 20 THEN
        RAISE EXCEPTION 'La batería del vehículo es inferior al 20%%';
    END IF;
    IF v_estado <> 'DISPONIBLE' OR v_estacion_id IS NULL THEN
        RAISE EXCEPTION 'El vehículo no está disponible en una estación';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM estaciones WHERE id = v_estacion_id AND activa = TRUE) THEN
        RAISE EXCEPTION 'La estación de origen está inactiva';
    END IF;

    SELECT id, precio_por_minuto
    INTO v_tarifa_id, v_precio
    FROM tarifas
    WHERE tipo_vehiculo = (SELECT tipo FROM vehiculos WHERE id = NEW.vehiculo_id)
      AND activa = TRUE
      AND vigente_desde <= NOW()
      AND (vigente_hasta IS NULL OR vigente_hasta > NOW())
    ORDER BY vigente_desde DESC
    LIMIT 1;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe una tarifa vigente para el vehículo';
    END IF;

    NEW.estacion_origen_id := v_estacion_id;
    NEW.tarifa_id := v_tarifa_id;
    NEW.precio_por_minuto_aplicado := v_precio;
    UPDATE vehiculos SET estado = 'EN_USO', estacion_id = NULL WHERE id = NEW.vehiculo_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_inicio_viaje ON viajes;
CREATE TRIGGER trg_validar_inicio_viaje
    BEFORE INSERT ON viajes
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_inicio_viaje();

CREATE OR REPLACE FUNCTION fn_auditar_estado_vehiculo()
RETURNS TRIGGER AS $$
DECLARE
    v_operador_id BIGINT;
    v_motivo TEXT;
BEGIN
    IF NEW.estado IN ('EN_MANTENCION', 'DADO_DE_BAJA')
            AND OLD.estado IS DISTINCT FROM NEW.estado THEN
        v_operador_id := NULLIF(current_setting('app.operador', true), '')::bigint;
        v_motivo := NULLIF(trim(current_setting('app.motivo', true)), '');

        IF v_operador_id IS NULL THEN
            RAISE EXCEPTION 'Falta el operador responsable del cambio de estado';
        END IF;
        IF v_motivo IS NULL THEN
            RAISE EXCEPTION 'El motivo del cambio de estado es obligatorio';
        END IF;

        INSERT INTO auditoria_estado_vehiculo (
            vehiculo_id, estado_anterior, estado_nuevo, operador_id, motivo
        ) VALUES (
            NEW.id, OLD.estado, NEW.estado, v_operador_id, v_motivo
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_estado_vehiculo ON vehiculos;
CREATE TRIGGER trg_auditar_estado_vehiculo
    AFTER UPDATE OF estado ON vehiculos
    FOR EACH ROW
    EXECUTE FUNCTION fn_auditar_estado_vehiculo();

CREATE OR REPLACE PROCEDURE finalizar_viaje(
    p_viaje_id BIGINT,
    p_estacion_destino_id BIGINT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_usuario_id BIGINT;
    v_vehiculo_id BIGINT;
    v_tipo_vehiculo VARCHAR(50);
    v_iniciado_en TIMESTAMPTZ;
    v_minutos NUMERIC;
    v_precio_minuto NUMERIC(10, 2);
    v_costo_total NUMERIC(10, 2);
    v_saldo_actual NUMERIC(10, 2);
BEGIN
    SELECT j.usuario_id, j.vehiculo_id, j.iniciado_en, veh.tipo
    INTO v_usuario_id, v_vehiculo_id, v_iniciado_en, v_tipo_vehiculo
    FROM viajes j
    JOIN vehiculos veh ON j.vehiculo_id = veh.id
    WHERE j.id = p_viaje_id AND j.finalizado_en IS NULL
    FOR UPDATE OF j, veh;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El viaje no existe o ya fue finalizado';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM estaciones WHERE id = p_estacion_destino_id AND activa = TRUE
    ) THEN
        RAISE EXCEPTION 'La estación de destino no existe o está inactiva';
    END IF;

    v_minutos := GREATEST(CEIL(EXTRACT(EPOCH FROM (NOW() - v_iniciado_en)) / 60.0), 1);
    SELECT precio_por_minuto_aplicado
    INTO v_precio_minuto
    FROM viajes
    WHERE id = p_viaje_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe una tarifa vigente para el tipo %', v_tipo_vehiculo;
    END IF;

    v_costo_total := v_minutos * v_precio_minuto;
    SELECT saldo
    INTO v_saldo_actual
    FROM billeteras
    WHERE usuario_id = v_usuario_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El usuario no tiene una billetera asociada';
    END IF;

    UPDATE billeteras SET saldo = saldo - v_costo_total WHERE usuario_id = v_usuario_id;
    INSERT INTO movimientos_billetera
        (billetera_id, operador_id, tipo, monto, motivo)
    SELECT id, NULL, 'CARGO_VIAJE', -v_costo_total,
           'Cobro automático del viaje ' || p_viaje_id
    FROM billeteras
    WHERE usuario_id = v_usuario_id;
    UPDATE viajes
    SET finalizado_en = NOW(),
        estacion_destino_id = p_estacion_destino_id,
        costo = v_costo_total
    WHERE id = p_viaje_id;
    UPDATE vehiculos
    SET estacion_id = p_estacion_destino_id, estado = 'DISPONIBLE'
    WHERE id = v_vehiculo_id;
END;
$$;

CREATE OR REPLACE PROCEDURE cancelar_viaje(
    p_viaje_id BIGINT,
    p_usuario_id BIGINT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_vehiculo_id BIGINT;
    v_estacion_origen_id BIGINT;
BEGIN
    SELECT vehiculo_id, estacion_origen_id
    INTO v_vehiculo_id, v_estacion_origen_id
    FROM viajes
    WHERE id = p_viaje_id
      AND usuario_id = p_usuario_id
      AND finalizado_en IS NULL
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El viaje no existe, no pertenece al usuario o ya finalizó';
    END IF;

    UPDATE viajes
    SET finalizado_en = NOW(), costo = 0, cancelado = TRUE
    WHERE id = p_viaje_id;
    UPDATE vehiculos
    SET estacion_id = v_estacion_origen_id, estado = 'DISPONIBLE'
    WHERE id = v_vehiculo_id;
END;
$$;

CREATE OR REPLACE PROCEDURE ejecutar_rebalanceo(
    p_solicitud_id UUID,
    p_estacion_origen_id BIGINT,
    p_estacion_destino_id BIGINT,
    p_tipo_vehiculo VARCHAR(50),
    p_cantidad INT,
    p_operador_id BIGINT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_disponibles_origen INT;
    v_disponibles_destino INT;
    v_vehiculos_destino INT;
    v_objetivo_origen INT;
    v_objetivo_destino INT;
    v_capacidad_destino INT;
    v_actualizados INT;
BEGIN
    IF p_solicitud_id IS NULL OR p_cantidad < 1 THEN
        RAISE EXCEPTION 'La solicitud y la cantidad deben ser válidas';
    END IF;
    IF p_estacion_origen_id = p_estacion_destino_id THEN
        RAISE EXCEPTION 'Las estaciones de origen y destino deben ser distintas';
    END IF;
    IF EXISTS (SELECT 1 FROM ordenes_traslado WHERE solicitud_id = p_solicitud_id) THEN
        RETURN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM estaciones WHERE id = p_estacion_origen_id AND activa) OR
       NOT EXISTS (SELECT 1 FROM estaciones WHERE id = p_estacion_destino_id AND activa) THEN
        RAISE EXCEPTION 'Las estaciones deben existir y estar activas';
    END IF;

    PERFORM id
    FROM estaciones
    WHERE id IN (p_estacion_origen_id, p_estacion_destino_id)
    ORDER BY id
    FOR UPDATE;

    SELECT capacidad,
           CASE p_tipo_vehiculo
               WHEN 'BICICLETA' THEN objetivo_bicicletas
               WHEN 'SCOOTER' THEN objetivo_scooters
           END
    INTO v_capacidad_destino, v_objetivo_destino
    FROM estaciones
    WHERE id = p_estacion_destino_id;

    SELECT CASE p_tipo_vehiculo
               WHEN 'BICICLETA' THEN objetivo_bicicletas
               WHEN 'SCOOTER' THEN objetivo_scooters
           END
    INTO v_objetivo_origen
    FROM estaciones
    WHERE id = p_estacion_origen_id;

    SELECT COUNT(*) INTO v_disponibles_origen
    FROM vehiculos
    WHERE estacion_id = p_estacion_origen_id
      AND tipo = p_tipo_vehiculo AND estado = 'DISPONIBLE';
    SELECT COUNT(*) INTO v_disponibles_destino
    FROM vehiculos
    WHERE estacion_id = p_estacion_destino_id
      AND tipo = p_tipo_vehiculo AND estado = 'DISPONIBLE';
    SELECT COUNT(*) INTO v_vehiculos_destino
    FROM vehiculos
    WHERE estacion_id = p_estacion_destino_id;

    IF v_disponibles_origen - v_objetivo_origen < p_cantidad THEN
        RAISE EXCEPTION 'La estación de origen no tiene el exceso solicitado';
    END IF;
    IF v_objetivo_destino - v_disponibles_destino < p_cantidad THEN
        RAISE EXCEPTION 'La estación de destino no tiene un déficit suficiente';
    END IF;
    IF v_capacidad_destino - v_vehiculos_destino < p_cantidad THEN
        RAISE EXCEPTION 'La estación de destino no tiene capacidad física suficiente';
    END IF;

    WITH seleccion AS (
        SELECT id
        FROM vehiculos
        WHERE estacion_id = p_estacion_origen_id
          AND tipo = p_tipo_vehiculo
          AND estado = 'DISPONIBLE'
        ORDER BY id
        LIMIT p_cantidad
        FOR UPDATE SKIP LOCKED
    )
    UPDATE vehiculos
    SET estacion_id = p_estacion_destino_id
    WHERE id IN (SELECT id FROM seleccion);

    GET DIAGNOSTICS v_actualizados = ROW_COUNT;
    IF v_actualizados <> p_cantidad THEN
        RAISE EXCEPTION 'No fue posible reservar todos los vehículos solicitados';
    END IF;

    INSERT INTO ordenes_traslado (
        solicitud_id, estacion_origen_id, estacion_destino_id,
        tipo_vehiculo, cantidad, operador_id
    ) VALUES (
        p_solicitud_id, p_estacion_origen_id, p_estacion_destino_id,
        p_tipo_vehiculo, p_cantidad, p_operador_id
    );
END;
$$;

REFRESH MATERIALIZED VIEW mv_demanda_estacion_horaria;
