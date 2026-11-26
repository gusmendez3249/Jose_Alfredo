package mx.utng.festivaltrack.app.auto.mapa

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow

/** Punto marcado en el mapa del auto. [etiqueta] es corta: "1", "2"… o "★". */
data class MarcadorMapa(val lat: Double, val lon: Double, val etiqueta: String, val destacado: Boolean = false)

/**
 * Mapa propio de FestivalTrack en la pantalla del auto.
 *
 * Dibuja sobre la superficie que presta el auto (plantillas de mapa): mosaicos de OpenStreetMap en
 * estilo oscuro, la ruta en dorado (el color de la app), los lugares del festival numerados, el
 * destino y la posición del auto. Responde a arrastrar y pellizcar en la pantalla y a los botones
 * de zoom de la plantilla. Lo que se ve se ajusta al área libre que deja el panel de información.
 */
class MapaAutoRenderer : SurfaceCallback {

    private var contenedor: SurfaceContainer? = null
    private var areaVisible: Rect? = null
    private val principal = Handler(Looper.getMainLooper())
    private var dibujoPendiente = false
    private val mosaicos = MosaicosMapa { pedirDibujo() }

    private var vista = Vista(21.157511, -100.934529, 15.0)
    private var ruta: List<Pair<Double, Double>> = emptyList()
    private var marcadores: List<MarcadorMapa> = emptyList()
    private var destino: Pair<Double, Double>? = null
    private var yo: Pair<Double, Double>? = null

    /** true mientras el mapa sigue al auto; se apaga al mover el mapa con el dedo. */
    var siguiendo = false
        private set

    /** Al cambiar si el mapa sigue al auto (para mostrar u ocultar el botón "centrar"). */
    var alCambiarSeguimiento: ((Boolean) -> Unit)? = null

    /** Encuadre que se aplica en cuanto se conoce el área visible. */
    private var encuadrePendiente = true

    // ─── Datos ───────────────────────────────────────────────────────────────

    fun mostrar(
        ruta: List<Pair<Double, Double>> = this.ruta,
        marcadores: List<MarcadorMapa> = this.marcadores,
        destino: Pair<Double, Double>? = this.destino
    ) {
        this.ruta = ruta
        this.marcadores = marcadores
        this.destino = destino
        pedirDibujo()
    }

    fun ponerUbicacion(lat: Double, lon: Double) {
        yo = lat to lon
        if (siguiendo) vista = vista.copy(lat = lat, lon = lon)
        pedirDibujo()
    }

    /** Ajusta la vista para que se vea todo: ruta, lugares, destino y el auto. */
    fun encuadrarTodo() {
        cambiarSeguimiento(false)
        val area = areaVisible
        val puntos = ruta + marcadores.map { it.lat to it.lon } + listOfNotNull(destino, yo)
        if (area == null || area.width() <= 0) {
            encuadrePendiente = true
            return
        }
        ProyeccionMapa.encuadrar(puntos, area.width().toDouble(), area.height().toDouble(), MARGEN_PX)
            ?.let { vista = it }
        encuadrePendiente = false
        pedirDibujo()
    }

    /** Acerca el mapa al auto y lo sigue mientras avanza. */
    fun seguirAuto() {
        val actual = yo ?: return encuadrarTodo()
        cambiarSeguimiento(true)
        vista = Vista(actual.first, actual.second, ZOOM_SEGUIR)
        pedirDibujo()
    }

    fun acercar() = escalar(2f, null, null)
    fun alejar() = escalar(0.5f, null, null)

    /**
     * Deja de dibujar: la pantalla del mapa ya no está a la vista y la superficie del auto
     * pasa a otra pantalla. Dos mapas dibujando en la misma superficie cierran la app.
     */
    fun soltarSuperficie() {
        contenedor = null
        dibujoPendiente = false
        principal.removeCallbacksAndMessages(null)
    }

    fun liberar() {
        soltarSuperficie()
        mosaicos.liberar()
    }

