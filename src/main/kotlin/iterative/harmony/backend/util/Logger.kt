package iterative.harmony.backend.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean

/*
 * This is just syntactic sugar to make it faster to declare loggers for each class
 */
@Bean fun Any.getLogger(): Logger = LoggerFactory.getLogger(this.javaClass)
