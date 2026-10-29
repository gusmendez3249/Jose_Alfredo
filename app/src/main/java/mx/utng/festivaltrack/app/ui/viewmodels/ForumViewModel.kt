package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.app.data.factory.InteractiveBiographyItem
import mx.utng.festivaltrack.app.data.firebase.BiographyFirestoreService
import mx.utng.festivaltrack.shared.data.remote.ComentarioCreateDto
import mx.utng.festivaltrack.shared.data.remote.ComentarioDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.PublicacionCreateDto
import mx.utng.festivaltrack.shared.data.remote.PublicacionDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

/** Tipos de publicación del foro (coinciden con el enum TipoPublicacion del backend). */
enum class ForumPostType(val api: String, val label: String) {
    EXPERIENCIA("EXPERIENCIA", "Experiencia"),
    RECOMENDACION("RECOMENDACION", "Recomendación"),
    FOTO("FOTO", "Foto");

    companion object {
        fun fromApi(value: String) = entries.firstOrNull { it.api == value } ?: EXPERIENCIA
    }
}

/**
 * Estado y acciones del foro de la Galería Interactiva. Los datos viven en el
 * backend (PostgreSQL) para que todos los usuarios vean lo mismo; las fotos se
 * comprimen en el dispositivo antes de subirse.
 */
class ForumViewModel(application: Application) : AndroidViewModel(application) {
    private val api = FestivalApiService.create()
    private val tokenManager = TokenManager(application)

    /** Archivo histórico oficial (alta del administrador vía Factory), mostrado en la galería. */
    val archive: StateFlow<List<InteractiveBiographyItem>> = BiographyFirestoreService.getInstance(application).itemsFlow

    private val _posts = MutableStateFlow<List<PublicacionDto>>(emptyList())
    val posts: StateFlow<List<PublicacionDto>> = _posts.asStateFlow()

    private val _filter = MutableStateFlow<ForumPostType?>(null)
    val filter: StateFlow<ForumPostType?> = _filter.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _endReached = MutableStateFlow(false)
    val endReached: StateFlow<Boolean> = _endReached.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _comments = MutableStateFlow<List<ComentarioDto>>(emptyList())
    val comments: StateFlow<List<ComentarioDto>> = _comments.asStateFlow()

    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val isLoggedIn: Boolean get() = !tokenManager.getToken().isNullOrBlank()
    val currentUserId: String? get() = tokenManager.getUserId()
    val isAdmin: Boolean get() = tokenManager.getUserRole()?.uppercase() == "ADMINISTRADOR"

    private var loadJob: kotlinx.coroutines.Job? = null
    private var commentsPostId: String? = null

    init {
        refresh()
    }

    private fun bearer(): String? = tokenManager.getToken()?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

