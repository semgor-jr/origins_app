package com.origin.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import io.ktor.client.request.forms.*
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RegisterRequest(val name: String, val email: String, val password: String)
@Serializable
data class VerifyEmailRequest(val email: String, val code: String)
@Serializable
data class LoginRequest(val email: String, val password: String)
@Serializable
data class UpdateMeRequest(val name: String)
@Serializable
data class UserDto(
    val id: String, 
    val name: String,
    val email: String,
    val isEmailVerified: Boolean,
    val autosomalData: List<OriginPortion> = emptyList(),
    val yHaplogroup: String? = null,
    val mtHaplogroup: String? = null
)
@Serializable
data class LoginResponse(val token: String, val user: UserDto)
@Serializable
data class OriginPortion(val region: String, val percent: Int)
@Serializable
data class ReportDto(val id: String, val userId: String, val name: String?, val summary: String, val origins: List<OriginPortion>, val publicId: String? = null)

@Serializable
data class NewsDto(val id: String, val title: String, val content: String, val imageUrl: String?, val source: String?, val publishedAt: String)

@Serializable
data class CreateReportRequest(val name: String, val description: String, val decodingMethod: String)

@Serializable
data class DecodingMethod(val id: String, val name: String, val description: String)


@Serializable
data class MessageResponse(val message: String)

@Serializable
data class VerifyEmailResponse(val user: UserDto, val token: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(val email: String, val code: String, val newPassword: String)

private val json = Json { ignoreUnknownKeys = true }

class ApiClient(private val baseUrl: String = AppConfig.apiBaseUrl, private val tokenProvider: () -> String? = { null }) {
	private val client = HttpClient(OkHttp) { 
		install(ContentNegotiation) { json() }
		install(HttpTimeout) {
			requestTimeoutMillis = AppConfig.requestTimeout
			connectTimeoutMillis = AppConfig.connectTimeout
			socketTimeoutMillis = AppConfig.requestTimeout
		}
		engine {
			preconfigured = OkHttpClient.Builder()
				.connectTimeout(AppConfig.connectTimeout / 1000, TimeUnit.SECONDS)
				.readTimeout(AppConfig.requestTimeout / 1000, TimeUnit.SECONDS)
				.writeTimeout(AppConfig.requestTimeout / 1000, TimeUnit.SECONDS)
				.build()
		}
	}

	suspend fun register(req: RegisterRequest): Map<String, String> {
		val resp = client.post("$baseUrl/auth/register") { contentType(ContentType.Application.Json); setBody(req) }
		val body = resp.bodyAsText(); if (resp.status.value !in 200..299) throw ResponseException(resp, body)
		return json.decodeFromString(body)
	}
	
	suspend fun verifyEmail(req: VerifyEmailRequest): VerifyEmailResponse {
		val resp = client.post("$baseUrl/auth/verify-email") { contentType(ContentType.Application.Json); setBody(req) }
		val body = resp.bodyAsText(); if (resp.status.value !in 200..299) throw ResponseException(resp, body)
		return json.decodeFromString(body)
	}

	suspend fun login(req: LoginRequest): LoginResponse {
		val resp = client.post("$baseUrl/auth/login") { contentType(ContentType.Application.Json); setBody(req) }
		val body = resp.bodyAsText(); if (resp.status.value !in 200..299) throw ResponseException(resp, body)
		return json.decodeFromString(body)
	}

	suspend fun getMe(): UserDto {
		val resp = client.get("$baseUrl/me") { tokenProvider()?.let { headers.append(HttpHeaders.Authorization, "Bearer $it") } }
		val body = resp.bodyAsText(); if (resp.status.value !in 200..299) throw ResponseException(resp, body)
		return json.decodeFromString(body)
	}

	suspend fun updateMe(name: String): UserDto {
		val resp = client.put("$baseUrl/me") {
			tokenProvider()?.let { headers.append(HttpHeaders.Authorization, "Bearer $it") }
			contentType(ContentType.Application.Json)
			setBody(UpdateMeRequest(name))
		}
		val body = resp.bodyAsText(); if (resp.status.value !in 200..299) throw ResponseException(resp, body)
		return json.decodeFromString(body)
	}

	suspend fun getReports(): List<ReportDto> {
		val resp = client.get("$baseUrl/reports") { tokenProvider()?.let { headers.append(HttpHeaders.Authorization, "Bearer $it") } }
		val body = resp.bodyAsText(); if (resp.status.value !in 200..299) throw ResponseException(resp, body)
		return json.decodeFromString(body)
	}

    suspend fun getReport(id: String): ReportDto {
		val resp = client.get("$baseUrl/reports/$id") { tokenProvider()?.let { headers.append(HttpHeaders.Authorization, "Bearer $it") } }
		val body = resp.bodyAsText(); if (resp.status.value !in 200..299) throw ResponseException(resp, body)
		return json.decodeFromString(body)
	}

	suspend fun getNews(): List<NewsDto> {
		val resp = client.get("$baseUrl/news")
		val body = resp.bodyAsText(); if (resp.status.value !in 200..299) throw ResponseException(resp, body)
		return json.decodeFromString(body)
	}

	suspend fun getNewsById(id: String): NewsDto {
		val resp = client.get("$baseUrl/news/$id")
		val body = resp.bodyAsText(); if (resp.status.value !in 200..299) throw ResponseException(resp, body)
		return json.decodeFromString(body)
	}

	suspend fun getDecodingMethods(): List<DecodingMethod> {
		val resp = client.get("$baseUrl/decoding-methods")
		val body = resp.bodyAsText(); if (resp.status.value !in 200..299) throw ResponseException(resp, body)
		return json.decodeFromString(body)
	}

    suspend fun createReport(request: CreateReportRequest): ReportDto {
		val resp = client.post("$baseUrl/reports") {
			header("Authorization", "Bearer ${tokenProvider()}")
			contentType(ContentType.Application.Json)
			setBody(request)
		}
		val body = resp.bodyAsText(); if (resp.status.value !in 200..299) throw ResponseException(resp, body)
		return json.decodeFromString(body)
	}

    suspend fun uploadVcfToReport(reportId: String, fileBytes: ByteArray, fileName: String): ReportDto {
		val resp = client.post("$baseUrl/reports/$reportId/upload") {
			header("Authorization", "Bearer ${tokenProvider()}")
			setBody(MultiPartFormDataContent(
				formData {
					append("file", fileBytes, Headers.build {
						append(HttpHeaders.ContentType, "application/octet-stream")
						append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
					})
				}
			))
		}
		val body = resp.bodyAsText(); if (resp.status.value !in 200..299) throw ResponseException(resp, body)
		return json.decodeFromString(body)
	}
	
	suspend fun requestPasswordReset(email: String): MessageResponse {
		val resp = client.post("$baseUrl/auth/forgot-password") { 
			contentType(ContentType.Application.Json)
			setBody(ForgotPasswordRequest(email)) 
		}
		val body = resp.bodyAsText(); if (resp.status.value !in 200..299) throw ResponseException(resp, body)
		return json.decodeFromString(body)
	}
	
	suspend fun resetPassword(email: String, code: String, newPassword: String): MessageResponse {
		val resp = client.post("$baseUrl/auth/reset-password") { 
			contentType(ContentType.Application.Json)
			setBody(ResetPasswordRequest(email, code, newPassword)) 
		}
		val body = resp.bodyAsText(); if (resp.status.value !in 200..299) throw ResponseException(resp, body)
		return json.decodeFromString(body)
	}
}
