import os
import io
import json
import requests
import time
import copy
from flask import render_template, request, send_file, flash
from core.app import app
from core import utils
from werkzeug.utils import secure_filename


# IP_ADDRESS='192.168.178.103'
# IP_ADDRESS='10.196.198.63'
IP_ADDRESS = 'host.docker.internal'

configurations = {}
datasets = set()


# Global Utilities for Routes
@app.after_request
def after_request(response):
    """
    Adds HTTP headers for Cross origin
    """
    response.headers.add('Access-Control-Allow-Origin', '*')
    response.headers.add('Access-Control-Allow-Headers',
                         'Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin')
    response.headers.add('Access-Control-Allow-Methods',
                         'GET,PUT,POST,DELETE,OPTIONS')
    response.headers.add("Cache-Control", "no-cache, no-store, must-revalidate, public, max-age=0")
    response.headers.add("Expires", 0)
    response.headers.add("Pragma", "no-cache")
    return response


@app.after_request
def add_header(response):
    """
    Adds HTTP headers for Cache Max Age
    """
    response.cache_control.max_age = 100
    return response


# Routes Defined Here
@app.route('/')
def home():
    """
    The Home Page
    """
    app.logger.info("RENDERING: {}".format(len(datasets)))
    return render_template('index.html',
                           config_value="",
                           dataset_value="",
                           dataset_selected="nop",
                           options=datasets,
                           path_value="",
                           correlation_value="0.5",
                           average_checked=True,
                           minimum_checked=False,
                           density_value="1",
                           edgesxsnaps_value="1",
                           size_range_value="1",
                           epsilon_value=1,
                           size_checked=True)


@app.route('/index')
def index_page():
    """
    The Home Page
    """
    return home()


@app.route('/index.html')
def index():
    """
    The Home Page
    """
    return home()


@app.route('/start')
def start_page():
    """
    The Start Page for the demonstration
    """
    return render_template('start.html')


@app.route('/start.html')
def start():
    """
    The Start Page for the demonstration
    """
    return start_page()


@app.route('/statistics', methods=['POST'])
def compute_stats():
    if request.method == 'POST':
        app.logger.info(request.form)
        if request.form["fileGroup"] == "local":
            uploaded_files = request.files.getlist("file[]")
            if len(uploaded_files) == 0:
                return "No File Selected!"
            for file in uploaded_files:
                if file:
                    filename = secure_filename(file.filename)
                    if filename == "":
                        return "Empty File Field!"
                    if "mapping" not in file.filename:
                        file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
                    file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
                    app.logger.info("SAVING FILE:" + filename)
        else:
            file_path = os.path.join(app.config['UPLOAD_FOLDER'], request.form['path_remote'])
            app.logger.info("remote file_path " + file_path)
        utils.compute_stats(file_path)
    return "Done"


@app.route('/getChartData', methods=['GET'])
def get_chart_data():
    if request.method == 'GET':
        name = request.args.get('name')
        task = request.args.get('chart')
        if (task == '1'):
            with io.open(os.path.join(app.config['UPLOAD_FOLDER'], name + "_general_stats.csv")) as f:
                return f.read()
        if (task == '2'):
            with io.open(os.path.join(app.config['UPLOAD_FOLDER'], name + "_snap_stats.csv")) as f:
                return f.read()
        if (task == '3'):
            with io.open(os.path.join(app.config['UPLOAD_FOLDER'], name + "_degrees.csv")) as f:
                return f.read()
        if (task == '4'):
            with io.open(os.path.join(app.config['UPLOAD_FOLDER'], name + "_num_snap_count_edges.csv")) as f:
                return f.read()
        if (task == '5'):
            with io.open(os.path.join(app.config['UPLOAD_FOLDER'], name + "_edges_per_snapshot.csv")) as f:
                return f.read()
        if (task == '6'):
            with io.open(os.path.join(app.config['UPLOAD_FOLDER'], name + "_degrees_per_snapshot.csv")) as f:
                return f.read()
    return "Done"


