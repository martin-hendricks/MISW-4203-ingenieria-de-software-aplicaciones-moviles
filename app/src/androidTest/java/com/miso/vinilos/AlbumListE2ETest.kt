package com.miso.vinilos

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.miso.vinilos.config.TestRetrofitClient
import com.miso.vinilos.helpers.FakeImageLoader
import com.miso.vinilos.helpers.JsonResponseHelper
import com.miso.vinilos.helpers.TestDataFactory
import com.miso.vinilos.matchers.CustomMatchers
import androidx.room.Room
import com.miso.vinilos.model.database.VinylRoomDatabase
import com.miso.vinilos.rules.MockWebServerRule
import com.miso.vinilos.rules.ScreenshotTestRule
import com.miso.vinilos.rules.TestDispatcherRule
import com.miso.vinilos.views.screens.AlbumListScreen
import com.miso.vinilos.views.theme.VinilosTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.miso.vinilos.viewmodels.AlbumViewModel
import com.miso.vinilos.viewmodels.ProfileViewModel
import coil.Coil

@RunWith(AndroidJUnit4::class)
@LargeTest
class AlbumListE2ETest {

    @get:Rule(order = 0)
    val testDispatcherRule = TestDispatcherRule()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule(order = 2)
    val mockWebServerRule = MockWebServerRule()

    @get:Rule(order = 3)
    val screenshotTestRule = ScreenshotTestRule().apply {
        setComposeTestRule(composeTestRule)
    }

    private var testDatabase: VinylRoomDatabase? = null

    @org.junit.Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        Coil.setImageLoader(FakeImageLoader(application))
    }

    @After
    fun tearDown() {
        testDatabase?.close()
        testDatabase = null
        Coil.reset()
    }

    private fun createTestViewModel(): AlbumViewModel {
        val testApiService = TestRetrofitClient.createTestApiService(mockWebServerRule.baseUrl)
        val application = ApplicationProvider.getApplicationContext<Application>()
        testDatabase = Room.inMemoryDatabaseBuilder(
            application,
            VinylRoomDatabase::class.java
        ).allowMainThreadQueries().build()
        val albumsDao = testDatabase!!.albumsDao()
        val testRepository = com.miso.vinilos.model.repository.AlbumRepository(
            application,
            albumsDao,
            testApiService
        )
        return AlbumViewModel(testRepository, testDispatcherRule.testDispatcher)
    }

    @Test
    fun testSuccessfulAlbumListLoading() {
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(JsonResponseHelper.createAlbumsSuccessResponse(testAlbums))

        val testViewModel = createTestViewModel()

        val application = ApplicationProvider.getApplicationContext<Application>()
        val profileViewModel = ProfileViewModel(application)

        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumListScreen(
                    albumViewModel = testViewModel,
                    profileViewModel = profileViewModel,
                    onAlbumClick = {},
                    onAddAlbum = {},
                    enableLifecycleRefresh = false
                )
            }
        }

        composeTestRule.waitForIdle()

        CustomMatchers.verifyAlbumsTitleIsVisible(composeTestRule)
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "The Dark Side of the Moon")
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Led Zeppelin IV")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "The Beatles")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "Pink Floyd")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "Led Zeppelin")

        screenshotTestRule.takeScreenshot("01-lista-exitosa")
    }

    @Test
    fun testErrorStateDisplay() {
        mockWebServerRule.server.enqueue(JsonResponseHelper.createServerErrorResponse())

        val testViewModel = createTestViewModel()

        val application = ApplicationProvider.getApplicationContext<Application>()
        val profileViewModel = ProfileViewModel(application)

        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumListScreen(
                    albumViewModel = testViewModel,
                    profileViewModel = profileViewModel,
                    onAlbumClick = {},
                    onAddAlbum = {},
                    enableLifecycleRefresh = false
                )
            }
        }

        composeTestRule.waitForIdle()

        CustomMatchers.verifyErrorMessageIsVisible(composeTestRule)
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)

        screenshotTestRule.takeScreenshot("estado-error")
    }

    @Test
    fun testErrorRetryFunctionality() {
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(JsonResponseHelper.createServerErrorResponse())
        mockWebServerRule.server.enqueue(JsonResponseHelper.createAlbumsSuccessResponse(testAlbums))

        val testViewModel = createTestViewModel()

        val application = ApplicationProvider.getApplicationContext<Application>()
        val profileViewModel = ProfileViewModel(application)

        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumListScreen(
                    albumViewModel = testViewModel,
                    profileViewModel = profileViewModel,
                    onAlbumClick = {},
                    onAddAlbum = {},
                    enableLifecycleRefresh = false
                )
            }
        }

        composeTestRule.waitForIdle()

        CustomMatchers.verifyErrorMessageIsVisible(composeTestRule)
        screenshotTestRule.takeScreenshot("01-estado-error")

        composeTestRule.onNodeWithText("Reintentar").performClick()
        composeTestRule.waitForIdle()

        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        screenshotTestRule.takeScreenshot("02-después-reintento")
    }

    @Test
    fun testEmptyListHandling() {
        mockWebServerRule.server.enqueue(JsonResponseHelper.createEmptyAlbumsResponse())

        val testViewModel = createTestViewModel()

        val application = ApplicationProvider.getApplicationContext<Application>()
        val profileViewModel = ProfileViewModel(application)

        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumListScreen(
                    albumViewModel = testViewModel,
                    profileViewModel = profileViewModel,
                    onAlbumClick = {},
                    onAddAlbum = {},
                    enableLifecycleRefresh = false
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("No hay álbumes disponibles").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aún no se han agregado álbumes").assertIsDisplayed()
    }

    @Test
    fun testUnknownArtistDisplay() {
        val albumWithoutPerformers = TestDataFactory.createAlbumWithoutPerformers()
        mockWebServerRule.server.enqueue(JsonResponseHelper.createAlbumsSuccessResponse(listOf(albumWithoutPerformers)))

        val testViewModel = createTestViewModel()

        val application = ApplicationProvider.getApplicationContext<Application>()
        val profileViewModel = ProfileViewModel(application)

        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumListScreen(
                    albumViewModel = testViewModel,
                    profileViewModel = profileViewModel,
                    onAlbumClick = {},
                    onAddAlbum = {},
                    enableLifecycleRefresh = false
                )
            }
        }

        composeTestRule.waitForIdle()

        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Unknown Artist Album")
        CustomMatchers.verifyUnknownArtistIsVisible(composeTestRule)

        screenshotTestRule.takeScreenshot("artista-desconocido")
    }

    @Test
    fun testMultiplePerformersDisplay() {
        val albumWithMultiplePerformers = TestDataFactory.createTestAlbum(
            id = 9,
            name = "Collaboration Album",
            performers = TestDataFactory.createTestPerformers()
        )
        mockWebServerRule.server.enqueue(JsonResponseHelper.createAlbumsSuccessResponse(listOf(albumWithMultiplePerformers)))

        val testViewModel = createTestViewModel()

        val application = ApplicationProvider.getApplicationContext<Application>()
        val profileViewModel = ProfileViewModel(application)

        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumListScreen(
                    albumViewModel = testViewModel,
                    profileViewModel = profileViewModel,
                    onAlbumClick = {},
                    onAddAlbum = {},
                    enableLifecycleRefresh = false
                )
            }
        }

        composeTestRule.waitForIdle()

        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Collaboration Album")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "The Beatles")

        screenshotTestRule.takeScreenshot("múltiples-performers")
    }
}
