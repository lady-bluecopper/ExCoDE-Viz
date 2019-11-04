package eu.unitn.disi.db.dence.webserver;

import com.google.gson.Gson;
import com.koloboke.collect.map.hash.HashObjObjMap;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import eu.unitn.disi.db.dence.webserver.local.ExplorationTaskRunner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import eu.unitn.disi.db.dence.webserver.local.TaskRunner;
import eu.unitn.disi.db.dence.webserver.utils.Configuration;
import eu.unitn.disi.db.dence.webserver.utils.Form;
import static spark.Spark.*;

/**
 *
 * @author bluecopper
 */
public class Laucher {

//    static String DATA_FOLDER = "/Users/bluecopper/Documents/PhD/CorrelatedEvents/2018-correlated-events-in-temporal-networks-demo/densecorrelatededgefinderdemo/app/uploads/";
    static String DATA_FOLDER = "/app/uploads/";
    static Gson parser = new Gson();
    static ExecutorService service = Executors.newFixedThreadPool(5);
    static int counter = 0;
    
    static HashObjObjMap<String, Configuration> configurations = HashObjObjMaps.newMutableMap();
    public static HashObjObjMap<String, String> outputs = HashObjObjMaps.newMutableMap();
    public static String explorationOutput = "UNAVAILABLE";

    public static void main(String[] args) {
        port(8082);

        post("/launchTask", (request, response) -> {
            System.out.println("Received: " + request.body());
            Configuration conf;
            boolean isForm = Boolean.valueOf(request.queryParams("form"));
            if (isForm) {
                Form form = parser.fromJson(request.body(), Form.class);
                conf = form.createConfiguration();
                configurations.put(conf.Name.toLowerCase(), conf);
            }
            else {
                conf = parser.fromJson(request.body(), Configuration.class);
            }
            conf.Dataset.Path = DATA_FOLDER + conf.Dataset.Path;
            boolean withGraph = Boolean.valueOf(request.queryParams("withGraph"));
            new Thread(new TaskRunner(conf, withGraph)).start();
            System.out.println("Task Submitted.");
            response.body("ack");
            return "ack";
        });
        
        post("/launchExplorationTask", (request, response) -> {
            System.out.println("Received: " + request.body());
            Configuration conf;
            boolean isForm = Boolean.valueOf(request.queryParams("form"));
            if (isForm) {
                Configuration temp = parser.fromJson(request.body(), Configuration.class);
                conf = configurations.get(temp.Name.toLowerCase());
                conf.Subgraph = temp.Subgraph;
            } else {
                conf = parser.fromJson(request.body(), Configuration.class);
                conf.Dataset.Path = DATA_FOLDER + conf.Dataset.Path;
            }
            ExplorationTaskRunner task = new ExplorationTaskRunner(conf);
            explorationOutput = "UNAVAILABLE";
            System.out.println("Exploration Task Created with Configuration: " + conf.toString());
            Future<String> output = service.submit(task);
            System.out.println("Exploration Task Submitted.");
            explorationOutput = output.get();
            response.body("ack");
            return "ack";
        });
        
        get("/getResults", (request, response) -> {
            counter++;
            String configName = request.queryParams("name");
            if (outputs.containsKey(configName)) {
                response.body(outputs.get(configName));
                return outputs.get(configName);
            } else {
                response.body("UNAVAILABLE");
                return "UNAVAILABLE";
            }
        });
        
        get("/getExplodedData", (request, response) -> {
            response.body(explorationOutput);
            return explorationOutput;
        });
    }
    
}
