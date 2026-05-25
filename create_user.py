# /// script
# requires-python = ">=3.11"
# dependencies = [
#   "psycopg2-binary",
#   "bcrypt",
#   "python-dotenv",
# ]
# ///

import os
import getpass
import psycopg2
import bcrypt
from dotenv import load_dotenv

load_dotenv()

db_url = os.getenv("DB_URL")
db_user = os.getenv("DB_USER")
db_password = os.getenv("DB_PASSWORD")

# Parse JDBC URL to psycopg2 format: jdbc:postgresql://host:port/dbname
url = db_url.replace("jdbc:postgresql://", "")
host_port, dbname = url.split("/", 1)
host, port = host_port.split(":")

username = input("Username: ")
password = getpass.getpass("Password: ")

hashed = bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()

conn = psycopg2.connect(host=host, port=port, dbname=dbname, user=db_user, password=db_password)
cur = conn.cursor()
cur.execute("INSERT INTO users (username, password) VALUES (%s, %s)", (username, hashed))
conn.commit()
cur.close()
conn.close()

print(f"User '{username}' created successfully.")
