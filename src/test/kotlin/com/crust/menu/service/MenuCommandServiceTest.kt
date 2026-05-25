package com.crust.menu.service

import com.crust.menu.domain.MenuVersion
import com.crust.menu.domain.exception.NoFallbackMenuException
import com.crust.menu.repository.MenuVersionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
class MenuCommandServiceTest {

    @Autowired
    private lateinit var menuCommandService: MenuCommandService

    @Autowired
    private lateinit var menuVersionRepository: MenuVersionRepository

    @BeforeEach
    fun setup() {
        menuVersionRepository.deleteAll()
    }

    @Test
    fun `Revert when a prior PUBLISHED version exists - that version remains active`() {
        val priorPublished = menuVersionRepository.save(MenuVersion(status = "PUBLISHED", createdAt = LocalDateTime.now().minusDays(2)))
        val currentVersion = menuVersionRepository.save(MenuVersion(status = "PUBLISHED", createdAt = LocalDateTime.now()))

        menuCommandService.revertMenuVersion(currentVersion.id)

        val reverted = menuVersionRepository.findById(currentVersion.id).get()
        assertEquals("REVERTED_DUE_TO_RISK", reverted.status)

        val prior = menuVersionRepository.findById(priorPublished.id).get()
        assertEquals("PUBLISHED", prior.status)
    }

    @Test
    fun `Revert when only DRAFT versions exist - most recent DRAFT becomes PUBLISHED`() {
        val oldDraft = menuVersionRepository.save(MenuVersion(status = "DRAFT", createdAt = LocalDateTime.now().minusDays(3)))
        val recentDraft = menuVersionRepository.save(MenuVersion(status = "DRAFT", createdAt = LocalDateTime.now().minusDays(1)))
        val currentVersion = menuVersionRepository.save(MenuVersion(status = "PUBLISHED", createdAt = LocalDateTime.now()))

        menuCommandService.revertMenuVersion(currentVersion.id)

        val reverted = menuVersionRepository.findById(currentVersion.id).get()
        assertEquals("REVERTED_DUE_TO_RISK", reverted.status)

        val promoted = menuVersionRepository.findById(recentDraft.id).get()
        assertEquals("PUBLISHED", promoted.status)

        val old = menuVersionRepository.findById(oldDraft.id).get()
        assertEquals("DRAFT", old.status)
    }

    @Test
    fun `Revert when no other versions exist - NoFallbackMenuException thrown, no state change`() {
        val currentVersion = menuVersionRepository.save(MenuVersion(status = "PUBLISHED", createdAt = LocalDateTime.now()))

        assertThrows<NoFallbackMenuException> {
            menuCommandService.revertMenuVersion(currentVersion.id)
        }

        val unchanged = menuVersionRepository.findById(currentVersion.id).get()
        assertEquals("PUBLISHED", unchanged.status)
    }
}
