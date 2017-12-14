package jenkins

import java.util.Map;
import urbanCode.UrbanRequestApplicationProcess;
import es.eci.utils.SystemPropertyBuilder;

SystemPropertyBuilder propBuilder = new SystemPropertyBuilder();

UrbanRequestApplicationProcess command = new UrbanRequestApplicationProcess();

command.initLogger { println it };

propBuilder.populate(command);

command.execute();

