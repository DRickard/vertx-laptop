# vertx-laptop
Experiments in doing laptops service in Vert.x

maven package to build; java -jar to execute.

You need to be able to connect to Voyager db to execute the app; if you need to use an SSL tunnel to connect, you'll need to edit the JDBC URL on line 30 (https://github.com/DRickard/vertx-laptop/blob/c92b76a38e4653bf12473bf0ea0527a24b089e11/src/main/java/io/vertx/blog/first/LaptopVerticle.java#L30)

Oracle JDBC references: https://docs.oracle.com/cd/E11882_01/appdev.112/e13995/oracle/jdbc/OracleDriver.html, https://docs.oracle.com/cd/B28359_01/java.111/b31224/urls.htm

If app starts properly, browse to http://localhost:8080/laptops/available and you should get JSON output akin to PROD service (https://webservices.library.ucla.edu/laptops/available).
