package mx.utng.festivaltrack.app.ui.utils

import android.content.Context
import android.hardware.Camera
import android.view.Surface
import android.view.WindowManager

/**
 * Rotación para la vista previa y el video de la transmisión en vivo.
 *
 * `CameraHelper.getCameraOrientation` de RootEncoder solo mira la rotación de la pantalla y
 * supone que el sensor de la cámara trasera está montado a 90°. Muchos teléfonos son así, pero
 * otros lo tienen a 270° o 0° (por ejemplo el emulador de Android 16): ahí la vista previa del
 * administrador y el video que reciben los asistentes salían girados 90°. Aquí se usa la
 * orientación real del sensor.
 */
object RotacionCamara {

    /**
     * Grados a rotar la imagen para verla derecha.
     *
     * @param sensor Orientación del sensor (`Camera.CameraInfo.orientation`): 0, 90, 180 o 270.
     * @param pantalla Rotación de la pantalla en grados: 0, 90, 180 o 270.
     * @param frontal true para la cámara frontal (su imagen va en espejo).
     */
    fun calcular(sensor: Int, pantalla: Int, frontal: Boolean = false): Int =
        if (frontal) (360 - (sensor + pantalla) % 360) % 360
        else (sensor - pantalla + 360) % 360

    /** Rotación para la cámara trasera de este teléfono en su orientación actual. */
    @Suppress("DEPRECATION")
    fun traseraActual(context: Context): Int {
        val pantalla = when ((context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        val sensor = runCatching {
            val info = Camera.CameraInfo()
            (0 until Camera.getNumberOfCameras())
                .firstOrNull { id -> Camera.getCameraInfo(id, info); info.facing == Camera.CameraInfo.CAMERA_FACING_BACK }
                ?.let { info.orientation }
        }.getOrNull() ?: 90 // Lo más común si no se puede leer.
        return calcular(sensor, pantalla)
    }
}
