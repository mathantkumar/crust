package com.crust.menu

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import com.crust.menu.service.MenuAuditConsumer
import com.crust.menu.service.OutboxRelay

@SpringBootTest
class MenuGraphApplicationTests {

    @MockitoBean
    lateinit var menuAuditConsumer: MenuAuditConsumer

    @MockitoBean
    lateinit var outboxRelay: OutboxRelay

    @Test
    fun contextLoads() {
    }

}
