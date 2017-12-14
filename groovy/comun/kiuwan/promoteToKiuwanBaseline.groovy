import es.eci.utils.SystemPropertyBuilder
import kiuwan.KiuwanPromoteBaseline;

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
KiuwanPromoteBaseline executor = new KiuwanPromoteBaseline();
executor.initLogger { println it }
propertyBuilder.populate(executor);

executor.execute();