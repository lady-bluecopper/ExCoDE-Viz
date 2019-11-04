package eu.unitn.disi.db.dence.webserver.local;

import static eu.unitn.disi.db.dence.main.Main.runTask;
import eu.unitn.disi.db.dence.webserver.Laucher;
import eu.unitn.disi.db.dence.webserver.utils.Configuration;

/**
 *
 * @author bluecopper
 */
public class TaskRunner implements Runnable {

    public Configuration conf;
    public boolean withGraph;
    
    public TaskRunner(Configuration conf, boolean withGraph) {
        this.conf = conf;
        this.withGraph = withGraph;
    }
    
    @Override
    public void run() {
        String result = runTask(conf, withGraph);
        Laucher.outputs.put(conf.Name, result);
    }

}
