package aix
class UndefinedEnvironment extends Exception {
     GString msg;
     UndefinedEnvironment(GString msg) {
         this.msg=msg;
     }
}