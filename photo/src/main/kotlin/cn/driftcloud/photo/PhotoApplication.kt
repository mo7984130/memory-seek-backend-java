package cn.driftcloud.photo

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/21
 */
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import java.util.*

@ComponentScan("cn.driftcloud")
@SpringBootApplication
@EnableAsync
class PhotoApplication {

    @PostConstruct
    fun init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

}

fun main(args: Array<String>) {
    runApplication<PhotoApplication>(*args)
}