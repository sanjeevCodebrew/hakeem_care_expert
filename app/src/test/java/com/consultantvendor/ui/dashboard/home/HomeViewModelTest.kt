package com.consultantvendor.ui.dashboard.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.consultantvendor.data.apis.WebService
import com.consultantvendor.data.models.responses.CommonDataModel
import com.consultantvendor.data.network.responseUtil.ApiResponse
import com.consultantvendor.data.network.responseUtil.Status
import io.mockk.every
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val webService: WebService = mockk()
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        viewModel = HomeViewModel(webService)
    }

    // --- home() ---

    @Test
    fun `home posts success on 200 response`() {
        val model = CommonDataModel()
        val mockCall = mockk<Call<ApiResponse<CommonDataModel>>>()
        every { webService.home() } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<CommonDataModel>>>()
                .onResponse(mockCall, Response.success(ApiResponse(data = model)))
        }

        viewModel.home()

        assertEquals(Status.SUCCESS, viewModel.home.value?.status)
    }

    @Test
    fun `home posts error on 500 response`() {
        val errorBody = """{"message":"Server error"}""".toResponseBody("application/json".toMediaType())
        val mockCall = mockk<Call<ApiResponse<CommonDataModel>>>()
        every { webService.home() } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<CommonDataModel>>>()
                .onResponse(mockCall, Response.error(500, errorBody))
        }

        viewModel.home()

        assertEquals(Status.ERROR, viewModel.home.value?.status)
        assertNotNull(viewModel.home.value?.error)
    }

    @Test
    fun `home posts error on network failure`() {
        val mockCall = mockk<Call<ApiResponse<CommonDataModel>>>()
        every { webService.home() } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<CommonDataModel>>>()
                .onFailure(mockCall, IOException("No internet"))
        }

        viewModel.home()

        assertEquals(Status.ERROR, viewModel.home.value?.status)
        assertNotNull(viewModel.home.value?.error)
    }

    // --- banners() ---

    @Test
    fun `banners posts success on 200 response`() {
        val model = CommonDataModel()
        val mockCall = mockk<Call<ApiResponse<CommonDataModel>>>()
        every { webService.banners() } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<CommonDataModel>>>()
                .onResponse(mockCall, Response.success(ApiResponse(data = model)))
        }

        viewModel.banners()

        assertEquals(Status.SUCCESS, viewModel.banners.value?.status)
    }

    @Test
    fun `banners posts error on network failure`() {
        val mockCall = mockk<Call<ApiResponse<CommonDataModel>>>()
        every { webService.banners() } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<CommonDataModel>>>()
                .onFailure(mockCall, IOException("Timeout"))
        }

        viewModel.banners()

        assertEquals(Status.ERROR, viewModel.banners.value?.status)
    }

    // --- notificationCount() ---

    @Test
    fun `notificationCount posts success on 200 response`() {
        val model = CommonDataModel()
        val mockCall = mockk<Call<ApiResponse<CommonDataModel>>>()
        every { webService.notificationCount() } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<CommonDataModel>>>()
                .onResponse(mockCall, Response.success(ApiResponse(data = model)))
        }

        viewModel.notificationCount()

        assertEquals(Status.SUCCESS, viewModel.notificationCount.value?.status)
    }

    @Test
    fun `notificationCount posts error on 401 response`() {
        val errorBody = """{"message":"Unauthorized"}""".toResponseBody("application/json".toMediaType())
        val mockCall = mockk<Call<ApiResponse<CommonDataModel>>>()
        every { webService.notificationCount() } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<CommonDataModel>>>()
                .onResponse(mockCall, Response.error(401, errorBody))
        }

        viewModel.notificationCount()

        assertEquals(Status.ERROR, viewModel.notificationCount.value?.status)
    }

    // --- getprofile1() ---

    @Test
    fun `getprofile1 posts success on 200 response`() {
        val model = CommonDataModel()
        val mockCall = mockk<Call<ApiResponse<CommonDataModel>>>()
        every { webService.getProfile() } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<CommonDataModel>>>()
                .onResponse(mockCall, Response.success(ApiResponse(data = model)))
        }

        viewModel.getprofile1()

        assertEquals(Status.SUCCESS, viewModel.getProfile.value?.status)
    }

    @Test
    fun `getprofile1 posts error on network failure`() {
        val mockCall = mockk<Call<ApiResponse<CommonDataModel>>>()
        every { webService.getProfile() } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<CommonDataModel>>>()
                .onFailure(mockCall, IOException("Connection reset"))
        }

        viewModel.getprofile1()

        assertEquals(Status.ERROR, viewModel.getProfile.value?.status)
    }

    // --- postLanguage1() ---

    @Test
    fun `postLanguage1 posts success on 200 response`() {
        val model = CommonDataModel()
        val mockCall = mockk<Call<ApiResponse<CommonDataModel>>>()
        every { webService.postLanguage(any()) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<CommonDataModel>>>()
                .onResponse(mockCall, Response.success(ApiResponse(data = model)))
        }

        viewModel.postLanguage1(hashMapOf("language" to "ar"))

        assertEquals(Status.SUCCESS, viewModel.postLanguage.value?.status)
    }
}
