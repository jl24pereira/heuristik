from faker import Faker
import psycopg2
import random

fake = Faker()

# Conexion PG
conn = psycopg2.connect(
    host="localhost",
    database="heuristik_db",
    user="root",
    password="root",
    port=5432
)
cur = conn.cursor()

# Categorías
categorias = [
    "Electronics", "Clothing", "Books", "Toys", "Furniture", "Groceries", "Beauty", "Health",
    "Jewelry", "Shoes", "Sports", "Home Decor", "Tools", "Garden", "Pet Supplies", "Office Supplies",
    "Automotive", "Appliances", "Music", "Movies", "Video Games", "Stationery", "Luggage", "Watches",
    "Baby Products", "Cleaning Supplies", "Kitchenware", "Outdoor Gear", "Beverages", "Snacks",
    "Fitness Equipment", "Smart Home", "Lighting", "Bedding", "Bath", "Hardware", "Art Supplies",
    "Crafts", "Musical Instruments", "Safety Equipment", "Storage", "Holiday Decor", "Party Supplies",
    "Hair Care", "Skincare", "Makeup", "Eyewear", "Mobility Aids", "Medical Supplies", "Books - Fiction", "Books - Non-Fiction"
]

# Generar nombres
def generar_nombre_producto(fake):
    adjetivo = random.choice([
        fake.color_name(), fake.word().capitalize(), fake.company_suffix()
    ])
    categoria = random.choice([
        "Speaker", "Headphones", "Desk", "Lamp", "Backpack", "Chair", "Bottle", "Watch", "Keyboard", "Monitor",
        "Blender", "Vacuum", "Tablet", "Phone", "Mug", "Sunglasses", "Printer", "Router", "Microwave", "Fan"
    ])
    sufijo = random.choice([
        "Pro", "X", "Plus", "Lite", "Max", "2000", "Elite", "Air", "Advance", "Smart"
    ])
    return f"{adjetivo} {categoria} {sufijo}"

nombres_generados = set()
NUM_REGISTROS = 5000

while len(nombres_generados) < NUM_REGISTROS:
    nombres_generados.add(generar_nombre_producto(fake))

for nombre in nombres_generados:
    category = random.choice(categorias)
    cost = round(random.uniform(0.25, 15), 2)
    price = round(cost * random.uniform(1.2, 1.8), 2)
    stock = random.randint(0, 200)

    cur.execute("""
        INSERT INTO product (name, category, cost, stock, price)
        VALUES (%s, %s, %s, %s, %s)
    """, (nombre, category, cost, stock, price))

conn.commit()
cur.close()
conn.close()

print(f"{NUM_REGISTROS} productos insertados correctamente.")
