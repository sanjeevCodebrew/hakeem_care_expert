package com.consultantvendor.ui.loginSignUp

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.consultantvendor.data.apis.WebService
import com.consultantvendor.data.models.responses.UserData
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

class LoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val webService: WebService = mockk()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        viewModel = LoginViewModel(webService)
    }

    // --- login() ---

    @Test
    fun `login posts success on 200 response`() {
        val userData = UserData().apply { id = "1"; name = "Ahmed Ali"; token = "tok123" }
        val mockCall = mockk<Call<ApiResponse<UserData>>>()
        every { webService.login(any()) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<UserData>>>()
                .onResponse(mockCall, Response.success(ApiResponse(data = userData)))
        }

        viewModel.login(hashMapOf("phone" to "0501234567", "country_code" to "+966"))

        assertEquals(Status.SUCCESS, viewModel.login.value?.status)
        assertEquals("1", viewModel.login.value?.data?.id)
        assertEquals("tok123", viewModel.login.value?.data?.token)
    }

    @Test
    fun `login posts error on 401 response`() {
        val errorBody = """{"message":"Unauthorized"}""".toResponseBody("application/json".toMediaType())
        val mockCall = mockk<Call<ApiResponse<UserData>>>()
        every { webService.login(any()) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<UserData>>>()
                .onResponse(mockCall, Response.error(401, errorBody))
        }

        viewModel.login(hashMapOf("phone" to "0501234567"))

        assertEquals(Status.ERROR, viewModel.login.value?.status)
        assertNotNull(viewModel.login.value?.error)
    }

    @Test
    fun `login posts error on network failure`() {
        val mockCall = mockk<Call<ApiResponse<UserData>>>()
        every { webService.login(any()) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<UserData>>>()
                .onFailure(mockCall, IOException("No internet"))
        }

        viewModel.login(hashMapOf("phone" to "0501234567"))

        assertEquals(Status.ERROR, viewModel.login.value?.status)
        assertNotNull(viewModel.login.value?.error)
    }

    // --- drLogin() ---

    @Test
    fun `drLogin posts success on 200 response`() {
        val userData = UserData().apply { id = "5"; name = "Dr. Sara"; token = "dr-tok" }
        val mockCall = mockk<Call<ApiResponse<UserData>>>()
        every { webService.drLogin(any()) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<UserData>>>()
                .onResponse(mockCall, Response.success(ApiResponse(data = userData)))
        }

        viewModel.drLogin(hashMapOf("email" to "dr@example.com", "password" to "secret"))

        assertEquals(Status.SUCCESS, viewModel.drLogin.value?.status)
        assertEquals("5", viewModel.drLogin.value?.data?.id)
        assertEquals("dr-tok", viewModel.drLogin.value?.data?.token)
    }

    @Test
    fun `drLogin posts error on 401 response`() {
        val errorBody = """{"message":"Invalid credentials"}""".toResponseBody("application/json".toMediaType())
        val mockCall = mockk<Call<ApiResponse<UserData>>>()
        every { webService.drLogin(any()) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<UserData>>>()
                .onResponse(mockCall, Response.error(401, errorBody))
        }

        viewModel.drLogin(hashMapOf("email" to "dr@example.com", "password" to "wrong"))

        assertEquals(Status.ERROR, viewModel.drLogin.value?.status)
        assertNotNull(viewModel.drLogin.value?.error)
    }

    // --- register() ---

    @Test
    fun `register posts success on 200 response`() {
        val userData = UserData().apply { id = "2"; name = "Khalid Nasser" }
        val mockCall = mockk<Call<ApiResponse<UserData>>>()
        every { webService.register(any()) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<UserData>>>()
                .onResponse(mockCall, Response.success(ApiResponse(data = userData)))
        }

        viewModel.register(hashMapOf("phone" to "0507654321", "name" to "Khalid Nasser"))

        assertEquals(Status.SUCCESS, viewModel.register.value?.status)
        assertEquals("2", viewModel.register.value?.data?.id)
    }

    @Test
    fun `register posts error on 400 response`() {
        val errorBody = """{"message":"Phone already exists"}""".toResponseBody("application/json".toMediaType())
        val mockCall = mockk<Call<ApiResponse<UserData>>>()
        every { webService.register(any()) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<UserData>>>()
                .onResponse(mockCall, Response.error(400, errorBody))
        }

        viewModel.register(hashMapOf("phone" to "0507654321"))

        assertEquals(Status.ERROR, viewModel.register.value?.status)
    }

    // --- logout() ---

    @Test
    fun `logout posts success on 200 response`() {
        val mockCall = mockk<Call<ApiResponse<UserData>>>()
        every { webService.logout() } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<UserData>>>()
                .onResponse(mockCall, Response.success(ApiResponse(data = null)))
        }

        viewModel.logout()

        assertEquals(Status.SUCCESS, viewModel.logout.value?.status)
    }

    // --- forgotPassword() ---

    @Test
    fun `forgotPassword posts success on 200 response`() {
        val mockCall = mockk<Call<ApiResponse<UserData>>>()
        every { webService.forgotPassword(any()) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<UserData>>>()
                .onResponse(mockCall, Response.success(ApiResponse(data = null)))
        }

        viewModel.forgotPassword(hashMapOf("phone" to "0501234567"))

        assertEquals(Status.SUCCESS, viewModel.forgotPassword.value?.status)
    }

    @Test
    fun `forgotPassword posts error on network failure`() {
        val mockCall = mockk<Call<ApiResponse<UserData>>>()
        every { webService.forgotPassword(any()) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<UserData>>>()
                .onFailure(mockCall, IOException("Timeout"))
        }

        viewModel.forgotPassword(hashMapOf("phone" to "0501234567"))

        assertEquals(Status.ERROR, viewModel.forgotPassword.value?.status)
    }

    // --- sendSms() ---

    @Test
    fun `sendSms posts success on 200 response`() {
        val mockCall = mockk<Call<ApiResponse<UserData>>>()
        every { webService.sendSMS(any()) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            firstArg<Callback<ApiResponse<UserData>>>()
                .onResponse(mockCall, Response.success(ApiResponse(data = null)))
        }

        viewModel.sendSms(hashMapOf("phone" to "0501234567"))

        assertEquals(Status.SUCCESS, viewModel.sendSMS.value?.status)
    }
}
