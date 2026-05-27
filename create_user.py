# /// script
# requires-python = ">=3.11"
# dependencies = [
#   "psycopg2-binary",
#   "bcrypt",
#   "python-dotenv",
#   "pyotp",
#   "qrcode",
# ]
# ///

import os
import getpass
import psycopg2
import bcrypt
import pyotp
import qrcode
from dotenv import load_dotenv

load_dotenv()

db_url = os.getenv("DB_URL")
db_user = os.getenv("DB_USER")
db_password = os.getenv("DB_PASSWORD")

# Parse JDBC URL to psycopg2 format: jdbc:postgresql://host:port/dbname
url = db_url.replace("jdbc:postgresql://", "")
host_port, dbname = url.split("/", 1)
host, port = host_port.split(":")

conn = psycopg2.connect(host=host, port=port, dbname=dbname, user=db_user, password=db_password)
cur = conn.cursor()

print("1. Create new user")
print("2. Add TOTP to existing user")
mode = input("Select an option: ").strip()

if mode == "1":
    username = input("Username: ")
    password = getpass.getpass("Password: ")
    enable_totp = input("Enable TOTP? (y/n): ").strip().lower() == "y"

    hashed = bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()

    totp_secret = None
    if enable_totp:
        totp_secret = pyotp.random_base32()
        uri = pyotp.totp.TOTP(totp_secret).provisioning_uri(name=username, issuer_name="Standbase")
        print(f"\nTOTP secret: {totp_secret}")
        print("Scan this QR code with your authenticator app:\n")
        qr = qrcode.QRCode()
        qr.add_data(uri)
        qr.print_ascii(invert=True)
        print()

    cur.execute(
        "INSERT INTO users (username, password, totp_secret, totp_enabled) VALUES (%s, %s, %s, %s)",
        (username, hashed, totp_secret, enable_totp)
    )
    conn.commit()
    print(f"User '{username}' created successfully.")

elif mode == "2":
    username = input("Username: ")

    cur.execute("SELECT id, totp_enabled FROM users WHERE username = %s", (username,))
    row = cur.fetchone()
    if not row:
        print(f"User '{username}' not found.")
    else:
        user_id, totp_enabled = row
        if totp_enabled:
            print(f"User '{username}' already has TOTP enabled.")
        else:
            totp_secret = pyotp.random_base32()
            uri = pyotp.totp.TOTP(totp_secret).provisioning_uri(name=username, issuer_name="Standbase")
            print(f"\nTOTP secret: {totp_secret}")
            print("Scan this QR code with your authenticator app:\n")
            qr = qrcode.QRCode()
            qr.add_data(uri)
            qr.print_ascii(invert=True)
            print()

            cur.execute(
                "UPDATE users SET totp_secret = %s, totp_enabled = %s WHERE id = %s",
                (totp_secret, True, user_id)
            )
            conn.commit()
            print(f"TOTP enabled for '{username}'.")

else:
    print("Invalid option.")

cur.close()
conn.close()