@app.route('/saveDatasetName', methods=['POST'])
def save_dataset_name():
    if request.method == 'POST':
        name = request.args.get('name')
        datasets.add(name)
        return "Dataset name saved"
    return "Dataset name not saved"


@app.route('/saveConfiguration', methods=['POST'])
def save_configuration():
    if request.method == 'POST':
        app.logger.info(request.data)
        data = json.loads(request.data)
        app.logger.info(data)
        name = data['Name'].lower()
        if name not in configurations:
            configurations[name] = data
            return "true"
        else:
            configurations[name] = data
    return "false"


@app.route('/deleteConfiguration', methods=['POST'])
def delete_configuration():
    name = request.args.get('name').lower()
    app.logger.info('Delete Configuration: {}'.format(name))
    if name in configurations:
        del configurations[name]
        return "true"
    return "false"


@app.route('/getConfiguration', methods=['GET'])
def get_configurations():
    name = request.args.get('name').lower()
    app.logger.info('Request Configuration: {}'.format(name))
    config_value = ""
    dataset_value = ""
    dataset_selected = "nop"
    path_value = ""
    correlation_value = "0.5"
    average_checked = True
    minimum_checked = False
    density_value = "1"
    edgesxsnaps_value = "1"
    size_range_value = "1"
    epsilon_value = 1
    size_checked = True
    if name in configurations:
        config = configurations[name]
        config_value = config['Name']
        dataset_value = config['Dataset']['Name']
        dataset_selected = config['Dataset']['Path']
        path_value = config['Dataset']['Path']
        correlation_value = config['Task']['Correlation']
        if config['Task']['DensityFunction'] == "Average":
            average_checked = True
            minimum_checked = False
        else:
            average_checked = False
            minimum_checked = True
        density_value = config['Task']['Density']
        edgesxsnaps_value = config['Task']['EdgesPerSnapshot']
        epsilon_value = config['Task']['Epsilon']
        if int(config['Task']['MaxSize']) == 10000:
            size_checked = True
            size_range_value = "1"
        else:
            size_checked = False
            size_range_value = config['Task']['MaxSize']
    return render_template('index.html',
                    config_value=config_value,
                    dataset_value=dataset_value,
                    dataset_selected=dataset_selected,
                    options=datasets,
                    path_value=path_value,
                    correlation_value=correlation_value,
                    average_checked=average_checked,
                    minimum_checked=minimum_checked,
                    density_value=density_value,
                    edgesxsnaps_value=edgesxsnaps_value,
                    size_range_value=size_range_value,
                    epsilon_value=epsilon_value,
                    size_checked=size_checked)


@app.route('/loadConfigurations', methods=['GET'])
def load_configurations():
    app.logger.info('Request Configurations')
    return json.dumps(configurations)


@app.route('/results', methods=['GET', 'PUT', 'POST', 'DELETE', 'OPTIONS'])
def results():
    if request.method == 'POST':
        app.logger.info(request.form)
        name = request.form['config'].lower()
        path_name = ""
        if request.form['fileGroup'] == 'local':
            files = request.form['path_local'].split(",")
            for file in files:
                if "mapping" not in file:
                    path_name = file.strip()
                    break
        else:
            path_name = request.form['path_remote']
        if utils.get_graph_size(path_name) > 4000:
            with_graph = 'false'
        else:
            with_graph = 'true'
        if name not in configurations:
            requests.post('http://{}:8082/launchTask'.format(IP_ADDRESS), data=json.dumps(request.form), params={'withGraph': with_graph, 'form': 'true'})
        else:
            requests.post('http://{}:8082/launchTask'.format(IP_ADDRESS), data=json.dumps(configurations[name]), params={'withGraph': with_graph, 'form': 'false'})
        doc = {
            'config': request.form['config'],
            'name': request.form['dataset'],
            'path_name': path_name
        }
        return render_template('results.html', doc=doc)
    doc = {
        'title': 'Server Error',
        'header': 'The application has encountered an error'
    }
    return render_template('error.html', doc=doc), 500


