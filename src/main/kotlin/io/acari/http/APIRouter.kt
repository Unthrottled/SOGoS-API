package io.acari.http

import io.acari.memory.user.UserInformationFinder
import io.acari.security.createVerificationHandler
import io.acari.user.UserService
import io.acari.util.loggerFor
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router
import software.amazon.awssdk.services.s3.presigner.S3Presigner

private val logger = loggerFor("APIRouter")

fun mountAPIRoute(
    vertx: Vertx,
    mongoClient: MongoClient,
    router: Router,
    presigner: S3Presigner
): Router {
  router.mountSubRouter("/", createAPIRoute(vertx, mongoClient, presigner))

  // Static content path must be mounted last, as a fall back
  router.get("/*")
    .failureHandler { routingContext ->
      val statusCode = routingContext.statusCode()
      if(statusCode != 401 && statusCode != 403){
        routingContext.reroute("/")
      } else {
        routingContext.response().setStatusCode(404).end()
      }
    }

  return router
}


fun createAPIRoute(
  vertx: Vertx,
  mongoClient: MongoClient,
  presigner: S3Presigner
): Router {
  val router = Router.router(vertx)
  router.get("/user").handler(createUserHandler(UserService(UserInformationFinder(mongoClient, vertx))))
  router.mountSubRouter("/history", createHistoryRoutes(vertx, mongoClient))
  router.route().handler(createVerificationHandler())// order is important here
  router.mountSubRouter("/user", createAuthorizedUserRoutes(vertx, mongoClient, presigner))
  router.mountSubRouter("/activity", createActivityRoutes(vertx, mongoClient))
  router.mountSubRouter("/strategy", createStrategyRoutes(vertx, mongoClient))
  router.mountSubRouter("/tactical", createTacticalRoutes(vertx, mongoClient))
  return router
}
