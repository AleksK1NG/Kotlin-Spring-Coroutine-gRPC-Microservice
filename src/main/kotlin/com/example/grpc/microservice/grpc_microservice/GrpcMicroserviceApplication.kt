package com.example.grpc.microservice.grpc_microservice

import com.example.microservices.grpc.order.*
import kotlinx.coroutines.coroutineScope
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.*

@SpringBootApplication
class GrpcMicroserviceApplication

fun main(args: Array<String>) {
    runApplication<GrpcMicroserviceApplication>(*args)
}

class OrderService : OrderServiceGrpcKt.OrderServiceCoroutineImplBase() {
    override suspend fun createOrder(request: CreateOrderRequest): CreateOrderResponse = coroutineScope {
        createOrderResponse { order = orderProto { id = UUID.randomUUID().toString() } }
    }
}