@app.route('/getResults', methods=['GET'])
def get_results():
    name = request.args.get('name')
    app.logger.info('Getting results of configuration {}'.format(name))
    result = 'NONE'
    while result == 'NONE' or result.text == 'UNAVAILABLE':
        time.sleep(2)
        result = requests.get('http://{}:8082/getResults'.format(IP_ADDRESS), params={'name': name})
    app.logger.info('Received text: `{}`'.format(result.text))
    return result.text


@app.route('/getGraph', methods=['GET'])
def get_graph_data():
    dataset = request.args.get('name')
    graph = utils.create_JSON_graph_string(dataset)
    return json.dumps(graph)


@app.route('/exploreSubgraph', methods=['POST'])
def explore_subgraph():
    if request.method == 'POST':
        name = request.args.get('name').lower()
        data = request.data.decode('utf-8')
        app.logger.info(data)
        app.logger.info('Request exploration of subgraph of configuration ' + name)
        if name not in configurations:
            requests.post('http://{}:8082/launchExplorationTask'.format(IP_ADDRESS), data=data, params={'form': 'true'})
        else:
            config = copy.deepcopy(configurations[name])
            jdata = json.loads(data)
            config['Subgraph'] = jdata['Subgraph']
            requests.post('http://{}:8082/launchExplorationTask'.format(IP_ADDRESS), data=json.dumps(config), params={'form': 'false'})
        return 'Done'
    doc = {
        'title': 'Server Error',
        'header': 'The application has encountered an error'
    }
    return render_template('error.html', doc=doc), 500


@app.route('/getExplodedData', methods=['GET'])
def get_exploded_data():
    result = 'NONE'
    while result == 'NONE' or result.text == 'UNAVAILABLE':
        time.sleep(2)
        result = requests.get('http://{}:8082/getExplodedData'.format(IP_ADDRESS))
    app.logger.info('Received text: `{}`'.format(result.text))
    return result.text


# Catch all routes
@app.route('/<path:fpath>', methods=['GET', 'PUT', 'POST', 'DELETE', 'OPTIONS'])
def route_frontend(fpath):
    """
    The Catch all route
    Everything not declared before (not a Flask route / API endpoint)...
    """
    app.logger.info("route_frontend")
    doc = {
        'title': 'Home',
        'header': 'The Page Title'
    }
    if '..' in fpath:
        doc['title'] = 'Access Denied'
        doc['header'] = 'The page requested cannot be accessed'

        app.logger.error("Illegal Request! Page '%s'", fpath)
        return render_template('error.html', doc=doc), 403

    # ...could be a static file needed by the front end that
    # doesn't use the `static` path (like in `<script src="bundle.js">`)
    file_path = os.path.join(app.static_folder, fpath)
    app.logger.info("Search for '%s' PATH '%s' into STATIC FOLDER %s ",
                    request.method, fpath, app.static_folder)

    if os.path.isfile(file_path):
        return send_file(file_path)

    doc['title'] = 'Page not Found'
    doc['header'] = "The page requested cannot be found on the server"

    app.logger.error("Page '%s' not found", fpath)
    return render_template('error.html', doc=doc), 404


# Error Pages
@app.errorhandler(500)
def coding_error(error):
    """
    Error 500 page
    """
    doc = {
        'title': 'Server Error',
        'header': 'The application has encountered an error'
    }
    app.logger.error("Raising error 500 '%s'", error)
    return render_template('error.html', doc=doc), 500


@app.errorhandler(404)
def page_not_found(error):
    """
    Error 404 page
    """
    doc = {
        'title': 'Page not Found',
        'header': "The page requested cannot be found on the server"
    }

    app.logger.error("Raising error 404 '%s'", error)
    return render_template('error.html', doc=doc), 404
