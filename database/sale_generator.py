import psycopg2
import random
from datetime import datetime, timedelta
import calendar
import uuid

# Parámetros configurables para generar ventas
MES = 12
ANIO = 2024
VENTAS_MIN = 19000
VENTAS_MAX = 22000
VENTAS_A_GENERAR = random.randint(VENTAS_MIN, VENTAS_MAX)
VENTAS_CANCELADAS = random.randint(1, 15)
PRODUCTOS_MIN_POR_VENTA = 1
PRODUCTOS_MAX_POR_VENTA = 25

# Rango horario: 6:00 AM a 8:00 PM
HORA_INICIO = 6
HORA_FIN = 20

# Tipos de pago y estado
PAYMENT_TYPES = ['CASH', 'CARD', 'TRANSFER', 'PAYPAL', 'BITCOIN']
SALE_STATUS = ['COMPLETED', 'CANCELED']

# Generar lista de fechas del mes
_, last_day = calendar.monthrange(ANIO, MES)
fechas_disponibles = []
for day in range(1, last_day + 1):
    for _ in range(random.randint(400, 900)):  # Aprox por día
        hora = random.randint(HORA_INICIO, HORA_FIN)
        minuto = random.randint(0, 59)
        segundo = random.randint(0, 59)
        fechas_disponibles.append(datetime(ANIO, MES, day, hora, minuto, segundo))

random.shuffle(fechas_disponibles)

# Conectar a PostgreSQL
conn = psycopg2.connect(
    host="localhost",
    database="heuristik_db",
    user="root",
    password="root",
    port=5432
)
cur = conn.cursor()

# Cargar productos existentes
cur.execute("SELECT id, price FROM product")
productos = cur.fetchall()

if not productos:
    raise Exception("No hay productos en la tabla product.")

ventas_generadas = []
detalles_generados = []

# IDs para ventas canceladas
cancelled_indexes = set(random.sample(range(VENTAS_A_GENERAR), VENTAS_CANCELADAS))

for i in range(VENTAS_A_GENERAR):
    sale_id = str(uuid.uuid4())
    sale_date = fechas_disponibles[i % len(fechas_disponibles)]
    payment_type = random.choice(PAYMENT_TYPES)
    status = 'CANCELED' if i in cancelled_indexes else 'COMPLETED'

    # Seleccionar productos únicos para el detalle
    productos_en_venta = random.sample(productos, random.randint(PRODUCTOS_MIN_POR_VENTA, min(PRODUCTOS_MAX_POR_VENTA, len(productos))))
    total_venta = sum([float(p[1]) for p in productos_en_venta])

    # Insertar venta
    ventas_generadas.append((sale_id, sale_date, payment_type, total_venta, status))

    # Insertar detalles
    for product_id, unit_price in productos_en_venta:
        cantidad = random.choices([1, 2, 3, 4, 5], weights=[70, 15, 7, 5, 3])[0]
        total_line = round(float(unit_price) * cantidad, 2)
        detalles_generados.append((
            str(uuid.uuid4()),
            sale_id,
            product_id,
            cantidad,
            float(unit_price)
        ))

# Insertar en la base de datos
for v in ventas_generadas:
    cur.execute("""
        INSERT INTO sale (id, sale_date, payment_type, total, status)
        VALUES (%s, %s, %s, %s, %s)
    """, v)

for d in detalles_generados:
    cur.execute("""
        INSERT INTO sale_detail (id, sale_id, product_id, quantity, unit_price)
        VALUES (%s, %s, %s, %s, %s)
    """, d)

conn.commit()
cur.close()
conn.close()

