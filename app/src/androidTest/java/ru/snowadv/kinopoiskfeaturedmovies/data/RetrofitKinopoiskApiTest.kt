package ru.snowadv.kinopoiskfeaturedmovies.data

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After

import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import ru.snowadv.kinopoiskfeaturedmovies.data.converter.JsonConverter
import ru.snowadv.kinopoiskfeaturedmovies.data.local.FilmsDao
import ru.snowadv.kinopoiskfeaturedmovies.data.local.FilmsDb
import ru.snowadv.kinopoiskfeaturedmovies.data.remote.KinopoiskApi
import ru.snowadv.kinopoiskfeaturedmovies.data.remote.dto.FilmDto
import ru.snowadv.kinopoiskfeaturedmovies.data.remote.dto.FilmInfoDto
import ru.snowadv.kinopoiskfeaturedmovies.data.remote.dto.FilmsDto
import ru.snowadv.kinopoiskfeaturedmovies.data.repository.FilmRepositoryImpl
import ru.snowadv.kinopoiskfeaturedmovies.di.AppModule
import ru.snowadv.kinopoiskfeaturedmovies.domain.model.Film
import ru.snowadv.kinopoiskfeaturedmovies.domain.repository.FilmRepository
import ru.snowadv.kinopoiskfeaturedmovies.feat.util.Resource
import javax.inject.Inject
import javax.inject.Named


@HiltAndroidTest
@UninstallModules(AppModule::class)
class RetrofitKinopoiskApiTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var mockWebServer: MockWebServer

    @Inject
    @Named("retrofit")
    lateinit var api: KinopoiskApi

    @Inject
    lateinit var filmsDto: FilmsDto
    @Inject
    lateinit var filmInfoDto: FilmInfoDto

    @Inject
    lateinit var jsonConverter: JsonConverter


    @Before
    fun setup() {
        hiltRule.inject()
    }

    @After
    fun cleanUp() {
        mockWebServer.shutdown()
    }

    @Test
    fun testGetFilms() = runBlocking{
        val response = MockResponse()
            .setResponseCode(200)
            .setBody(jsonConverter.toJson(filmsDto)!!)

        mockWebServer.enqueue(response)

        assertEquals(api.getFilms(1), filmsDto)
    }

    @Test
    fun testGetFilmInfo() = runBlocking{
        val filmId = 123L

        val response = MockResponse()
            .setResponseCode(200)
            .setBody(jsonConverter.toJson(filmInfoDto)!!)

        mockWebServer.enqueue(response)

        val fetchedFilmInfoDto = api.getFilmInfo(filmId)

        assertEquals(filmInfoDto, fetchedFilmInfoDto)
    }




    @Test
    fun testGetFilms_correctEndpoint() = runBlocking{
        val response = MockResponse()
            .setResponseCode(200)
            .setBody(jsonConverter.toJson(filmsDto)!!)

        mockWebServer.enqueue(response)

        api.getFilms()

        val request = mockWebServer.takeRequest()
        assertEquals("/api/v2.2/films/top?page=1&type=TOP_100_POPULAR_FILMS", request.path)
    }



    @Test
    fun testGetFilmInfo_correctEndpoint() = runBlocking{
        val filmId = 123L

        val response = MockResponse()
            .setResponseCode(200)
            .setBody(jsonConverter.toJson(filmInfoDto)!!)

        mockWebServer.enqueue(response)

        api.getFilmInfo(filmId)

        val request = mockWebServer.takeRequest()
        assertEquals("/api/v2.2/films/$filmId", request.path)
    }



}