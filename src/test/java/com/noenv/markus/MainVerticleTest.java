package com.noenv.markus;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.multipart.MultipartForm;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import static com.noenv.markus.MainVerticle.*;

@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

  private static final HttpClientOptions optionsHttp2 = new HttpClientOptions()
    .setSsl(true).setUseAlpn(true).setTrustStoreOptions(new JksOptions().setPath("client-truststore.jks").setPassword("wibble"))
    .setVerifyHost(true).setAlpnVersions(Collections.singletonList(HttpVersion.HTTP_2)).setProtocolVersion(HttpVersion.HTTP_2);
  private static final WebClientOptions webOptionsHttp2 = new WebClientOptions()
    .setSsl(true).setUseAlpn(true).setTrustStoreOptions(new JksOptions().setPath("client-truststore.jks").setPassword("wibble"))
    .setVerifyHost(true).setAlpnVersions(Collections.singletonList(HttpVersion.HTTP_2)).setProtocolVersion(HttpVersion.HTTP_2);

  private static String auth() {
    final var auth = "markus" + ":" + "sukram";
    return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
  }

  private Vertx vertx;

  @Before
  public void before(final TestContext context) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(MainVerticle.class, new DeploymentOptions())
      .onComplete(context.asyncAssertSuccess());
  }

  @After
  public void after(final TestContext context) {
    vertx.close().onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void shouldDownload_200OK(final TestContext context) {
    vertx.createHttpClient(optionsHttp2)
      .request(new RequestOptions()
        .setMethod(HttpMethod.POST)
        .setPort(PORT_HTTP_2)
        .setURI(DOWNLOAD)
      )
      .flatMap(HttpClientRequest::send)
      .onSuccess(response -> context.assertEquals(200, response.statusCode()))
      .flatMap(HttpClientResponse::body)
      .onSuccess(body -> context.assertEquals("YPPIE", body.toString().trim()))
      .<Void>mapEmpty()
      .onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void shouldDownload_auth_200OK(final TestContext context) {
    vertx.createHttpClient(optionsHttp2)
      .request(new RequestOptions()
        .setMethod(HttpMethod.POST)
        .setPort(PORT_HTTP_2)
        .setURI(DOWNLOAD_AUTH)
        .setHeaders(MultiMap.caseInsensitiveMultiMap().add(HttpHeaders.AUTHORIZATION, auth()))
      )
      .flatMap(HttpClientRequest::send)
      .onSuccess(response -> context.assertEquals(200, response.statusCode()))
      .flatMap(HttpClientResponse::body)
      .onSuccess(body -> context.assertEquals("YPPIE", body.toString().trim()))
      .<Void>mapEmpty()
      .onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void shouldUpload_200OK(final TestContext context) {
    final var file = new File(getClass().getClassLoader().getResource("logback.xml").getFile());

    WebClient.create(vertx, webOptionsHttp2)
      .request(HttpMethod.POST, new RequestOptions()
        .setPort(PORT_HTTP_2)
        .setURI(UPLOAD)
      )
      .sendMultipartForm(MultipartForm.create()
        .textFileUpload("filename", file.getName(), file.getAbsolutePath(), "plain/text")
      )

      .onSuccess(response -> context.assertEquals(200, response.statusCode()))
      .<Void>mapEmpty()
      .onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void shouldUpload_auth_200OK(final TestContext context) {
    final var file = new File(getClass().getClassLoader().getResource("logback.xml").getFile());

    WebClient.create(vertx, webOptionsHttp2)
      .request(HttpMethod.POST, new RequestOptions()
        .setPort(PORT_HTTP_2)
        .setURI(UPLOAD_AUTH)
        .setHeaders(MultiMap.caseInsensitiveMultiMap().add(HttpHeaders.AUTHORIZATION, auth()))
      )
      .sendMultipartForm(MultipartForm.create()
        .textFileUpload("filename", file.getName(), file.getAbsolutePath(), "plain/text")
      )

      .onSuccess(response -> context.assertEquals(200, response.statusCode()))
      .<Void>mapEmpty()
      .onComplete(context.asyncAssertSuccess());
  }
}
