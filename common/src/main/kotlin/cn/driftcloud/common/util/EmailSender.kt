package cn.driftcloud.common.util

import jakarta.mail.internet.InternetAddress
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/12
 */
@Component
@ConditionalOnClass(JavaMailSender::class)
@ConditionalOnProperty(prefix = "spring.mail", name = ["host"])
class EmailSender(
    private val mailSender: JavaMailSender
) {

    private val log = LoggerFactory.getLogger(EmailSender::class.java)

    /**
     * 发送 HTML 邮件
     * @param to 接收人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param nickname 发送人昵称
     */
    fun sendHtmlMail(
        from: String,
        nickname: String,
        to: String,
        subject: String,
        content: String,
    ) {
        return batchSendHtmlMail(from, nickname, arrayOf(to), subject, content)
    }

    fun batchSendHtmlMail(
        from: String,
        nickname: String,
        to: Array<String>,
        subject: String,
        content: String,
    ) {
        val message = mailSender.createMimeMessage()

        val helper = MimeMessageHelper(message, true, "UTF-8")

        try {
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(content, true)
            helper.setFrom(InternetAddress(from, nickname))
            mailSender.send(message)
        } catch (e: Exception) {
            log.error("发送HTML邮件到 $to 失败", e)
            throw RuntimeException("发送HTML邮件到 $to 失败", e)
        }
    }


}