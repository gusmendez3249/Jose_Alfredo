package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mx.utng.festivaltrack.app.data.factory.InteractiveBiographyItem
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.ForumPostType
import mx.utng.festivaltrack.app.ui.viewmodels.ForumViewModel
import mx.utng.festivaltrack.shared.data.remote.PublicacionDto

/**
 * Galería Interactiva: foro de la comunidad donde los asistentes comparten
 * experiencias, recomendaciones y fotos del festival. Dos vistas: "Foro"
 * (publicaciones completas) y "Mosaico" (solo fotos, estilo galería).
 */
@Composable
fun GalleryScreen(viewModel: ForumViewModel = viewModel()) {
    val posts by viewModel.posts.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val endReached by viewModel.endReached.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val isPublishing by viewModel.isPublishing.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentsLoading by viewModel.commentsLoading.collectAsState()
    val archive by viewModel.archive.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    var mosaic by rememberSaveable { mutableStateOf(false) }
    var showComposer by remember { mutableStateOf(false) }
    var commentsPost by remember { mutableStateOf<PublicacionDto?>(null) }
    var viewerUrl by remember { mutableStateOf<String?>(null) }
    var archiveItem by remember { mutableStateOf<InteractiveBiographyItem?>(null) }
    val showArchive = filter == null && archive.isNotEmpty()

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    val listState = rememberLazyListState()
    // Cuando el usuario se acerca al final del feed se pide la siguiente página.
    val nearEnd by remember { derivedStateOf { listState.isNearEnd(posts.size) } }
    LaunchedEffect(nearEnd, posts.size) { if (nearEnd && !mosaic) viewModel.loadMore() }

    val visiblePosts = if (mosaic) posts.filter { !it.imagenUrl.isNullOrBlank() } else posts

    Scaffold(
        containerColor = ForumBg,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (viewModel.isLoggedIn) showComposer = true else viewModel.notifyLoginRequired()
                },
                containerColor = PrimaryGold,
                contentColor = Color.Black,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Compartir", fontWeight = FontWeight.Bold) }
            )
        }
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            ForumHeader(
                filter = filter,
                mosaic = mosaic,
                onFilter = viewModel::setFilter,
                onToggleMosaic = { mosaic = !mosaic },
                onRefresh = viewModel::refresh
            )

            when {
                isLoading && posts.isEmpty() -> FeedSkeleton()
                loadError != null && posts.isEmpty() -> StateMessage(
                    title = "Sin conexión con el foro",
                    body = loadError!!,
                    action = "Reintentar",
                    onAction = viewModel::refresh
                )
                visiblePosts.isEmpty() -> LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showArchive) item { ArchiveCarousel(archive) { archiveItem = it } }
                    item {
                        StateMessage(
                            title = if (mosaic) "Aún no hay fotos" else "Aún no hay publicaciones",
                            body = "Sé el primero en compartir tu experiencia, una recomendación o una foto del festival.",
                            action = "Compartir ahora",
                            onAction = { if (viewModel.isLoggedIn) showComposer = true else viewModel.notifyLoginRequired() },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                        )
                    }
                }
                mosaic -> LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showArchive) item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                        ArchiveCarousel(archive) { archiveItem = it }
                    }
                    items(visiblePosts, key = { it.id }) { post ->
                        val url = resolveImageUrl(post.imagenUrl) ?: return@items
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ForumField)
                                .clickable { viewerUrl = url }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                                contentDescription = post.titulo ?: "Foto de ${post.autorNombre}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 300.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("♥ ${post.likes}", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showArchive) item { ArchiveCarousel(archive) { archiveItem = it } }
                    items(visiblePosts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            canDelete = viewModel.isAdmin || post.autorId == viewModel.currentUserId,
                            onLike = { viewModel.toggleLike(post) },
                            onComments = {
                                commentsPost = post
                                viewModel.openComments(post.id)
                            },
                            onDelete = { viewModel.deletePost(post) },
                            onOpenImage = { viewerUrl = it }
                        )
                    }
                    item {
                        when {
                            isLoadingMore -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PrimaryGold, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                            endReached && posts.size > 3 -> Text(
                                "Has llegado al final · gracias por ser parte de la comunidad",
                                color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showComposer) {
        ComposerSheet(
            isPublishing = isPublishing,
            onPublish = { type, title, content, place, uri ->
                viewModel.publish(type, title, content, place, uri) { showComposer = false }
            },
            onDismiss = { showComposer = false }
        )
    }

    commentsPost?.let { post ->
        CommentsSheet(
            comments = comments,
            isLoading = commentsLoading,
            isLoggedIn = viewModel.isLoggedIn,
            currentUserId = viewModel.currentUserId,
            isAdmin = viewModel.isAdmin,
            onSend = { text, done -> viewModel.sendComment(post.id, text, done) },
            onDelete = { viewModel.deleteComment(post.id, it) },
            onLoginRequired = viewModel::notifyLoginRequired,
            onDismiss = {
                commentsPost = null
                viewModel.closeComments()
            }
        )
    }

    archiveItem?.let { ArchiveDetailDialog(it) { archiveItem = null } }
    viewerUrl?.let { ImageViewerDialog(url = it, onDismiss = { viewerUrl = null }) }
}

private fun LazyListState.isNearEnd(total: Int): Boolean {
    if (total == 0) return false
    val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return false
    return last >= total - 3
}

@Composable
private fun ForumHeader(
    filter: ForumPostType?,
    mosaic: Boolean,
    onFilter: (ForumPostType?) -> Unit,
    onToggleMosaic: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "GALERÍA INTERACTIVA", color = PrimaryGold, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
                )
                Text("Comunidad del Festival", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onToggleMosaic) {
                Icon(
                    if (mosaic) Icons.Default.ViewAgenda else Icons.Default.GridView,
                    contentDescription = if (mosaic) "Ver como foro" else "Ver como mosaico de fotos",
                    tint = PrimaryGold
                )
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar", tint = Color.LightGray) }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { ForumFilterChip("Todo", filter == null, PrimaryGold) { onFilter(null) } }
            items(ForumPostType.entries.toList()) { t ->
                ForumFilterChip(
                    when (t) {
                        ForumPostType.EXPERIENCIA -> "Experiencias"
                        ForumPostType.RECOMENDACION -> "Recomendaciones"
                        ForumPostType.FOTO -> "Fotos"
                    },
                    filter == t, typeColor(t)
                ) { onFilter(t) }
            }
        }
    }
}

@Composable
private fun ForumFilterChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = ForumCard, labelColor = Color.LightGray,
            selectedContainerColor = color.copy(alpha = 0.22f), selectedLabelColor = color
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected,
            borderColor = ForumBorder, selectedBorderColor = color.copy(alpha = 0.7f)
        )
    )
}

@Composable
private fun StateMessage(
    title: String, body: String, action: String, onAction: () -> Unit,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, color = Color.Gray, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) { Text(action, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun FeedSkeleton() {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.35f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha"
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ForumCard.copy(alpha = alpha))
            )
        }
    }
}
