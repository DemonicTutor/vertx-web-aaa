package com.noenv.markus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.ext.auth.properties.PropertyFileAuthorization;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthorizationHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.BodyHandler;

public final class MainVerticle extends AbstractVerticle {

  public static final int PORT_HTTP_2 = 8443;

  public static final String UPLOAD = "/up";
  public static final String UPLOAD_AUTH = "/up-auth";
  public static final String DOWNLOAD = "/down";
  public static final String DOWNLOAD_AUTH = "/down-auth";

  @Override
  public void start(final Promise<Void> promise) {
    final var router = Router.router(vertx);

    final var basic = BasicAuthHandler.create(PropertyFileAuthentication.create(vertx, "auth.properties"));

    final var roles = AuthorizationHandler.create(RoleBasedAuthorization.create("developer"))
      .addAuthorizationProvider(PropertyFileAuthorization.create(vertx, "auth.properties"));

    final var body = BodyHandler.create();

    router.post(DOWNLOAD_AUTH)
      .handler(basic)
      .handler(roles)
      .handler(this::download);
    router.post(DOWNLOAD)
      .handler(this::download);

    router.post(UPLOAD)
      .handler(body)
      .handler(this::upload);
    router.post(UPLOAD_AUTH)
      .handler(body)
      .handler(basic)
      .handler(roles)
      .handler(this::upload);

    vertx.createHttpServer(new HttpServerOptions()
      .setSsl(true)
      .setUseAlpn(true)
      .setPemKeyCertOptions(new PemKeyCertOptions().setCertPath("server-cert.pem").setKeyPath("server-key.pem"))
    )
      .requestHandler(router)
      .listen(PORT_HTTP_2)
      .<Void>mapEmpty().onComplete(promise);
  }

  private void download(final RoutingContext routingContext) {
    routingContext.response().sendFile("test.txt");
  }

  private void upload(final RoutingContext routingContext) {
    routingContext.response()
      .setStatusCode(!routingContext.fileUploads().isEmpty() ? 200 : 500)
      .end();
  }
}
