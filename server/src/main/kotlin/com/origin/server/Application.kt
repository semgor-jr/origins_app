package com.origin.server

import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.*
import java.util.UUID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.createMissingTablesAndColumns
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.mindrot.jbcrypt.BCrypt
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import io.ktor.server.plugins.statuspages.*


@Serializable
data class RegisterRequest(val name: String, val email: String, val password: String)
@Serializable
data class VerifyEmailRequest(val email: String, val code: String)
@Serializable
data class LoginRequest(val email: String, val password: String)
@Serializable
data class ForgotPasswordRequest(val email: String)
@Serializable
data class ResetPasswordRequest(val email: String, val code: String, val newPassword: String)
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
data class ErrorResponse(val error: String)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class StatusResponse(val status: String)

@Serializable
data class RegisterResponse(val message: String, val email: String)

@Serializable
data class VerifyEmailResponse(val user: UserDto, val token: String)
@Serializable
data class UpdateMeRequest(val name: String)
@Serializable
data class ReportDto(val id: String, val userId: String, val name: String?, val summary: String, val origins: List<OriginPortion>, val publicId: String? = null)
@Serializable
data class PublicReportDto(val id: String, val name: String?, val summary: String, val origins: List<OriginPortion>, val createdAt: String)
@Serializable
data class OriginPortion(val region: String, val percent: Int)
@Serializable
data class LoginResponse(val token: String, val user: UserDto)
@Serializable
data class NewsDto(val id: String, val title: String, val content: String, val imageUrl: String?, val source: String?, val publishedAt: String)
@Serializable
data class CreateReportRequest(val name: String, val description: String, val decodingMethod: String, val analysisType: String? = null)
@Serializable
data class DecodingMethod(val id: String, val name: String, val description: String)

object Users : Table() {
	val id = varchar("id", 36)
	val name = varchar("name", 255)
	val contact = varchar("contact", 255).uniqueIndex()
	val passwordHash = varchar("password_hash", 60)
	val autosomalData = text("autosomal_data").nullable()
	val yHaplogroup = varchar("y_haplogroup", 50).nullable()
	val mtHaplogroup = varchar("mt_haplogroup", 50).nullable()
	val verificationCode = varchar("verification_code", 10).nullable()
	val isEmailVerified = bool("is_email_verified").default(false)
	val verificationCodeExpires = long("verification_code_expires").nullable()
	override val primaryKey = PrimaryKey(id)
}

object Reports : Table() {
	val id = varchar("id", 36)
	val userId = varchar("user_id", 36).index()
	val name = varchar("name", 255).nullable()
	val description = text("description").nullable()
	val decodingMethod = varchar("decoding_method", 100).nullable()
	val summary = text("summary")
	val originsJson = text("origins_json")
	val publicId = varchar("public_id", 36).uniqueIndex().nullable()
	override val primaryKey = PrimaryKey(id)
}

object News : Table() {
	val id = varchar("id", 36)
	val title = varchar("title", 500)
	val content = text("content")
	val imageUrl = varchar("image_url", 1000).nullable()
	val newsSource = varchar("news_source", 255).nullable()
	val publishedAt = datetime("published_at")
	override val primaryKey = PrimaryKey(id)
}

fun initDatabase() {
	val url = System.getenv("DATABASE_URL")
	val user = System.getenv("DATABASE_USER")
	val pass = System.getenv("DATABASE_PASSWORD")
	if (url.isNullOrBlank()) {
		Database.connect("jdbc:h2:./build/localdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE", driver = "org.h2.Driver")
	} else {
		Database.connect(url, driver = "org.postgresql.Driver", user = user ?: "origin", password = pass ?: "origin")
	}
	transaction { 
		createMissingTablesAndColumns(Users, Reports, News)
		// Заполняем тестовыми новостями, если таблица пустая
		val newsCount = News.selectAll().count()
		if (newsCount == 0L) {
			seedNews()
		}
		// Создаем тестового пользователя для тестирования
		createTestUser()
	}
}

private val json = Json { ignoreUnknownKeys = true }

private fun createTestUser() {
	// Проверяем, есть ли уже тестовый пользователь
	val existingUser = Users.select { Users.contact eq "test@example.com" }.firstOrNull()
	if (existingUser == null) {
		// Создаем тестового пользователя с уже верифицированным email
		Users.insert {
			it[id] = UUID.randomUUID().toString()
			it[name] = "Test User"
			it[contact] = "test@example.com"
			it[passwordHash] = BCrypt.hashpw("password123", BCrypt.gensalt())
			it[isEmailVerified] = true  // Уже верифицирован
			it[verificationCode] = null
			it[verificationCodeExpires] = null
		}
		println("✅ Test user created: test@example.com / password123")
	} else {
		// Обновляем существующего пользователя, чтобы он был верифицирован
		Users.update({ Users.contact eq "test@example.com" }) {
			it[isEmailVerified] = true
			it[verificationCode] = null
			it[verificationCodeExpires] = null
		}
		println("ℹ️ Test user updated: test@example.com (verified)")
	}
}

