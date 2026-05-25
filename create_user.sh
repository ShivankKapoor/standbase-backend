#!/bin/bash
python3 -m venv .venv
source .venv/bin/activate
pip install -q psycopg2-binary bcrypt python-dotenv
python3 create_user.py
deactivate
