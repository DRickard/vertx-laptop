package io.vertx.blog.first;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;

import java.util.List;
import java.util.stream.Collectors;

public class MyFirstVerticle extends AbstractVerticle {
    private JDBCClient jdbc;
    private JsonObject config;
    private JsonObject output;

    @Override
    public void start(Future<Void> fut) {
        config = new JsonObject()
		.put("url","jdbc:oracle:thin:@ils-db-prod.library.ucla.edu:1521:VGER")
		.put("driver_class", "oracle.jdbc.OracleDriver")
		.put("user", "vger_support")
		.put("password", "vger_support_pwd");
	jdbc = JDBCClient.createShared(vertx, config);
	startBackend(
            (connection) -> confirmConnection(connection,
            (nothing) -> startWebApp(
                (http) -> completeStartup(http, fut)
            ), fut
        ), fut);
    }

    private void startBackend(Handler<AsyncResult<SQLConnection>> next, Future<Void> fut) {
        jdbc.getConnection(ar -> {
            if (ar.failed()) {
                fut.fail(ar.cause());
            } else {
                next.handle(Future.succeededFuture(ar.result()));
            }
        });
    }

    private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
      // Create a router object.
      Router router = Router.router(vertx);

      // Bind "/" to our hello message.
      router.route("/").handler(routingContext -> {
        HttpServerResponse response = routingContext.response();
        response
          .putHeader("content-type", "text/html")
          .end("<h1>Hello from my first Vert.x 3 application</h1>");
      });

      router.get("/laptops/available").handler(this::getAll);

      // Create the HTTP server and pass the "accept" method to the request handler.
      vertx
        .createHttpServer()
        .requestHandler(router::accept)
        .listen(
            // Retrieve the port from the configuration,
            // default to 8080.
            config().getInteger("http.port", 8080),
            next::handle
        );
    }

    private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
      if (http.succeeded()) {
        fut.complete();
      } else {
        fut.fail(http.cause());
      }
    }

    @Override
    public void stop() throws Exception {
      // Close the JDBC client.
      jdbc.close();
    }

    private void getAll(RoutingContext routingContext) {
      jdbc.getConnection(ar -> {
        SQLConnection connection = ar.result();
        connection.query("SELECT \"loc\", \"chromebooks_in\", \"mac_laptops_in\",\"win_laptops_in\", \"ipads_in\" FROM vger_support.clicc_counts ORDER BY \"loc\"", result -> {
          List<AvailableItems> items = result.result().getRows().stream().map(AvailableItems::new).collect(Collectors.toList());
          routingContext.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(items));
          connection.close();
        });
      });
    }

    private void confirmConnection(AsyncResult<SQLConnection> result, Handler<AsyncResult<Void>> next, Future<Void> fut) {
      if (result.failed()) {
        fut.fail(result.cause());
      } else {
        SQLConnection connection = result.result();
        connection.query("SELECT * FROM vger_support.clicc_counts", select -> {
          if (select.failed()) {
            fut.fail(select.cause());
            connection.close();
            return;
          } else {
            next.handle(Future.<Void>succeededFuture());
            connection.close();
          }
        });
      }
    }

}
