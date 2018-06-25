#!/bin/bash

rm -f *.class

libs=".:../libs/antlr-runtime-3.5.2.jar:../libs/backport-util-concurrent-3.1.jar:../libs/commons-codec-1.5.jar:../libs/commons-lang-2.6.jar:../libs/commons-logging-1.1.1.jar:../libs/groovy-all-1.6.9.jar:../libs/htmlunit-2.27-OSGi.jar:../libs/ical4j-1.0.2.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar:../libs/natty-0.13.jar:../libs/quartz-1.8.4.jar:../libs/slf4j-api-1.7.10.jar:../libs/slf4j-nop-1.7.10.jar"
# echo 'javac -cp ".:../libs/htmlunit-2.27-OSGi.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar" QARetrieval.java'
# javac -cp ".:../libs/htmlunit-2.27-OSGi.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar" QARetrieval.java

echo 'javac' $libs 'TagMe.java'
javac -cp $libs TagMe.java

echo 'javac used "htmlunit-2.27-OSGi.jar,json-simple-1.1.1.jar,mysql-connector-java-5.1.42-bin.jar,natty-0.13.jar" NTriple.java'
javac -cp $libs NTriple.java

echo 'javac used "htmlunit-2.27-OSGi.jar,json-simple-1.1.1.jar,mysql-connector-java-5.1.42-bin.jar,natty-0.13.jar" MySQLHandler.java'
javac -cp $libs MySQLHandler.java

#echo 'javac -cp ".:../libs/htmlunit-2.27-OSGi.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar" MatchesDBHandler.java'
#javac -cp ".:../libs/htmlunit-2.27-OSGi.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar" MatchesDBHandler.java

echo 'javac used "htmlunit-2.27-OSGi.jar,json-simple-1.1.1.jar,mysql-connector-java-5.1.42-bin.jar,natty-0.13.jar" FreebaseDBHandler.java'
javac -cp $libs FreebaseDBHandler.java

echo 'javac used "htmlunit-2.27-OSGi.jar,json-simple-1.1.1.jar,mysql-connector-java-5.1.42-bin.jar,natty-0.13.jar" MergeSort.java'
javac -cp $libs MergeSort.java

echo 'javac used "htmlunit-2.27-OSGi.jar,json-simple-1.1.1.jar,mysql-connector-java-5.1.42-bin.jar,natty-0.13.jar" Predicates.java'
javac -cp $libs Predicates.java

# echo 'javac -cp ".:../libs/htmlunit-2.27-OSGi.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar" AnswerID.java'
# javac -cp ".:../libs/htmlunit-2.27-OSGi.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar" AnswerID.java

# echo 'javac -cp ".:../libs/htmlunit-2.27-OSGi.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar" QARetrievalMain.java'
# javac -cp ".:../libs/htmlunit-2.27-OSGi.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar" QARetrievalMain.java

# echo 'javac -cp ".:../libs/htmlunit-2.27-OSGi.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar" TagMeMain.java'
# javac -cp ".:../libs/htmlunit-2.27-OSGi.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar" TagMeMain.java

# echo 'javac -cp ".:../libs/htmlunit-2.27-OSGi.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar" Clear.java'
# javac -cp ".:../libs/htmlunit-2.27-OSGi.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar" Clear.java

echo 'javac used "htmlunit-2.27-OSGi.jar,json-simple-1.1.1.jar,mysql-connector-java-5.1.42-bin.jar,natty-0.13.jar" Search.java'
javac -cp $libs Search.java

# echo 'javac -cp ".:../libs/htmlunit-2.27-OSGi.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar" DumbMain.java'
# javac -cp ".:../libs/htmlunit-2.27-OSGi.jar:../libs/json-simple-1.1.1.jar:../libs/mysql-connector-java-5.1.42-bin.jar" DumbMain.java

echo 'javac used "htmlunit-2.27-OSGi.jar,json-simple-1.1.1.jar,mysql-connector-java-5.1.42-bin.jar,natty-0.13.jar" Main.java'
javac -cp $libs Main.java



echo 'all files compiled'