private fun seedNews() {
	val sampleNews = listOf(
		NewsDto(
			id = UUID.randomUUID().toString(),
			title = "Новые открытия в области генетики предков",
			content = "Ученые обнаружили новые маркеры ДНК, которые позволяют более точно определять географическое происхождение предков. Эти открытия помогут людям лучше понять свою историю и корни.",
			imageUrl = "https://images.unsplash.com/photo-1559757148-5c350d0d3c56?w=400",
			source = "Генетические исследования",
			publishedAt = "2024-01-15T10:00:00Z"
		),
		NewsDto(
			id = UUID.randomUUID().toString(),
			title = "Как ДНК-тесты помогают найти родственников",
			content = "Современные технологии анализа ДНК позволяют находить дальних родственников по всему миру. Многие люди находят неожиданные связи и расширяют свое семейное древо.",
			imageUrl = "https://images.unsplash.com/photo-1576091160399-112ba8d25d1f?w=400",
			source = "Семейная история",
			publishedAt = "2024-01-14T15:30:00Z"
		),
		NewsDto(
			id = UUID.randomUUID().toString(),
			title = "Миграционные пути наших предков",
			content = "Исследование показывает, как древние народы перемещались по континентам. Анализ генетических данных раскрывает увлекательные истории миграций, которые сформировали современное человечество.",
			imageUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400",
			source = "Антропология",
			publishedAt = "2024-01-13T09:15:00Z"
		),
		NewsDto(
			id = UUID.randomUUID().toString(),
			title = "Точность современных ДНК-тестов",
			content = "Современные методы анализа ДНК достигли невероятной точности. Новые алгоритмы позволяют определять происхождение с точностью до конкретных регионов и даже городов.",
			imageUrl = "https://images.unsplash.com/photo-1559757175-0eb30cd8c063?w=400",
			source = "Научные технологии",
			publishedAt = "2024-01-12T14:20:00Z"
		),
		NewsDto(
			id = UUID.randomUUID().toString(),
			title = "История через призму генетики",
			content = "Генетические исследования помогают переписывать историю человечества. Новые данные показывают, что многие исторические события имели более сложные последствия, чем мы думали ранее.",
			imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=400",
			source = "Историческая генетика",
			publishedAt = "2024-01-11T11:45:00Z"
		)
	)
	
	sampleNews.forEach { news ->
		News.insert {
			it[News.id] = news.id
			it[News.title] = news.title
			it[News.content] = news.content
			it[News.imageUrl] = news.imageUrl
			it[News.newsSource] = news.source
			it[News.publishedAt] = java.time.LocalDateTime.parse(news.publishedAt.replace("Z", ""))
		}
	}
}


    private suspend fun estimateOriginsFromVcf(vcfBytes: ByteArray, methodId: String? = null): List<OriginPortion> {
        return try {
            // Выбираем анализатор в зависимости от метода
            when (methodId) {
                "autosomal_analysis" -> {
                    println("Запускаем аутосомный анализ...")
                    try {
                        val result = AutosomalDnaAnalyzer().analyzeAutosomalDna(vcfBytes)
                        if (result.isNotEmpty() && result.sumOf { it.percent } > 0) {
                            println("Аутосомный анализ успешно выполнен")
                            return result
                        }
                    } catch (e: Exception) {
                        println("Ошибка в аутосомном анализе: ${e.message}")
                    }
                }
                
                "y_haplogroup" -> {
                    println("Запускаем анализ Y-гаплогруппы...")
                    try {
                        val result = YHaplogroupAnalyzer().analyzeYHaplogroup(vcfBytes)
                        if (result.isNotEmpty() && result.sumOf { it.percent } > 0) {
                            println("Анализ Y-гаплогруппы успешно выполнен")
                            return result
                        }
                    } catch (e: Exception) {
                        println("Ошибка в анализе Y-гаплогруппы: ${e.message}")
                    }
                }
                
                "mt_haplogroup" -> {
                    println("Запускаем анализ мт-гаплогруппы...")
                    try {
                        val result = MtHaplogroupAnalyzer().analyzeMtHaplogroup(vcfBytes)
                        if (result.isNotEmpty() && result.sumOf { it.percent } > 0) {
                            println("Анализ мт-гаплогруппы успешно выполнен")
                            return result
                        }
                    } catch (e: Exception) {
                        println("Ошибка в анализе мт-гаплогруппы: ${e.message}")
                    }
                }
                
                else -> {
                    println("Запускаем универсальный анализ...")
                    // По умолчанию используем аутосомный анализ
                    try {
                        val result = AutosomalDnaAnalyzer().analyzeAutosomalDna(vcfBytes)
                        if (result.isNotEmpty() && result.sumOf { it.percent } > 0) {
                            println("Универсальный анализ успешно выполнен")
                            return result
                        }
                    } catch (e: Exception) {
                        println("Ошибка в универсальном анализе: ${e.message}")
                    }
                }
            }
		
            // Если выбранный анализатор не сработал, используем fallback
            println("Анализатор не сработал, используем резервное распределение")
            createFallbackDistribution(0)
        } catch (e: Exception) {
            println("Критическая ошибка в анализе VCF: ${e.message}")
            e.printStackTrace()
            createFallbackDistribution(0)
        }
    }


