package iterative.harmony.backend.service

import iterative.harmony.backend.exception.ImportException
import iterative.harmony.backend.model.Franchise
import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.Player
import iterative.harmony.backend.model.SkillGroup
import iterative.harmony.backend.model.Team
import iterative.harmony.backend.model.User
import iterative.harmony.backend.model.base.LongEntity
import iterative.harmony.backend.repository.PlayerIdByUser
import iterative.harmony.backend.repository.PlayerRepository
import iterative.harmony.backend.repository.UserIdByDiscordId
import iterative.harmony.backend.repository.UserRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.Mockito.`when` as whenever
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension

private val now = Instant.parse("2026-10-25T12:00:00Z")

@ExtendWith(MockitoExtension::class)
class PlayerServiceTest {

    @Mock private lateinit var userRepositoryMock: UserRepository
    @Mock private lateinit var playerRepositoryMock: PlayerRepository
    @Spy private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @InjectMocks private lateinit var playerService: PlayerService

    private fun <T : LongEntity> T.withId(id: Long): T {
        val field = LongEntity::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(this, id)
        return this
    }

    private val org = Organization("Minor League Esports", "MLE", "America/New_York").withId(1)
    private val academy = SkillGroup(org, "Academy League", "AL", "url", null).withId(10)
    private val champion = SkillGroup(org, "Champion League", "CL", "url", null).withId(11)
    private val sharks = Franchise(org, "Sharks", "SHK", "url").withId(20)
    private val sharksAcademy = Team(org, academy, sharks, "AL Sharks").withId(30)
    private val user =
        User(
            userId = UUID.randomUUID(),
            username = "someUser",
            displayName = "someUser",
            discordId = "111111111111111111",
            discordAvatarHash = null,
            timeZoneId = null,
            roles = emptySet(),
        )

    private fun context(
        existingPlayers: List<Player> = emptyList(),
        userIdsByDiscordId: Map<String, List<UUID>> = mapOf(user.discordId to listOf(user.userId!!)),
    ) =
        PlayerImportContext(
            org,
            listOf(sharksAcademy),
            listOf(academy, champion),
            listOf(sharks),
            existingPlayers.associate { it.user.userId!! to it.id!! },
            userIdsByDiscordId,
        )

    private fun row(vararg overrides: Pair<String, String>): Map<String, String> =
        mapOf(
            "name" to "someUser",
            "salary" to "13.5",
            "discord_id" to user.discordId,
            "skill_group" to "Academy League",
            "game_title" to "Rocket League",
            "franchise" to "Sharks",
            "Franchise Staff Position" to "Captain",
            "slot" to "PLAYERA",
            "current_scrim_points" to "4",
            "Eligible Through" to "15 Oct 2026",
        ) + overrides

    private fun existingPlayer(team: Team? = sharksAcademy) =
        Player(
                name = "old name",
                organization = org,
                skillGroup = champion,
                team = team,
                user = user,
                teamRole = "Captain",
                slot = "PLAYERB",
            )
            .withId(100)

    private fun stubUserReference() {
        whenever(userRepositoryMock.getReferenceById(user.userId!!)).thenReturn(user)
    }

    private fun stubPlayerLookup(player: Player) {
        whenever(playerRepositoryMock.findById(player.id!!)).thenReturn(Optional.of(player))
    }

