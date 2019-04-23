package io.acari.http

import io.acari.model.TestObject
import io.acari.util.POKOCodec
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.micrometer.PrometheusScrapingHandler
import io.vertx.reactivex.MaybeHelper

fun mountSupportingRoutes(vertx: Vertx, router: Router, configuration: JsonObject): Router {
  router.route("/metrics").handler(PrometheusScrapingHandler.create())

  attachTestRouter(vertx, router)

  return router
}

private fun attachTestRouter(vertx: Vertx, router: Router) {
  val eventBus = vertx.eventBus()
  eventBus.registerDefaultCodec(
    TestObject::class.java, POKOCodec(
      { json, testObject ->
        json.put("message", testObject.message)
          .put("issuedDate", testObject.issuedDate)
      },
      { jsonObject ->
        TestObject(jsonObject.getString("message"), jsonObject.getInstant("issuedDate"))
      },
      TestObject::class.java.name
    )
  )

  eventBus.consumer<TestObject>("test.message") { message ->
    println("I got dis from da queue ${message.body()}")
    message.reply(TestObject("Very Nice!"))
  }

  eventBus.consumer<TestObject>("test.message") { message ->
    println("Holy Shit! A Message: ${message.body()}")
    message.reply(TestObject("Interesting!"))
  }

  router.get("/testo").handler {
    MaybeHelper.toMaybe<Message<TestObject>> {
      eventBus.send("test.message", TestObject("Gib de pussi b0ss"), it)
    }.subscribe { message ->
      println("I got a response! ${message.body()}")
    }
    it.response().setStatusCode(200).end("Hey!\n")
  }

  router.get("/publisho").handler {

      eventBus.publish("test.message", TestObject("Whaddup Pimps?"))

    it.response().setStatusCode(200).end("Hey!\n")
  }
}