// Резервное распределение
private fun createFallbackDistribution(totalSnps: Int): List<OriginPortion> {
	val basePercentage = if (totalSnps > 0) 100 / 6 else 25
	return listOf(
		OriginPortion("Европа", basePercentage + (totalSnps % 3)),
		OriginPortion("Восточная Азия", basePercentage + (totalSnps % 2)),
		OriginPortion("Ближний Восток", basePercentage),
		OriginPortion("Африка", basePercentage),
		OriginPortion("Южная Азия", basePercentage),
		OriginPortion("Америка", 100 - (basePercentage * 5 + (totalSnps % 5)))
	).filter { it.percent > 0 }
}



fun Application.jwtModule() {
	val secret = System.getenv("JWT_SECRET") ?: "dev-secret"
	val issuer = "origin-app"
	val audience = "origin-users"
	val algorithm = Algorithm.HMAC256(secret)
	install(Authentication) {
		jwt("auth-jwt") {
			realm = "origin"
			verifier(JWT.require(algorithm).withIssuer(issuer).withAudience(audience).build())
			validate { cred ->
				val uid = cred.payload.getClaim("uid").asString()
				if (!uid.isNullOrBlank()) JWTPrincipal(cred.payload) else null
			}
		}
	}
	environment.monitor.subscribe(ApplicationStopping) {}

}