    fun setFilter(type: ForumPostType?) {
        if (_filter.value == type) return
        _filter.value = type
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            _endReached.value = false
            try {
                val page = api.getForo(bearer(), _filter.value?.api, null, PAGE_SIZE)
                _posts.value = page
                _endReached.value = page.size < PAGE_SIZE
            } catch (e: Exception) {
                _loadError.value = "No pudimos cargar el foro. Revisa tu conexión e intenta de nuevo."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        if (_isLoading.value || _isLoadingMore.value || _endReached.value) return
        val cursor = _posts.value.lastOrNull()?.createdAt ?: return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val page = api.getForo(bearer(), _filter.value?.api, cursor, PAGE_SIZE)
                val known = _posts.value.mapTo(HashSet()) { it.id }
                _posts.update { it + page.filter { p -> p.id !in known } }
                _endReached.value = page.size < PAGE_SIZE
            } catch (e: Exception) {
                _messages.tryEmit("No se pudieron cargar más publicaciones")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    /** "Me gusta" optimista: se refleja al instante y se corrige con la respuesta del servidor. */
    fun toggleLike(post: PublicacionDto) {
        val token = bearer() ?: return notifyLoginRequired()
        patchPost(post.id) {
            it.copy(likedByMe = !it.likedByMe, likes = (it.likes + if (it.likedByMe) -1 else 1).coerceAtLeast(0))
        }
        viewModelScope.launch {
            try {
                val res = api.toggleLikePublicacion(token, post.id)
                patchPost(post.id) { it.copy(likedByMe = res.liked, likes = res.likes) }
            } catch (e: Exception) {
                patchPost(post.id) { it.copy(likedByMe = post.likedByMe, likes = post.likes) }
                _messages.tryEmit("No se pudo registrar tu me gusta")
            }
        }
    }

    fun publish(
        type: ForumPostType,
        titulo: String,
        contenido: String,
        lugar: String,
        imageUri: Uri?,
        onDone: () -> Unit
    ) {
        val token = bearer() ?: return notifyLoginRequired()
        if (_isPublishing.value) return
        viewModelScope.launch {
            _isPublishing.value = true
            try {
                val imageUrl = imageUri?.let { uploadImage(token, it) }
                val created = api.crearPublicacion(
                    token,
                    PublicacionCreateDto(
                        tipo = type.api,
                        titulo = titulo.trim().ifBlank { null },
                        contenido = contenido.trim(),
                        imagenUrl = imageUrl,
                        lugar = lugar.trim().ifBlank { null }
                    )
                )
                val matchesFilter = _filter.value == null || _filter.value?.api == created.tipo
                if (matchesFilter) _posts.update { listOf(created) + it }
                _messages.tryEmit("¡Publicado! Gracias por compartir")
                onDone()
            } catch (e: Exception) {
                _messages.tryEmit(
                    if (e is IOException) "Sin conexión. No se pudo publicar."
                    else "No se pudo publicar. Intenta de nuevo."
                )
            } finally {
                _isPublishing.value = false
            }
        }
    }

    fun deletePost(post: PublicacionDto) {
        val token = bearer() ?: return notifyLoginRequired()
        viewModelScope.launch {
            try {
                api.eliminarPublicacion(token, post.id)
                _posts.update { list -> list.filterNot { it.id == post.id } }
                _messages.tryEmit("Publicación eliminada")
            } catch (e: Exception) {
                _messages.tryEmit("No se pudo eliminar la publicación")
            }
        }
    }

    // ─── Comentarios ─────────────────────────────────────────────────────────

    fun openComments(postId: String) {
        commentsPostId = postId
        _comments.value = emptyList()
        viewModelScope.launch {
            _commentsLoading.value = true
            try {
                val list = api.getComentarios(postId)
                if (commentsPostId == postId) _comments.value = list
            } catch (e: Exception) {
                _messages.tryEmit("No se pudieron cargar los comentarios")
            } finally {
                _commentsLoading.value = false
            }
        }
    }

    fun closeComments() {
        commentsPostId = null
        _comments.value = emptyList()
    }

    fun sendComment(postId: String, texto: String, onSent: () -> Unit) {
        val token = bearer() ?: return notifyLoginRequired()
        val clean = texto.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            try {
                val c = api.comentar(token, postId, ComentarioCreateDto(clean))
                if (commentsPostId == postId) _comments.update { it + c }
                patchPost(postId) { it.copy(comentarios = it.comentarios + 1) }
                onSent()
            } catch (e: Exception) {
                _messages.tryEmit("No se pudo enviar tu comentario")
            }
        }
    }

    fun deleteComment(postId: String, comment: ComentarioDto) {
        val token = bearer() ?: return notifyLoginRequired()
        viewModelScope.launch {
            try {
                api.eliminarComentario(token, comment.id)
                _comments.update { list -> list.filterNot { it.id == comment.id } }
                patchPost(postId) { it.copy(comentarios = (it.comentarios - 1).coerceAtLeast(0)) }
            } catch (e: Exception) {
                _messages.tryEmit("No se pudo eliminar el comentario")
            }
        }
    }

    // ─── Utilidades ──────────────────────────────────────────────────────────

    fun notifyLoginRequired() {
        _messages.tryEmit("Inicia sesión para participar en el foro")
    }

    private fun patchPost(id: String, transform: (PublicacionDto) -> PublicacionDto) {
        _posts.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    private suspend fun uploadImage(token: String, uri: Uri): String = withContext(Dispatchers.IO) {
        val file = compressImage(uri)
        try {
            val body = file.asRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("file", file.name, body)
            api.subirImagen(token, part).url
        } finally {
            file.delete()
        }
    }

    /** Reduce la foto a máx. 1600 px por lado, corrige su orientación EXIF y la guarda como JPEG. */
    private fun compressImage(uri: Uri): File {
        val resolver = getApplication<Application>().contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_SIDE || bounds.outHeight / (sample * 2) >= MAX_SIDE) sample *= 2

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IOException("No se pudo leer la imagen")

        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val finalBitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        val out = File.createTempFile("foro_", ".jpg", getApplication<Application>().cacheDir)
        out.outputStream().use { finalBitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        if (finalBitmap !== decoded) decoded.recycle()
        finalBitmap.recycle()
        return out
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val MAX_SIDE = 1600
    }
}
