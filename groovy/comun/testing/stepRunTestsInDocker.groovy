package testing

import es.eci.utils.SystemPropertyBuilder;
import rtc.RTCUtils;
import testing.commands.RunTestsCommand;

/**
 * Funcionalidad de descarga de RTC implementada en groovy.
 * Ver la parametrización en RTCDownloaderCommand.groovy.
 */

// Obligatorios hasta el décimo

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
def params = propertyBuilder.getSystemParameters();

def aplicacion = params["aplicacion"];
def instantanea = params["instantanea"];
def testParams = params["testParams"];
def volumePath = params["volumePath"];
def nexusUrl = params["nexusUrl"];
def dockerRegistry = params["dockerRegistry"];
def urbanGroupId = params["urbanGroupId"];
def gitGroup = params["gitGroup"];

RunTestsCommand command = new RunTestsCommand(
			aplicacion,
			instantanea,
			nexusUrl,
			testParams,
			volumePath,
			dockerRegistry,
			urbanGroupId,
			gitGroup
			);

command.initLogger { println it };

command.execute();
