package br.com.storehouse.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(
    scanBasePackages = [
        "br.com.storehouse",
        "br.com.pinguimice.admin"
    ]
)
@EntityScan(
    basePackages = [
        "br.com.storehouse.data.entities",
        "br.com.pinguimice.admin.entity"
    ]
)
@EnableJpaRepositories(
    basePackages = [
        "br.com.storehouse.data.repository",
        "br.com.pinguimice.admin.repository"
    ]
)
class TestApplication
