package cn.driftcloud.gateway

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import java.util.*

@ComponentScan(basePackages = ["cn.driftcloud"])
@SpringBootApplication
open class GatewayApplication{

    @PostConstruct
    fun init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

}

fun main(args: Array<String>) {
    runApplication<GatewayApplication>(*args)
}
