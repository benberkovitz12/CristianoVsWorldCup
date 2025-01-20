package com.example.cristianovsworldcup

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class TiltGameActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private val matrix = Array(11) { IntArray(5) { 0 } }
    private val amberViews = mutableMapOf<Pair<Int, Int>, ImageView>()
    private var jackPosition = 1
    private var score = 0
    private var amberSpeed = 500L
    private var lives = 3

    private lateinit var jackImageView: ImageView
    private lateinit var scoreText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        jackImageView = findViewById(R.id.imageView)
        scoreText = findViewById(R.id.scoreText)

        findViewById<View>(R.id.button).visibility = View.GONE
        findViewById<View>(R.id.button2).visibility = View.GONE


        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)


        initializeMatrix()
        startBallMovement()
        startAddingBalls()
        updateHearts()


        mediaPlayer = MediaPlayer.create(this, R.raw.soundtrack)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
    }

    override fun onPause() {
        super.onPause()

        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()

        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mediaPlayer.release()
    }

    private fun initializeMatrix() {
        for (row in matrix.indices) {
            for (col in matrix[row].indices) {
                matrix[row][col] = 0
            }
        }
        matrix[matrix.size - 1][jackPosition] = 2
    }

    private fun startAddingBalls() {
        handler.post(object : Runnable {
            override fun run() {
                addNewBall()

                val delay = when {
                    score < 10 -> 3000L
                    score < 20 -> 2000L
                    score < 35 -> 1500L
                    else -> 850L
                }

                handler.postDelayed(this, delay)
            }
        })
    }

    private fun addNewBall() {
        val column = Random.nextInt(0, 5)
        matrix[0][column] = 1
        updateUI()
    }

    private fun startBallMovement() {
        handler.post(object : Runnable {
            override fun run() {
                moveAllBallsDown()
                adjustSpeed()
                handler.postDelayed(this, amberSpeed)
            }
        })
    }

    private fun moveAllBallsDown() {
        for (row in matrix.size - 1 downTo 1) {
            for (col in matrix[row].indices) {
                if (matrix[row - 1][col] == 1) {
                    matrix[row][col] = 1
                    matrix[row - 1][col] = 0
                }
            }
        }

        for (col in matrix[matrix.size - 1].indices) {
            if (matrix[matrix.size - 1][col] == 1) {

                handleCollisions()
            }
        }

        updateUI()
    }

    private fun handleCollisions() {
        val lastRow = matrix.size - 1

        for (col in matrix[lastRow].indices) {
            if (matrix[lastRow][col] == 1) {
                if (col == jackPosition) {
                    lives -= 1
                    score -= 1
                    updateHearts()
                    showCollisionEffect()

                    if (lives <= 0) {
                        gameOver()
                    }
                } else {
                    score += 1
                }


                matrix[lastRow][col] = 0
                val key = Pair(lastRow, col)
                amberViews[key]?.let { view ->
                    (view.parent as? RelativeLayout)?.removeView(view)
                    amberViews.remove(key)
                }
            }
        }

        updateScoreUI()
    }

    private fun updateGoalPosition() {
        val laneWidth = resources.displayMetrics.widthPixels / 5
        val jackX = jackPosition * laneWidth + laneWidth / 2 - jackImageView.width / 2
        jackImageView.x = jackX.toFloat()
    }

    private fun showCollisionEffect() {
        val collisionSound = MediaPlayer.create(this, R.raw.hitsound)
        collisionSound.start()
        collisionSound.setOnCompletionListener { it.release() }

        jackImageView.setImageResource(R.drawable.pirates)
        Handler(Looper.getMainLooper()).postDelayed({
            jackImageView.setImageResource(R.drawable.jackrunning)
        }, 500)
    }

    private fun updateScoreUI() {
        scoreText.text = "Score: $score"
    }

    private fun updateHearts() {
        val heart1 = findViewById<ImageView>(R.id.heart1)
        val heart2 = findViewById<ImageView>(R.id.heart2)
        val heart3 = findViewById<ImageView>(R.id.heart3)

        heart1.visibility = if (lives >= 1) View.VISIBLE else View.INVISIBLE
        heart2.visibility = if (lives >= 2) View.VISIBLE else View.INVISIBLE
        heart3.visibility = if (lives >= 3) View.VISIBLE else View.INVISIBLE
    }

    private fun adjustSpeed() {
        amberSpeed = when {
            score < 20 -> 750L
            score < 30 -> 650L
            score < 40 -> 500L
            else -> 400L
        }
    }

    private fun gameOver() {
        handler.removeCallbacksAndMessages(null)
        runOnUiThread {
            Toast.makeText(this, "Game Over!", Toast.LENGTH_LONG).show()


            saveScore(score, 40.7128, -74.0060)


            startActivity(Intent(this, RecordsActivity::class.java))
            finish()
        }
    }



    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val xValue = it.values[0]
            val sensitivityThreshold = 3.0


            if (xValue > sensitivityThreshold && jackPosition > 0) {
                jackPosition -= 1
                updateGoalPosition()
            } else if (xValue < -sensitivityThreshold && jackPosition < 4) {
                jackPosition += 1
                updateGoalPosition()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    private fun saveScore(score: Int, latitude: Double, longitude: Double) {
        val sharedPreferences = getSharedPreferences("GameScores", MODE_PRIVATE)
        val editor = sharedPreferences.edit()


        val existingScores = sharedPreferences.getStringSet("scores", mutableSetOf()) ?: mutableSetOf()
        val newScoreEntry = "$score,$latitude,$longitude"


        existingScores.add(newScoreEntry)


        editor.putStringSet("scores", existingScores)
        editor.apply()
    }

    private fun updateUI() {
        val keysToRemove = mutableListOf<Pair<Int, Int>>()

        for ((key, amberView) in amberViews) {
            val (row, col) = key
            if (row >= matrix.size || matrix[row][col] != 1) {
                (amberView.parent as? RelativeLayout)?.removeView(amberView)
                keysToRemove.add(key)
            }
        }

        keysToRemove.forEach { amberViews.remove(it) }

        for (row in matrix.indices) {
            for (col in matrix[row].indices) {
                if (matrix[row][col] == 1) {
                    val ballView = getOrCreateBallView(row, col)
                    ballView.y = row * 200f

                    val params = ballView.layoutParams as RelativeLayout.LayoutParams
                    val laneWidth = resources.displayMetrics.widthPixels / 5
                    params.marginStart = (col * laneWidth).toInt() + (laneWidth / 2 - ballView.width / 2).toInt()
                    ballView.layoutParams = params
                }
            }
        }
    }

    private fun getOrCreateBallView(row: Int, col: Int): ImageView {
        val key = Pair(row, col)

        if (amberViews.containsKey(key)) {
            return amberViews[key]!!
        }

        val newBall = ImageView(this).apply {
            setImageResource(R.drawable.amberface)
            layoutParams = RelativeLayout.LayoutParams(
                dpToPx(90),
                dpToPx(90)
            )
        }

        findViewById<RelativeLayout>(R.id.main).addView(newBall)
        amberViews[key] = newBall

        return newBall
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}