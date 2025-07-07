package br.com.storehouse.logging

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Aspect
@Component
class LoggingAspect {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Around("@annotation(br.com.storehouse.logging.LogCall)")
    fun logMethodCall(joinPoint: ProceedingJoinPoint): Any? {
        val className = joinPoint.signature.declaringType.simpleName
        val methodName = joinPoint.signature.name
        val args = joinPoint.args.joinToString(", ") { it.toString() }

        logger.info("➡️ Called $className.$methodName with args: [$args]")

        var result: Any?
        val duration = measureTimeMillis {
            result = joinPoint.proceed()
        }

        logger.info("✅ $className.$methodName completed in ${duration}ms")
        return result
    }
}
