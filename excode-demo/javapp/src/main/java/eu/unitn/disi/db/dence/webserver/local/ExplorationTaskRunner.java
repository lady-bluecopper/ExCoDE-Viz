package eu.unitn.disi.db.dence.webserver.local;

import static eu.unitn.disi.db.dence.main.Main.runExplorationTask;
import eu.unitn.disi.db.dence.webserver.utils.Configuration;
import java.util.concurrent.Callable;

/**
 *
 * @author bluecopper
 */
public class ExplorationTaskRunner implements Callable<String> {
    
    Configuration config;
    
    public ExplorationTaskRunner(Configuration config) {
        this.config = config;
    }

    @Override
    public String call() throws Exception {
        return runExplorationTask(config);
    }
    
}
