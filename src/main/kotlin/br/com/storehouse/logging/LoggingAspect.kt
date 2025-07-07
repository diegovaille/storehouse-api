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
        val methodSignature = joinPoint.signature as org.aspectj.lang.reflect.MethodSignature
        val className = methodSignature.declaringType.simpleName
        val methodName = methodSignature.name
        val parameterNames = methodSignature.parameterNames
        val args = joinPoint.args

        val paramMap = parameterNames.zip(args).associate { (name, value) ->
            name to (value?.toString() ?: "null")
        }

        val formattedParams = paramMap.entries.joinToString(", ") { "${it.key}=${it.value}" }

        logger.info("➡️ Called $className.$methodName with: [$formattedParams]")

        var result: Any?
        val duration = measureTimeMillis {
            result = joinPoint.proceed()
        }

        logger.info("✅ $className.$methodName completed in ${duration}ms")
        return result
    }
}