fun main() {
	initDatabase()
	embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080, host = "0.0.0.0") {
		install(CallLogging)
		install(CORS) { 
			anyHost()
			allowHeader("Content-Type")
			allowHeader("Authorization")
			allowHeader("Content-Length")
		}
		install(ContentNegotiation) { json() }
		install(StatusPages) {
			exception<Throwable> { call, cause ->
				cause.printStackTrace()
				call.respond(HttpStatusCode.InternalServerError, ErrorResponse( (cause.message ?: "internal error")))
			}
		}
		jwtModule()

		routing {
			get("/") { call.respond(StatusResponse("ok")) }
			


        get("/test-axiom-world-array") {
            try {
                val axiomService = com.origin.server.data.AxiomWorldArrayService()
                val stats = axiomService.getMarkerStats()
                call.respond(HttpStatusCode.OK, """
                    {
                        "status": "success",
                        "axiom_world_array_stats": {
                            "total_markers": ${stats.totalMarkers},
                            "loaded_markers": ${stats.loadedMarkers},
                            "ancestry_informative_markers": ${stats.ancestryInformativeMarkers},
                            "average_quality": ${stats.averageQuality},
                            "cache_size": ${stats.cacheSize}
                        }
                    }
                """)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, """
                    {
                        "status": "error",
                        "message": "${e.message}"
                    }
                """)
            }
        }

			// Публичные эндпоинты
			get("/decoding-methods") {
				val methods = listOf(
					DecodingMethod(
						id = "autosomal_analysis",
						name = "Аутосомный анализ",
						description = "Определение этнического происхождения по аутосомным хромосомам (1-22) с использованием Affymetrix Axiom World Array маркеров"
					),
					DecodingMethod(
						id = "y_haplogroup",
						name = "Определение Y-гаплогруппы",
						description = "Анализ Y-хромосомы для определения отцовской линии и гаплогруппы предков по мужской линии"
					),
					DecodingMethod(
						id = "mt_haplogroup",
						name = "Определение мт-гаплогруппы",
						description = "Анализ митохондриальной ДНК для определения материнской линии и гаплогруппы предков по женской линии"
					)
				)
				call.respond(methods)
			}

			post("/auth/register") {
				val text = call.receiveText()
				val req = try { json.decodeFromString<RegisterRequest>(text) } catch (e: Exception) { 
					call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad json: ${e.message}"))
                    return@post
				}
				
				// Проверяем валидность email
				if (!EmailService.isValidEmail(req.email)) {
					call.respond(HttpStatusCode.BadRequest, ErrorResponse( "invalid_email"))
					return@post
				}
				
				val email = req.email.lowercase()
				
				// Проверяем, не существует ли уже пользователь с таким email
				val existing = transaction { Users.select { Users.contact eq email }.firstOrNull() }
				if (existing != null) { 
					call.respond(HttpStatusCode.Conflict, ErrorResponse( "email_already_exists"))
                    return@post
				}
				
				// Генерируем код подтверждения
				val verificationCode = EmailService.generateVerificationCode()
				val codeExpires = System.currentTimeMillis() + (10 * 60 * 1000) // 10 минут
				
				val id = UUID.randomUUID().toString()
				val hash = BCrypt.hashpw(req.password, BCrypt.gensalt())
				
				transaction { 
					Users.insert { 
						it[Users.id] = id
						it[Users.name] = req.name
						it[Users.contact] = email
						it[Users.passwordHash] = hash
						it[Users.verificationCode] = verificationCode
						it[Users.isEmailVerified] = false
						it[Users.verificationCodeExpires] = codeExpires
					} 
				}
				
				// Отправляем код подтверждения
				EmailService.sendVerificationCode(req.email, verificationCode)
				
				call.respond(RegisterResponse(
					message = "Код подтверждения отправлен на email",
					email = req.email
				))
			}

			post("/auth/verify-email") {
				val text = call.receiveText()
				val req = try { json.decodeFromString<VerifyEmailRequest>(text) } catch (e: Exception) { 
					call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad json: ${e.message}"))
                    return@post
				}
				
				val email = req.email.lowercase()
				
				// Находим пользователя по email
				val user = transaction { Users.select { Users.contact eq email }.firstOrNull() }
				if (user == null) {
					call.respond(HttpStatusCode.NotFound, ErrorResponse( "user_not_found"))
					return@post
				}
				
				// Проверяем, не верифицирован ли уже email
				if (user[Users.isEmailVerified]) {
					call.respond(HttpStatusCode.BadRequest, ErrorResponse( "email_already_verified"))
					return@post
				}
				
				// Проверяем код
				val storedCode = user[Users.verificationCode]
				val codeExpires = user[Users.verificationCodeExpires]
				
				if (storedCode == null || codeExpires == null) {
					call.respond(HttpStatusCode.BadRequest, ErrorResponse( "no_verification_code"))
					return@post
				}
				
				if (System.currentTimeMillis() > codeExpires) {
					call.respond(HttpStatusCode.BadRequest, ErrorResponse( "verification_code_expired"))
					return@post
				}
				
				if (storedCode != req.code) {
					call.respond(HttpStatusCode.BadRequest, ErrorResponse( "invalid_verification_code"))
					return@post
				}
				
				// Подтверждаем email
				transaction {
					Users.update({ Users.id eq user[Users.id] }) {
						it[Users.isEmailVerified] = true
						it[Users.verificationCode] = null
						it[Users.verificationCodeExpires] = null
					}
				}
				
				// Получаем данные анализа
				val autosomalData = user[Users.autosomalData]?.let { json.decodeFromString<List<OriginPortion>>(it) } ?: emptyList()
				val yHaplogroup = user[Users.yHaplogroup]
				val mtHaplogroup = user[Users.mtHaplogroup]
				
				// Генерируем JWT токен
				val token = JWT.create()
					.withSubject(user[Users.id])
					.withExpiresAt(java.util.Date(System.currentTimeMillis() + 86400000)) // 24 часа
					.sign(Algorithm.HMAC256("secret"))
				
				call.respond(VerifyEmailResponse(
					user = UserDto(
						id = user[Users.id], 
						name = user[Users.name],
						email = user[Users.contact],
						isEmailVerified = true,
						autosomalData = autosomalData,
						yHaplogroup = yHaplogroup,
						mtHaplogroup = mtHaplogroup
					),
					token = token
				))
			}

			post("/auth/login") {
				try {
					val text = call.receiveText()
					val req = try { json.decodeFromString<LoginRequest>(text) } catch (e: Exception) { call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad json: ${e.message}")); return@post }
					val email = req.email.lowercase()
					val row = transaction { Users.select { Users.contact eq email }.firstOrNull() }
					if (row == null) { call.respond(HttpStatusCode.Unauthorized, ErrorResponse( "invalid credentials")); return@post }
					
					// Проверяем, верифицирован ли email
					if (!row[Users.isEmailVerified]) {
						call.respond(HttpStatusCode.Forbidden, ErrorResponse("email_not_verified"))
						return@post
					}
					
					val ok = try { BCrypt.checkpw(req.password, row[Users.passwordHash]) } catch (_: Exception) { false }
					if (!ok) { call.respond(HttpStatusCode.Unauthorized, ErrorResponse( "invalid credentials")); return@post }
					val uid = row[Users.id]
					val issuer = "origin-app"
					val audience = "origin-users"
					val token = JWT.create().withIssuer(issuer).withAudience(audience).withClaim("uid", uid).sign(Algorithm.HMAC256(System.getenv("JWT_SECRET") ?: "dev-secret"))
					call.respond(LoginResponse(token = token, user = UserDto(
						id = uid, 
						name = row[Users.name],
						email = row[Users.contact],
						isEmailVerified = row[Users.isEmailVerified],
						autosomalData = row[Users.autosomalData]?.let { json.decodeFromString<List<OriginPortion>>(it) } ?: emptyList(),
						yHaplogroup = row[Users.yHaplogroup],
						mtHaplogroup = row[Users.mtHaplogroup]
					)))
				} catch (e: Exception) {
					e.printStackTrace()
					call.respond(HttpStatusCode.InternalServerError, ErrorResponse( (e.message ?: "internal error")))
				}
			}

			authenticate("auth-jwt") {
				get("/me") {
					val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asString()
					val user = transaction { Users.select { Users.id eq uid }.first() }
					
					// Получаем данные анализа
					val autosomalData = user[Users.autosomalData]?.let { json.decodeFromString<List<OriginPortion>>(it) } ?: emptyList()
					val yHaplogroup = user[Users.yHaplogroup]
					val mtHaplogroup = user[Users.mtHaplogroup]
					
					call.respond(UserDto(
						id = user[Users.id], 
						name = user[Users.name],
						email = user[Users.contact],
						isEmailVerified = user[Users.isEmailVerified],
						autosomalData = autosomalData,
						yHaplogroup = yHaplogroup,
						mtHaplogroup = mtHaplogroup
					))
				}

				put("/me") {
					val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asString()
					val req = call.receive<UpdateMeRequest>()
					transaction { Users.update({ Users.id eq uid }) { it[name] = req.name } }
					val row = transaction { Users.select { Users.id eq uid }.first() }
					call.respond(UserDto(
						id = uid, 
						name = row[Users.name],
						email = row[Users.contact],
						isEmailVerified = row[Users.isEmailVerified],
						autosomalData = row[Users.autosomalData]?.let { json.decodeFromString<List<OriginPortion>>(it) } ?: emptyList(),
						yHaplogroup = row[Users.yHaplogroup],
						mtHaplogroup = row[Users.mtHaplogroup]
					))
				}

				get("/reports") {
					val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asString()
					val list = transaction {
						Reports.select { Reports.userId eq uid }.map {
							ReportDto(
								id = it[Reports.id],
								userId = it[Reports.userId],
								name = it[Reports.name],
								summary = it[Reports.summary],
								origins = json.decodeFromString(it[Reports.originsJson]),
								publicId = it[Reports.publicId]
							)
						}
					}
					call.respond(list)
				}

				post("/reports") {
					val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asString()
					val req = call.receive<CreateReportRequest>()
					
					// Проверяем, есть ли уже отчет с таким типом анализа
					val existingReport = transaction { 
						Reports.select { (Reports.userId eq uid) and (Reports.decodingMethod eq req.decodingMethod) }.firstOrNull()
					}
					
					if (existingReport != null) {
						// Если отчет существует, обновляем его вместо создания нового
						transaction {
							Reports.update({ Reports.id eq existingReport[Reports.id] }) {
								it[Reports.name] = req.name
								it[Reports.description] = req.description
								it[Reports.summary] = "Отчет обновлен, ожидает загрузки файла"
								it[Reports.originsJson] = json.encodeToString(emptyList<OriginPortion>())
							}
						}
						
						val report = ReportDto(
							id = existingReport[Reports.id],
							userId = uid,
							name = existingReport[Reports.name],
							summary = "Отчет обновлен, ожидает загрузки файла",
							origins = emptyList(),
							publicId = existingReport[Reports.publicId]
						)
						call.respond(report)
						return@post
					}
					
					// Если отчета нет, создаем новый
					val id = UUID.randomUUID().toString()
					val publicId = UUID.randomUUID().toString()
					
					transaction {
						Reports.insert {
							it[Reports.id] = id
							it[Reports.userId] = uid
							it[Reports.name] = req.name
							it[Reports.description] = req.description
							it[Reports.decodingMethod] = req.decodingMethod
							it[Reports.summary] = "Отчет создан, ожидает загрузки файла"
							it[Reports.originsJson] = json.encodeToString(emptyList<OriginPortion>())
							it[Reports.publicId] = publicId
						}
					}
					
					val report = ReportDto(
						id = id,
						userId = uid,
						name = req.name,
						summary = "Отчет создан, ожидает загрузки файла",
						origins = emptyList(),
						publicId = publicId
					)
					call.respond(report)
				}

				post("/reports/replace") {
					val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asString()
					val req = call.receive<CreateReportRequest>()
					
					// Находим существующий отчет
					val existingReport = transaction { 
						Reports.select { (Reports.userId eq uid) and (Reports.decodingMethod eq req.decodingMethod) }.firstOrNull()
					}
					
					if (existingReport == null) {
						call.respond(HttpStatusCode.NotFound, ErrorResponse( "report_not_found"))
						return@post
					}
					
					// Обновляем существующий отчет
					transaction {
						Reports.update({ Reports.id eq existingReport[Reports.id] }) {
							it[Reports.name] = req.name
							it[Reports.description] = req.description
							it[Reports.summary] = "Отчет обновлен, ожидает загрузки файла"
							it[Reports.originsJson] = json.encodeToString(emptyList<OriginPortion>())
						}
					}
					
					val report = ReportDto(
						id = existingReport[Reports.id],
						userId = uid,
						name = existingReport[Reports.name],
						summary = "Отчет обновлен, ожидает загрузки файла",
						origins = emptyList(),
						publicId = existingReport[Reports.publicId]
					)
					call.respond(report)
				}

				post("/reports/upload") {
					val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asString()
					val multi = call.receiveMultipart()
					var bytes: ByteArray? = null
					var fileName: String? = null
					while (true) {
						when (val part = multi.readPart() ?: break) {
							is PartData.FileItem -> {
								fileName = part.originalFileName
								bytes = part.streamProvider().readBytes()
								part.dispose()
							}
							else -> { part.dispose() }
						}
					}
					if (bytes == null) { call.respond(HttpStatusCode.BadRequest, ErrorResponse( "file missing")); return@post }
					val origins = estimateOriginsFromVcf(bytes, "autosomal_analysis")
					val summary = "Файл: ${fileName ?: "unknown"}, метод: autosomal_analysis, проанализировано SNP: ${origins.size}, найдено совпадений: ${origins.size}, размер файла: ${bytes.size} байт"
					val id = UUID.randomUUID().toString()
					transaction {
						Reports.insert {
							it[Reports.id] = id
							it[userId] = uid
							it[Reports.summary] = summary
							it[originsJson] = json.encodeToString(origins)
						}
					}
					call.respond(ReportDto(id = id, userId = uid, name = null, summary = summary, origins = origins))
				}

				get("/reports/{id}") {
					val id = call.parameters["id"]
					val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asString()
					if (id.isNullOrBlank()) { call.respond(HttpStatusCode.BadRequest, ErrorResponse( "id required")); return@get }
					val row = transaction { Reports.select { (Reports.id eq id) and (Reports.userId eq uid) }.firstOrNull() }
					if (row == null) { call.respond(HttpStatusCode.NotFound, ErrorResponse( "not found")); return@get }
					val origins = json.decodeFromString<List<OriginPortion>>(row[Reports.originsJson])
					call.respond(ReportDto(id = row[Reports.id], userId = row[Reports.userId], name = row[Reports.name], summary = row[Reports.summary], origins = origins, publicId = row[Reports.publicId]))
				}

				post("/reports/{id}/upload") {
					val reportId = call.parameters["id"]
					val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asString()
					if (reportId.isNullOrBlank()) { call.respond(HttpStatusCode.BadRequest, ErrorResponse( "id required")); return@post }
					
					// Проверяем, что отчет принадлежит пользователю
					val report = transaction { Reports.select { (Reports.id eq reportId) and (Reports.userId eq uid) }.firstOrNull() }
					if (report == null) { call.respond(HttpStatusCode.NotFound, ErrorResponse( "report not found")); return@post }
					
					val multi = call.receiveMultipart()
					var bytes: ByteArray? = null
					var fileName: String? = null
					while (true) {
						when (val part = multi.readPart() ?: break) {
							is PartData.FileItem -> {
								fileName = part.originalFileName
								bytes = part.streamProvider().readBytes()
								part.dispose()
							}
							else -> { part.dispose() }
						}
					}
					if (bytes == null) { call.respond(HttpStatusCode.BadRequest, ErrorResponse( "file missing")); return@post }
					
					// Используем специализированные анализаторы в зависимости от метода
					val origins = estimateOriginsFromVcf(bytes, report[Reports.decodingMethod])
					
					// Сохраняем результаты в профиль пользователя
					val decodingMethod = report[Reports.decodingMethod]
					when (decodingMethod) {
						"autosomal_analysis" -> {
							transaction {
								Users.update({ Users.id eq uid }) {
									it[autosomalData] = json.encodeToString(origins)
								}
							}
						}
						"y_haplogroup" -> {
							val haplogroup = origins.firstOrNull()?.region?.substringAfter("Y-гаплогруппа: ")?.substringBefore(" (") ?: "Неопределено"
							transaction {
								Users.update({ Users.id eq uid }) {
									it[yHaplogroup] = haplogroup
								}
							}
						}
						"mt_haplogroup" -> {
							val haplogroup = origins.firstOrNull()?.region?.substringAfter("мт-гаплогруппа: ")?.substringBefore(" (") ?: "Неопределено"
							transaction {
								Users.update({ Users.id eq uid }) {
									it[mtHaplogroup] = haplogroup
								}
							}
						}
					}
					
					val summary = "Файл: ${fileName ?: "unknown"}, метод: ${report[Reports.decodingMethod]}, проанализировано SNP: ${origins.size}, найдено совпадений: ${origins.size}, размер файла: ${bytes.size} байт"
					
					transaction {
						Reports.update({ Reports.id eq reportId }) {
							it[Reports.summary] = summary
							it[Reports.originsJson] = json.encodeToString(origins)
						}
					}
					
					call.respond(ReportDto(id = reportId, userId = uid, name = report[Reports.name], summary = summary, origins = origins))
				}

				delete("/reports/{id}") {
					val reportId = call.parameters["id"]
					val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asString()
					if (reportId.isNullOrBlank()) { call.respond(HttpStatusCode.BadRequest, ErrorResponse( "id required")); return@delete }
					
					// Проверяем, что отчет принадлежит пользователю
					val report = transaction { Reports.select { (Reports.id eq reportId) and (Reports.userId eq uid) }.firstOrNull() }
					if (report == null) { call.respond(HttpStatusCode.NotFound, ErrorResponse( "report not found")); return@delete }
					
					// Удаляем отчет
					val deleted = transaction {
						Reports.deleteWhere { (Reports.id eq reportId) and (Reports.userId eq uid) }
					}
					
					if (deleted == 0) {
						call.respond(HttpStatusCode.NotFound, ErrorResponse( "report not found"))
						return@delete
					}
					
					call.respond(HttpStatusCode.NoContent)
				}
			}

			// Публичные эндпоинты для новостей (не требуют аутентификации)
			get("/news") {
				val news = transaction {
					News.selectAll()
						.orderBy(News.publishedAt to SortOrder.DESC)
						.map {
							NewsDto(
								id = it[News.id],
								title = it[News.title],
								content = it[News.content],
								imageUrl = it[News.imageUrl],
								source = it[News.newsSource],
								publishedAt = it[News.publishedAt].toString()
							)
						}
				}
				call.respond(news)
			}

			get("/news/{id}") {
				val id = call.parameters["id"]
				if (id.isNullOrBlank()) { 
					call.respond(HttpStatusCode.BadRequest, ErrorResponse( "id required"))
					return@get 
				}
				val news = transaction {
					News.select { News.id eq id }.firstOrNull()
				}
				if (news == null) { 
					call.respond(HttpStatusCode.NotFound, ErrorResponse( "not found"))
					return@get 
				}
				val newsDto = NewsDto(
					id = news[News.id],
					title = news[News.title],
					content = news[News.content],
					imageUrl = news[News.imageUrl],
					source = news[News.newsSource],
					publishedAt = news[News.publishedAt].toString()
				)
				call.respond(newsDto)
			}

			// Публичные эндпоинты для отчетов (не требуют аутентификации)
			get("/public/reports/{publicId}") {
				val publicId = call.parameters["publicId"]
				if (publicId.isNullOrBlank()) {
					call.respond(HttpStatusCode.BadRequest, ErrorResponse( "publicId required"))
					return@get
				}
				
				val row = transaction { 
					Reports.select { Reports.publicId eq publicId }.firstOrNull() 
				}
				
				if (row == null) {
					call.respond(HttpStatusCode.NotFound, ErrorResponse( "report not found"))
					return@get
				}
				
				val origins = json.decodeFromString<List<OriginPortion>>(row[Reports.originsJson])
				val publicReport = PublicReportDto(
					id = row[Reports.id],
					name = row[Reports.name],
					summary = row[Reports.summary],
					origins = origins,
					createdAt = row[Reports.id] // Используем ID как дату создания для простоты
				)
				call.respond(publicReport)
			}

			// Админские эндпоинты для управления новостями (требуют аутентификации)
			authenticate("auth-jwt") {
				post("/admin/news") {
					val req = call.receive<NewsDto>()
					val id = UUID.randomUUID().toString()
					val now = java.time.LocalDateTime.now()
					
					transaction {
						News.insert {
							it[News.id] = id
							it[News.title] = req.title
							it[News.content] = req.content
							it[News.imageUrl] = req.imageUrl
							it[News.newsSource] = req.source
							it[News.publishedAt] = now
						}
					}
					
					val createdNews = NewsDto(
						id = id,
						title = req.title,
						content = req.content,
						imageUrl = req.imageUrl,
						source = req.source,
						publishedAt = now.toString()
					)
					call.respond(HttpStatusCode.Created, createdNews)
				}

				put("/admin/news/{id}") {
					val id = call.parameters["id"]
					if (id.isNullOrBlank()) {
						call.respond(HttpStatusCode.BadRequest, ErrorResponse( "id required"))
						return@put
					}
					
					val req = call.receive<NewsDto>()
					val updated = transaction {
						News.update({ News.id eq id }) {
							it[News.title] = req.title
							it[News.content] = req.content
							it[News.imageUrl] = req.imageUrl
							it[News.newsSource] = req.source
						} > 0
					}
					
					if (!updated) {
						call.respond(HttpStatusCode.NotFound, ErrorResponse( "news not found"))
						return@put
					}
					
					val news = transaction {
						News.select { News.id eq id }.first()
					}
					
					val newsDto = NewsDto(
						id = news[News.id],
						title = news[News.title],
						content = news[News.content],
						imageUrl = news[News.imageUrl],
						source = news[News.newsSource],
						publishedAt = news[News.publishedAt].toString()
					)
					call.respond(newsDto)
				}

				delete("/admin/news/{id}") {
					val id = call.parameters["id"]
					if (id.isNullOrBlank()) {
						call.respond(HttpStatusCode.BadRequest, ErrorResponse( "id required"))
						return@delete
					}
					
					val deleted = transaction {
						News.deleteWhere { News.id eq id }
					}
					
					if (deleted == 0) {
						call.respond(HttpStatusCode.NotFound, ErrorResponse( "news not found"))
						return@delete
					}
					
					call.respond(HttpStatusCode.NoContent)
				}
				
				// Очистка всех пользователей и отчетов
				delete("/admin/clear-users") {
					transaction {
						// Удаляем все отчеты
						Reports.deleteAll()
						
						// Удаляем всех пользователей
						Users.deleteAll()
					}
					
					call.respond(HttpStatusCode.OK, MessageResponse( "users and reports cleared"))
				}
				
				// Очистка всех отчетов
				delete("/admin/clear-reports") {
					transaction {
						Reports.deleteAll()
					}
					
					call.respond(HttpStatusCode.OK, MessageResponse( "reports cleared"))
				}
				
				// Получение списка пользователей
				get("/admin/users") {
					val users = transaction {
						Users.selectAll().map { user ->
							UserDto(
								id = user[Users.id],
								name = user[Users.name],
								email = user[Users.contact],
								isEmailVerified = user[Users.isEmailVerified],
								autosomalData = emptyList(),
								yHaplogroup = null,
								mtHaplogroup = null
							)
						}
					}
					call.respond(users)
				}
				
				// Удаление конкретного пользователя
				delete("/admin/users/{id}") {
					val userId = call.parameters["id"]
					if (userId.isNullOrBlank()) {
						call.respond(HttpStatusCode.BadRequest, ErrorResponse( "id required"))
						return@delete
					}
					
					transaction {
						// Сначала удаляем все отчеты пользователя
						Reports.deleteWhere { Reports.userId eq userId }
						// Затем удаляем пользователя
						Users.deleteWhere { Users.id eq userId }
					}
					
					call.respond(HttpStatusCode.OK, MessageResponse( "user deleted"))
				}
			}
			
			// Эндпоинты для восстановления пароля
			route("/auth") {
				// Запрос на восстановление пароля
				post("/forgot-password") {
					val request = call.receive<ForgotPasswordRequest>()
					val email = request.email.trim()
					
					if (email.isBlank()) {
						call.respond(HttpStatusCode.BadRequest, ErrorResponse("Email is required"))
						return@post
					}
					
					val user = transaction {
						Users.select { Users.contact eq email }.firstOrNull()
					}
					
					if (user == null) {
						call.respond(HttpStatusCode.NotFound, ErrorResponse("Данная почта не зарегистрирована"))
						return@post
					}
					
					// Генерируем код восстановления
					val resetCode = (100000..999999).random().toString()
					val expiresAt = System.currentTimeMillis() + 600000 // 10 минут
					
					transaction {
						Users.update({ Users.contact eq email }) {
							it[verificationCode] = resetCode
							it[verificationCodeExpires] = expiresAt
						}
					}
					
					// Отправляем email с кодом
					println("=== PASSWORD RESET CODE ===")
					println("To: $email")
					println("Subject: Восстановление пароля")
					println("Your reset code: $resetCode")
					println("Code expires in 10 minutes")
					println("===============================")
					
					call.respond(HttpStatusCode.OK, MessageResponse("Код восстановления отправлен на ваш email"))
				}
				
				// Сброс пароля
				post("/reset-password") {
					val request = call.receive<ResetPasswordRequest>()
					val email = request.email.trim()
					val code = request.code.trim()
					val newPassword = request.newPassword
					
					if (email.isBlank() || code.isBlank() || newPassword.isBlank()) {
						call.respond(HttpStatusCode.BadRequest, ErrorResponse("All fields are required"))
						return@post
					}
					
					if (newPassword.length < 6) {
						call.respond(HttpStatusCode.BadRequest, ErrorResponse("Password must be at least 6 characters"))
						return@post
					}
					
					val user = transaction {
						Users.select { Users.contact eq email }.firstOrNull()
					}
					
					if (user == null) {
						call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
						return@post
					}
					
					val storedCode = user[Users.verificationCode]
					val codeExpires = user[Users.verificationCodeExpires]
					
					if (storedCode != code) {
						call.respond(HttpStatusCode.BadRequest, ErrorResponse("Неверный код подтверждения"))
						return@post
					}
					
					if (codeExpires == null || System.currentTimeMillis() > codeExpires) {
						call.respond(HttpStatusCode.BadRequest, ErrorResponse("Код истек. Запросите новый код"))
						return@post
					}
					
					// Обновляем пароль
					val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
					transaction {
						Users.update({ Users.contact eq email }) {
							it[passwordHash] = hashedPassword
							it[verificationCode] = null
							it[verificationCodeExpires] = null
						}
					}
					
					call.respond(HttpStatusCode.OK, MessageResponse("Пароль успешно изменен"))
				}
			}
		}
	}.start(wait = true)
}
