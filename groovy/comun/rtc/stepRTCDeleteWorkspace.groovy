package rtc

import es.eci.utils.SystemPropertyBuilder;
import rtc.RTCUtils;

import rtc.commands.RTCDeleteRepositoryWorkspace;

/**
 * Funcionalidad de borrado de workspace de fix de RTC implementada en groovy.
 * Ver la parametrización en RTCDeleteRepositoryWorkspace.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
def systemParameters = propertyBuilder.getSystemParameters();

def stream = systemParameters.get("stream");
def workspace = "WSR - ${stream} - ADDFIX - IC"

RTCDeleteRepositoryWorkspace command = new RTCDeleteRepositoryWorkspace();
command.initLogger { println it }
command.setLight(true);
command.setPwdRTC(systemParameters.get("pwdRTC"));
command.setUrlRTC(systemParameters.get("urlRTC"));
command.setUserRTC(systemParameters.get("userRTC"));
command.setWorkspaceRTC(workspace);
command.setScmToolsHome(systemParameters.get("scmToolsHome"));

command.execute();