    // ─── SurfaceCallback ─────────────────────────────────────────────────────

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        contenedor = surfaceContainer
        pedirDibujo()
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        contenedor = null
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        areaVisible = Rect(visibleArea)
        if (encuadrePendiente) encuadrarTodo() else pedirDibujo()
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        if (areaVisible == null) areaVisible = Rect(stableArea)
        pedirDibujo()
    }

    override fun onScroll(distanceX: Float, distanceY: Float) {
        cambiarSeguimiento(false)
        val z = vista.zoom
        val x = ProyeccionMapa.xMundo(vista.lon, z) + distanceX
        val y = ProyeccionMapa.yMundo(vista.lat, z) + distanceY
        vista = Vista(ProyeccionMapa.latDe(y, z), ProyeccionMapa.lonDe(x, z), z)
        pedirDibujo()
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        escalar(scaleFactor, focusX.takeIf { it >= 0 }, focusY.takeIf { it >= 0 })
    }

    // ─── Dibujo ──────────────────────────────────────────────────────────────

    /** Cambia el zoom dejando fijo el punto bajo el dedo (o el centro, con los botones). */
    private fun escalar(factor: Float, focoX: Float?, focoY: Float?) {
        val nuevoZoom = (vista.zoom + log2(factor.toDouble())).coerceIn(ProyeccionMapa.ZOOM_MIN, ProyeccionMapa.ZOOM_MAX)
        if (focoX == null || focoY == null || siguiendo) {
            vista = vista.copy(zoom = nuevoZoom)
        } else {
            val (cx, cy) = centroPantalla()
            val z = vista.zoom
            val fx = ProyeccionMapa.xMundo(vista.lon, z) + (focoX - cx)
            val fy = ProyeccionMapa.yMundo(vista.lat, z) + (focoY - cy)
            val latFoco = ProyeccionMapa.latDe(fy, z)
            val lonFoco = ProyeccionMapa.lonDe(fx, z)
            val nx = ProyeccionMapa.xMundo(lonFoco, nuevoZoom) - (focoX - cx)
            val ny = ProyeccionMapa.yMundo(latFoco, nuevoZoom) - (focoY - cy)
            vista = Vista(ProyeccionMapa.latDe(ny, nuevoZoom), ProyeccionMapa.lonDe(nx, nuevoZoom), nuevoZoom)
        }
        pedirDibujo()
    }

    private fun cambiarSeguimiento(valor: Boolean) {
        if (siguiendo == valor) return
        siguiendo = valor
        alCambiarSeguimiento?.invoke(valor)
    }

    private fun pedirDibujo() {
        if (dibujoPendiente) return
        dibujoPendiente = true
        principal.post {
            dibujoPendiente = false
            dibujar()
        }
    }

    private fun centroPantalla(): Pair<Float, Float> {
        val area = areaVisible
        val c = contenedor
        return when {
            area != null && area.width() > 0 -> area.exactCenterX() to area.exactCenterY()
            c != null -> c.width / 2f to c.height / 2f
            else -> 0f to 0f
        }
    }

    private fun dibujar() {
        val c = contenedor ?: return
        val superficie = c.surface ?: return
        if (!superficie.isValid) return
        // Canvas por software: el de hardware aborta la app si el auto retira la superficie a la mitad.
        val canvas = runCatching { superficie.lockCanvas(null) }.getOrNull() ?: return
        try {
            canvas.drawColor(COLOR_FONDO)
            dibujarMosaicos(canvas, c.width, c.height)
            dibujarRuta(canvas)
            marcadores.forEach { dibujarMarcador(canvas, it) }
            destino?.let { dibujarDestino(canvas, it) }
            yo?.let { dibujarYo(canvas, it) }
            dibujarCreditos(canvas)
        } finally {
            runCatching { superficie.unlockCanvasAndPost(canvas) }
        }
    }

    private fun aPantalla(lat: Double, lon: Double): Pair<Float, Float> {
        val (cx, cy) = centroPantalla()
        val z = vista.zoom
        val x = ProyeccionMapa.xMundo(lon, z) - ProyeccionMapa.xMundo(vista.lon, z) + cx
        val y = ProyeccionMapa.yMundo(lat, z) - ProyeccionMapa.yMundo(vista.lat, z) + cy
        return x.toFloat() to y.toFloat()
    }

    private fun dibujarMosaicos(canvas: Canvas, ancho: Int, alto: Int) {
        val zEntero = floor(vista.zoom).toInt().coerceIn(ProyeccionMapa.ZOOM_MIN.toInt(), ProyeccionMapa.ZOOM_MAX.toInt())
        val escala = 2.0.pow(vista.zoom - zEntero)
        val tam = ProyeccionMapa.TAM_MOSAICO * escala
        val (cx, cy) = centroPantalla()
        val mundoX = ProyeccionMapa.xMundo(vista.lon, zEntero.toDouble()) * escala
        val mundoY = ProyeccionMapa.yMundo(vista.lat, zEntero.toDouble()) * escala
        val izquierda = mundoX - cx
        val arriba = mundoY - cy
        val x0 = floor(izquierda / tam).toInt()
        val y0 = floor(arriba / tam).toInt()
        val x1 = floor((izquierda + ancho) / tam).toInt()
        val y1 = floor((arriba + alto) / tam).toInt()
        val destinoRect = RectF()
        for (tx in x0..x1) for (ty in y0..y1) {
            val px = (tx * tam - izquierda).toFloat()
            val py = (ty * tam - arriba).toFloat()
            destinoRect.set(px, py, px + tam.toFloat() + 0.5f, py + tam.toFloat() + 0.5f)
            val bitmap = mosaicos.obtener(zEntero, tx, ty)
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, null, destinoRect, pincelMosaico)
            } else {
                mosaicos.respaldo(zEntero, tx, ty)?.let { (padre, factor) -> dibujarRespaldo(canvas, padre, factor, tx, ty, destinoRect) }
            }
        }
    }

    /** Pinta la parte que corresponde de un mosaico de menor zoom, ampliada. */
    private fun dibujarRespaldo(canvas: Canvas, padre: Bitmap, factor: Int, tx: Int, ty: Int, destinoRect: RectF) {
        val lado = padre.width / factor
        val sx = Math.floorMod(tx, factor) * lado
        val sy = Math.floorMod(ty, factor) * lado
        canvas.drawBitmap(padre, Rect(sx, sy, sx + lado, sy + lado), destinoRect, pincelMosaico)
    }

    private fun dibujarRuta(canvas: Canvas) {
        if (ruta.size < 2) return
        val trazo = Path()
        ruta.forEachIndexed { i, (lat, lon) ->
            val (x, y) = aPantalla(lat, lon)
            if (i == 0) trazo.moveTo(x, y) else trazo.lineTo(x, y)
        }
        canvas.drawPath(trazo, pincelRutaBorde)
        canvas.drawPath(trazo, pincelRuta)
        // Tramos punteados: del auto al inicio de la ruta y del final al destino exacto.
        yo?.let { punteado(canvas, it, ruta.first()) }
        destino?.let { punteado(canvas, ruta.last(), it) }
    }

    private fun punteado(canvas: Canvas, a: Pair<Double, Double>, b: Pair<Double, Double>) {
        val (ax, ay) = aPantalla(a.first, a.second)
        val (bx, by) = aPantalla(b.first, b.second)
        canvas.drawLine(ax, ay, bx, by, pincelPunteado)
    }

    private fun dibujarMarcador(canvas: Canvas, m: MarcadorMapa) {
        val (x, y) = aPantalla(m.lat, m.lon)
        val radio = if (m.destacado) 22f else 18f
        canvas.drawCircle(x, y, radio + 3f, pincelBordeOscuro)
        canvas.drawCircle(x, y, radio, if (m.destacado) pincelDorado else pincelMarcador)
        canvas.drawText(m.etiqueta, x, y + pincelEtiqueta.textSize / 3f, pincelEtiqueta)
    }

    /** Destino: pin dorado con punta. */
    private fun dibujarDestino(canvas: Canvas, d: Pair<Double, Double>) {
        val (x, y) = aPantalla(d.first, d.second)
        val pin = Path().apply {
            moveTo(x, y)
            lineTo(x - 14f, y - 24f)
            lineTo(x + 14f, y - 24f)
            close()
        }
        canvas.drawCircle(x, y - 34f, 21f, pincelBordeOscuro)
        canvas.drawPath(pin, pincelDorado)
        canvas.drawCircle(x, y - 34f, 18f, pincelDorado)
        canvas.drawCircle(x, y - 34f, 7f, pincelBordeOscuro)
    }

    /** El auto: punto azul con borde blanco y halo. */
    private fun dibujarYo(canvas: Canvas, p: Pair<Double, Double>) {
        val (x, y) = aPantalla(p.first, p.second)
        canvas.drawCircle(x, y, 26f, pincelHalo)
        canvas.drawCircle(x, y, 13f, pincelBlanco)
        canvas.drawCircle(x, y, 9f, pincelYo)
    }

    /** Los datos de OpenStreetMap exigen dar crédito sobre el mapa. */
    private fun dibujarCreditos(canvas: Canvas) {
        val area = areaVisible ?: return
        canvas.drawText("© OpenStreetMap", area.right - 12f, area.bottom - 12f, pincelCreditos)
    }

    // ─── Pinceles ────────────────────────────────────────────────────────────

    private val pincelMosaico = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            setSaturation(0f)
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        -0.78f, 0f, 0f, 0f, 225f,
                        0f, -0.78f, 0f, 0f, 225f,
                        0f, 0f, -0.82f, 0f, 235f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        })
    }
    private val pincelRutaBorde = trazo(Color.rgb(60, 45, 10), 16f)
    private val pincelRuta = trazo(COLOR_DORADO, 9f)
    private val pincelPunteado = trazo(COLOR_DORADO, 5f).apply { pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f) }
    private val pincelDorado = relleno(COLOR_DORADO)
    private val pincelMarcador = relleno(Color.rgb(250, 245, 230))
    private val pincelBordeOscuro = relleno(Color.rgb(28, 27, 31))
    private val pincelBlanco = relleno(Color.WHITE)
    private val pincelYo = relleno(Color.rgb(66, 133, 244))
    private val pincelHalo = relleno(Color.argb(70, 66, 133, 244))
    private val pincelEtiqueta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(28, 27, 31)
        textSize = 22f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val pincelCreditos = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 230, 230, 230)
        textSize = 14f
        textAlign = Paint.Align.RIGHT
    }

    private fun trazo(color: Int, ancho: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = ancho
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun relleno(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    companion object {
        /** Dorado de FestivalTrack (#E6C27A). */
        val COLOR_DORADO = Color.rgb(230, 194, 122)
        private val COLOR_FONDO = Color.rgb(28, 28, 30)
        private const val MARGEN_PX = 70.0
        private const val ZOOM_SEGUIR = 16.5
    }
}
