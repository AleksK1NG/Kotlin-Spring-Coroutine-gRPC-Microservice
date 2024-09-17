package com.example.grpc.microservice.grpc_microservice

import com.example.microservices.grpc.order.*
import kotlinx.coroutines.coroutineScope
import net.devh.boot.grpc.server.service.GrpcService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.*

@SpringBootApplication
class GrpcMicroserviceApplication

fun main(args: Array<String>) {
    runApplication<GrpcMicroserviceApplication>(*args)
}

@GrpcService
class OrderService : OrderServiceGrpcKt.OrderServiceCoroutineImplBase() {
    override suspend fun createOrder(request: CreateOrderRequest): CreateOrderResponse = coroutineScope {
        log.info("gRPC createOrder request: $request")
        createOrderResponse {
            order = orderProto {
                id = UUID.randomUUID().toString()
                email = "Alex@gmail.com"
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}