    @Nested
    @DisplayName("import")
    inner class Import {

        @Test
        fun `creates a player with team and roster fields when franchise matches a team`() {
            stubUserReference()
            val context = context()
            val batch = mutableListOf<Player>()

            playerService.import(context, batch, row())

            val player = batch.single()
            assertEquals(sharksAcademy, player.team)
            assertEquals(academy, player.skillGroup)
            assertEquals("Captain", player.teamRole)
            assertEquals("PLAYERA", player.slot)
            assertEquals("13.5", player.salary)
            assertEquals(4, player.currentScrimPoints)
            assertEquals("15 Oct 2026", player.eligibleThrough)
            assertEquals(now, player.importedAt)
        }

        @Test
        fun `overwrites an existing player's fields`() {
            val existing = existingPlayer(team = null)
            stubPlayerLookup(existing)
            val batch = mutableListOf<Player>()

            playerService.import(context(listOf(existing)), batch, row())

            assertSame(existing, batch.single())
            assertEquals("someUser", existing.name)
            assertEquals(academy, existing.skillGroup)
            assertEquals(sharksAcademy, existing.team)
            assertEquals("PLAYERA", existing.slot)
            assertEquals("15 Oct 2026", existing.eligibleThrough)
            assertEquals(now, existing.importedAt)
        }

        @Test
        fun `creates only one player when the same user appears twice in a run`() {
            stubUserReference()
            val context = context()
            val batch = mutableListOf<Player>()

            playerService.import(context, batch, row())
            playerService.import(context, batch, row("skill_group" to "Champion League"))

            val player = batch.single()
            assertEquals(1, context.newPlayersByUserId.size)
            assertEquals(champion, player.skillGroup)
        }

        @Test
        fun `loads an existing player once when the user appears twice in a batch`() {
            val existing = existingPlayer()
            stubPlayerLookup(existing)
            val context = context(listOf(existing))
            val batch = mutableListOf<Player>()

            playerService.import(context, batch, row())
            playerService.import(context, batch, row("skill_group" to "Champion League"))

            assertSame(existing, batch.single())
            assertEquals(champion, existing.skillGroup)
            verify(playerRepositoryMock, times(1)).findById(100L)
            assertTrue(context.newPlayersByUserId.isEmpty())
        }

        @Test
        fun `skips rows that are not Rocket League without an error`() {
            val batch = mutableListOf<Player>()

            playerService.import(context(), batch, row("game_title" to "Trackmania"))

            assertTrue(batch.isEmpty())
            verifyNoInteractions(userRepositoryMock)
        }

        @Test
        fun `removes player from team when franchise is FP`() {
            val existing = existingPlayer()
            stubPlayerLookup(existing)
            val batch = mutableListOf<Player>()

            playerService.import(
                context(listOf(existing)),
                batch,
                row("franchise" to "FP", "Franchise Staff Position" to "NA", "slot" to "NONE"),
            )

            assertNull(existing.team)
            assertNull(existing.teamRole)
            assertNull(existing.slot)
        }

        @Test
        fun `removes player from team when franchise is unknown`() {
            val existing = existingPlayer()
            stubPlayerLookup(existing)

            playerService.import(
                context(listOf(existing)),
                mutableListOf(),
                row("franchise" to "Unknown"),
            )

            assertNull(existing.team)
            assertNull(existing.teamRole)
            assertNull(existing.slot)
        }

        @Test
        fun `stores a NONE slot as null`() {
            stubUserReference()
            val batch = mutableListOf<Player>()

            playerService.import(context(), batch, row("slot" to "NONE"))

            assertNull(batch.single().slot)
        }

        @Test
        fun `throws ImportException when discord_id is missing`() {
            val batch = mutableListOf<Player>()

            assertThrows<ImportException> {
                playerService.import(context(), batch, row() - "discord_id")
            }
        }

        @Test
        fun `throws ImportException when no user has the discord_id`() {
            assertThrows<ImportException> {
                playerService.import(
                    context(userIdsByDiscordId = emptyMap()),
                    mutableListOf(),
                    row(),
                )
            }
        }

        @Test
        fun `throws ImportException when the discord_id belongs to more than one user`() {
            val duplicates = mapOf(user.discordId to listOf(user.userId!!, UUID.randomUUID()))

            assertThrows<ImportException> {
                playerService.import(
                    context(userIdsByDiscordId = duplicates),
                    mutableListOf(),
                    row(),
                )
            }
        }

        @Test
        fun `throws ImportException when skill group is unknown`() {

            assertThrows<ImportException> {
                playerService.import(
                    context(),
                    mutableListOf(),
                    row("skill_group" to "Unknown League"),
                )
            }
        }
    }

