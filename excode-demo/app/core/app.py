"""
This defines the the Flask App object that handles the Web App
It is instantiated when imported by the main.py
"""

from logging.handlers import RotatingFileHandler
from logging import StreamHandler, Formatter, INFO
from flask import Flask


def init_log():
    """
    Initialize the logger
    """
    log_formatter = Formatter(("%(asctime)s [%(threadName)-12.12s] "
                               "[%(levelname)-5.5s]  %(message)s"))

    file_handler = RotatingFileHandler('/logs/flask-out.log',
                                       maxBytes=10000, backupCount=1)
    file_handler.setLevel(INFO)
    file_handler.setFormatter(log_formatter)
    app.logger.addHandler(file_handler)

    console_handler = StreamHandler()
    console_handler.setFormatter(log_formatter)
    console_handler.setLevel(INFO)
    app.logger.addHandler(console_handler)
    app.logger.setLevel(INFO)
    app.logger.info("Ready!")

app = Flask(__name__, static_folder="/app/static", template_folder='/app/templates')
app.config['UPLOAD_FOLDER'] = '/app/uploads'
app.config.from_object('config')
init_log()
