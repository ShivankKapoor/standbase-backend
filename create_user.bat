@echo off
python -m venv .venv
call .venv\Scripts\activate
pip install -q psycopg2-binary bcrypt python-dotenv
python create_user.py
deactivate
