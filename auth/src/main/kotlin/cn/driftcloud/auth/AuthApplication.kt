package cn.driftcloud.auth

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import java.util.*

@SpringBootApplication
@ComponentScan("cn.driftcloud")
class AuthApplication{

    @PostConstruct
    fun init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

}

fun main(args: Array<String>) {
    runApplication<AuthApplication>(*args)
}