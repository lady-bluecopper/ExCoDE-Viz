FROM tiangolo/uwsgi-nginx-flask:python3.6-alpine3.7

RUN pip install --upgrade pip

# XOLAP Client Requirements
RUN mkdir -p /install
WORKDIR /install

RUN apk --no-cache add lapack
RUN apk --no-cache add --virtual builddeps g++ gfortran musl-dev lapack-dev
RUN apk --no-cache add --virtual builddeps freetype-dev libpng-dev
RUN apk --no-cache add --virtual builddeps jpeg-dev zlib-dev

COPY ./requirements.txt /install/requirements.txt
RUN cat /install/requirements.txt | grep -v '^#'  | grep -v '^-i' | xargs -n 1 pip install

# Flask App Requirements
RUN mkdir -p /app
WORKDIR /app
VOLUME /app
COPY ./app/requirements.txt /app/requirements.txt
RUN cat /app/requirements.txt | grep -v '^#'  | grep -v '^-i' | xargs -n 1 pip install

RUN pip install pylint


# Actual Code goes Last so that previous things do not need to be re-run
COPY ./app /app
COPY ./app/uploads/ /app/uploads/

#RUN pylint /app/xolap /app/core /app/main.py

EXPOSE 80
ENV STATIC_PATH /app/static
ENV FLASK_APP=main.py
ENV FLASK_DEBUG=0

ENTRYPOINT ["flask"]
CMD ["run", "--host", "0.0.0.0", "--port", "80"]