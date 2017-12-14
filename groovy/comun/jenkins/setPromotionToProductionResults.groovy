import buildtree.BuildBean
import buildtree.BuildTreeHelper
import hudson.model.ParametersAction
import hudson.model.StringParameterValue

/**
 - Promoción a la corriente/rama de PRODUCCION: $RESULT_RTC
 - Promoción a baseline de Kiuwan: $RESULT_KIUWAN
 - Promoción al repositorio Nexus de productivo: $RESULT_NEXUS
 **/

BuildTreeHelper bth = new BuildTreeHelper();

List<BuildBean> treeList = bth.executionTree(build);

String RESULT_RTC;
String RESULT_KIUWAN;
String RESULT_NEXUS;

for(BuildBean buildBean in treeList) {
	if(buildBean.getName().equals("promoteToProduction")) {
		RESULT_RTC = buildBean.getResult();
	}
	if(buildBean.getName().equals("setKiuwanBaseline")) {
		RESULT_KIUWAN = buildBean.getResult();
	}
	if(buildBean.getName().equals("stepPromoteToNexusProductivo")) {
		RESULT_NEXUS = buildBean.getResult();
	}
}

def paramsToSet = [];
paramsToSet.add(new StringParameterValue("RESULT_RTC", RESULT_RTC));
paramsToSet.add(new StringParameterValue("RESULT_KIUWAN", RESULT_KIUWAN));
paramsToSet.add(new StringParameterValue("RESULT_NEXUS", RESULT_NEXUS));

setParams(build, paramsToSet);


public void setParams(build, params) {
	def paramsIn = build?.actions.find{ it instanceof ParametersAction }?.parameters
	def index = build?.actions.findIndexOf{ it instanceof ParametersAction }
	def paramsTmp = []
	if (paramsIn!=null) {
		//No se borra nada para compatibilidad hacia atrás.
		paramsTmp.addAll(paramsIn)
		//Borra de la lista los paramaterAction
		build?.actions.remove(index)
	}
	paramsTmp.addAll(params)

	build?.actions.add(new ParametersAction(paramsTmp))
}



