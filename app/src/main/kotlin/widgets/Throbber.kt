package widgets

import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.RenderingHints
import java.util.*
import javax.swing.JComponent

// degrees per second
private const val rotationRate = 360.0f;

// 1 divided by frames per second
private const val deltaTime = 1.0f / 30.0f;

class Throbber(private val image: Image, private val width: Int, private val height: Int, paddingX: Int, paddingY: Int) : JComponent() {

    companion object {
        private const val serialVersionUID = 1L
    }

    private val centerX = (width.toDouble() + paddingX * 2) / 2
    private val centerY = (height.toDouble() + paddingY * 2) / 2
    private val offsetX = paddingX
    private val offsetY = paddingY

    private var angle = 0.0;
    private var isRunning = false;
    private var timer: Timer? = null;

    fun startAnimation() {
        timer = Timer().also {
            val task = object : TimerTask() {
                override fun run() {
                    updateRotation()
                }
            }
            it.scheduleAtFixedRate(task, 0L, (deltaTime * 1000.0f).toLong())
            isRunning = true
        }
    }

    fun stopAnimation() {
        isRunning = false
        timer?.cancel()
        timer?.purge()
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        if (!isRunning) return
        val g2d = g as Graphics2D
        g2d.rotate(Math.toRadians(angle), centerX, centerY)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.drawImage(image, offsetX, offsetY, width, height, null)
    }

    private fun updateRotation() {
        angle += rotationRate * deltaTime;
        if (angle > 360.0f) angle -= 360.0f;
        repaint();
    }
}
