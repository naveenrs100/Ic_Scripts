package nexus;

import es.eci.utils.SystemPropertyBuilder;
import nexus.PromoteToNexusProductivo;

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
PromoteToNexusProductivo executor = new PromoteToNexusProductivo();
executor.initLogger { println it };
propertyBuilder.populate(executor);

executor.execute();