    @Nested
    @DisplayName("findStalePlayers")
    inner class FindStalePlayers {
        private val lastImportedAt = now.minusSeconds(3600)

        private fun stubStalePlayers(staleIds: List<Long>, totalCount: Long) {
            whenever(playerRepositoryMock.findStalePlayerIds(org, lastImportedAt))
                .thenReturn(staleIds)
            whenever(playerRepositoryMock.countByOrganization(org)).thenReturn(totalCount)
        }

        @Test
        fun `returns players not imported since the last import`() {
            stubStalePlayers(listOf(101L), totalCount = 20)

            assertEquals(listOf(101L), playerService.findStalePlayers(org, lastImportedAt))
        }

        @Test
        fun `returns nothing when too many players are stale`() {
            // 3 of 20 is 15%, above the 10% maximum
            stubStalePlayers(listOf(101L, 102L, 103L), totalCount = 20)

            assertTrue(playerService.findStalePlayers(org, lastImportedAt).isEmpty())
        }

        @Test
        fun `returns nothing when no players are stale`() {
            whenever(playerRepositoryMock.findStalePlayerIds(org, lastImportedAt))
                .thenReturn(emptyList())

            assertTrue(playerService.findStalePlayers(org, lastImportedAt).isEmpty())
            verify(playerRepositoryMock, never()).countByOrganization(org)
        }
    }

    @Nested
    @DisplayName("saveBatch")
    inner class SaveBatch {
        private fun newPlayer() =
            Player(
                name = "new",
                organization = org,
                skillGroup = academy,
                team = null,
                user = user,
                teamRole = null,
            )

        @Test
        fun `saves the batch`() {
            val batch = listOf(existingPlayer(), newPlayer())

            playerService.saveBatch(batch)

            verify(playerRepositoryMock).saveAll(batch)
            verifyNoMoreInteractions(playerRepositoryMock)
        }
    }

    @Nested
    @DisplayName("deleteBatch")
    inner class DeleteBatch {
        @Test
        fun `deletes players by id`() {
            playerService.deleteBatch(listOf(100L))

            verify(playerRepositoryMock).deleteAllByIdInBatch(listOf(100L))
        }
    }

    @Nested
    @DisplayName("buildImportContext")
    inner class BuildImportContext {
        @Test
        fun `keys existing player ids by user id`() {
            val idPair =
                object : PlayerIdByUser {
                    override val userId = user.userId!!
                    override val playerId = 100L
                }
            whenever(playerRepositoryMock.findPlayerIdsByOrganization(org))
                .thenReturn(listOf(idPair))

            val context =
                playerService.buildImportContext(
                    org,
                    listOf(sharksAcademy),
                    listOf(academy),
                    listOf(sharks),
                )

            assertEquals(mapOf(user.userId to 100L), context.playerIdsByUserId)
        }

        @Test
        fun `groups user ids by discord id so duplicates are kept`() {
            val otherUserId = UUID.randomUUID()
            fun idPair(discordId: String, userId: UUID) =
                object : UserIdByDiscordId {
                    override val discordId = discordId
                    override val userId = userId
                }
            whenever(userRepositoryMock.findAllUserIdsByDiscordId())
                .thenReturn(
                    listOf(
                        idPair(user.discordId, user.userId!!),
                        idPair("222", otherUserId),
                        idPair("222", user.userId!!),
                    )
                )

            val context =
                playerService.buildImportContext(org, emptyList(), emptyList(), emptyList())

            assertEquals(
                mapOf(
                    user.discordId to listOf(user.userId),
                    "222" to listOf(otherUserId, user.userId),
                ),
                context.userIdsByDiscordId,
            )
        }
    }
}
