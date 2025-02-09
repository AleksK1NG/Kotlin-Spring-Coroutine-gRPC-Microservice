package com.example.grpc.microservice.grpc_microservice

import com.example.microservices.grpc.order.*
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.kotlin.CoroutineContextServerInterceptor
import kotlinx.coroutines.coroutineScope
import net.devh.boot.grpc.client.inject.GrpcClient
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import net.devh.boot.grpc.server.service.GrpcService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.util.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@SpringBootApplication
class GrpcMicroserviceApplication

fun main(args: Array<String>) {
    runApplication<GrpcMicroserviceApplication>(*args)
}


@Component
class OrderClient {

    @GrpcClient("local-server")
    lateinit var orderGrpcClient: OrderServiceGrpcKt.OrderServiceCoroutineStub

    suspend fun getOrder(): GetOrderByIdResponse = coroutineScope {
        val response = orderGrpcClient.getOrderById(getOrderByIdRequest { id = UUID.randomUUID().toString() })
        log.info("gRPC client response: $response")
        response
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}

@GrpcService
class OrderService(private val orderGrpcClient: OrderClient) : OrderServiceGrpcKt.OrderServiceCoroutineImplBase() {

    override suspend fun createOrder(request: CreateOrderRequest): CreateOrderResponse = coroutineScope {
        log.info("gRPC createOrder request: $request")
        val metadata = coroutineContext[GrpcRequestContext]
        log.info("gRPC metadata: $metadata")

        val response = orderGrpcClient.getOrder()
        log.info("gRPC response : $response")

        createOrderResponse {
            order = orderProto {
                id = UUID.randomUUID().toString()
                email = "Alex@gmail.com"
            }
        }
    }

    override suspend fun getOrderById(request: GetOrderByIdRequest): GetOrderByIdResponse = coroutineScope {
        getOrderByIdResponse { order = orderProto { id = request.id } }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}

@Configuration(proxyBeanMethods = false)
class GlobalInterceptorConfiguration {

    @GrpcGlobalServerInterceptor
    fun logInterceptor(): LogInterceptor = LogInterceptor()

    @GrpcGlobalServerInterceptor
    fun logCoroutineInterceptor(): LogCoroutineInterceptor {
        return LogCoroutineInterceptor()
    }
}


//@GrpcGlobalServerInterceptor
class LogInterceptor : ServerInterceptor {

    override fun <ReqT : Any, RespT : Any> interceptCall(
        serverCall: ServerCall<ReqT, RespT>,
        metadata: Metadata,
        callHandler: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        log.info("interceptor call: ${serverCall.methodDescriptor.fullMethodName}")
        return callHandler.startCall(serverCall, metadata)
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}


//@GrpcGlobalServerInterceptor
class LogCoroutineInterceptor : CoroutineContextServerInterceptor() {
    override fun coroutineContext(call: ServerCall<*, *>, headers: Metadata): CoroutineContext {
        log.info("CoroutineContextServerInterceptor headers: ${headers.keys()}")
        val map = mutableMapOf<String, Any?>()
        headers.keys()
            .map { Pair(it, Metadata.Key.of(it, Metadata.ASCII_STRING_MARSHALLER)) }
            .forEach { map[it.first] = headers[it.second] }
        return GrpcRequestContext(map)
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}

data class GrpcRequestContext(val data: MutableMap<String, Any?>) :
    AbstractCoroutineContextElement(GrpcRequestContext) {
    companion object Key : CoroutineContext.Key<GrpcRequestContext>
}