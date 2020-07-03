set JAVA_HOME=C:\Java\openjdk-14.0.1
set PATH=%JAVA_HOME%\bin;%PATH%

rem httpcomponents as a lib
rem javac --module-path lib;lib\apache;lib\gson;lib\slf4j -d classes src/info/ginj/Ginj.java --source-path src
rem jdeps --module-path lib;lib\apache;lib\gson;lib\slf4j;classes -s --module Ginj
rem jlink --module-path lib;lib\apache;lib\gson;lib\slf4j;classes --add-modules Ginj --output dist

rem httpcomponents unzipped as src
javac --module-path lib;lib\gson;lib\slf4j -d classes src/info/ginj/Ginj.java --source-path src
jdeps --multi-release 14 --module-path lib;lib\gson;lib\slf4j;classes -s --module Ginj
jlink --module-path lib;lib\gson;lib\slf4j;classes --add-modules Ginj --output customJre
