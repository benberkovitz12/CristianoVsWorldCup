package com.example.cristianovsworldcup

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val matrix = Array(9){IntArray(5){0} }
    private val amberViews = mutableMapOf<Pair<Int, Int>, ImageView>()
    private var jackPosition = 1
    lateinit var jackImageView: ImageView
    lateinit var scoreText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var score = 0
    private var amberSpeed = 500L
    private var lives = 3
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        jackImageView = findViewById(R.id.imageView)
        scoreText = findViewById(R.id.scoreText)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mediaPlayer = MediaPlayer.create(this, R.raw.soundtrack).apply {
            isLooping = true
            start()
        }

        checkAndRequestLocationPermission()
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show()
            startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }

        initializeMatrix()
        startBallMovement()
        startAddingBalls()
        updateHearts()
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun onResume() {
        super.onResume()
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    private fun addNewBall() {
        val column = Random.nextInt(0, 5)
        matrix[0][column] = 1
        updateUI()
    }

    private fun getOrCreateBallView(row: Int, col: Int): ImageView {
        val key = Pair(row, col)
        val amberViews = mutableMapOf<Pair<Int, Int>, ImageView>()

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

    private fun updateHearts() {
        val heart1 = findViewById<ImageView>(R.id.heart1)
        val heart2 = findViewById<ImageView>(R.id.heart2)
        val heart3 = findViewById<ImageView>(R.id.heart3)

        heart1.visibility = if (lives >= 1) View.VISIBLE else View.INVISIBLE
        heart2.visibility = if (lives >= 2) View.VISIBLE else View.INVISIBLE
        heart3.visibility = if (lives >= 3) View.VISIBLE else View.INVISIBLE
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun initializeMatrix() {
        for (row in matrix.indices) {
            for (col in matrix[row].indices) {
                matrix[row][col] = 0
            }
        }
        matrix[matrix.size - 1][jackPosition] = 2
    }

    fun move_left(view: View) {
        if (jackPosition > 0) {
            jackPosition -= 1
            updateGoalPosition()
        }
    }

    fun move_right(view: View) {
        if (jackPosition < 4) {
            jackPosition += 1
            updateGoalPosition()
        }
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

    private fun showCollisionEffect() {
        val collisionSound = MediaPlayer.create(this, R.raw.hitsound)
        collisionSound.start()


        collisionSound.setOnCompletionListener {
            collisionSound.release()
        }
        jackImageView.setImageResource(R.drawable.pirates)
        Handler(Looper.getMainLooper()).postDelayed({
            jackImageView.setImageResource(R.drawable.jackrunning)
        }, 500)
    }

    private fun updateGoalPosition() {
        val laneWidth = resources.displayMetrics.widthPixels / 5
        val jackX = jackPosition * laneWidth + laneWidth / 2 - jackImageView.width / 2
        jackImageView.x = jackX.toFloat()
    }

    private fun updateScoreUI() {
        scoreText.text = "Score: $score"
    }

    private fun gameOver() {
        handler.removeCallbacksAndMessages(null)
        runOnUiThread {
            Toast.makeText(this, "Game Over!", Toast.LENGTH_LONG).show()
            saveScore(score)
            startActivity(Intent(this, RecordsActivity::class.java))
            finish()
        }
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

    private fun checkLocationPermission() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun checkAndRequestLocationPermission() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission is required!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }


    private fun handleCollisions() {
        val lastRow = matrix.size - 2

        for (col in matrix[lastRow].indices) {
            if (matrix[lastRow - 1][col] == 1) {
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
            }
        }
        updateScoreUI()
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


        for (col in matrix[0].indices) {
            if (matrix[matrix.size - 1][col] == 1) {
                matrix[matrix.size - 1][col] = 0


                val key = Pair(matrix.size - 1, col)
                amberViews[key]?.let { view ->
                    (view.parent as? RelativeLayout)?.removeView(view)
                    amberViews.remove(key)
                }
            }
        }


        handleCollisions()
        updateUI()
    }

    private fun saveScore(score: Int) {
        val sharedPreferences = getSharedPreferences("GameScores", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val existingScores = sharedPreferences.getStringSet("scores", mutableSetOf()) ?: mutableSetOf()


        val newScoreEntry = "$score"
        existingScores.add(newScoreEntry)

        editor.putStringSet("scores", existingScores)
        editor.apply()
    }


    private fun saveScoreFallback(score: Int) {
        saveScoreToSharedPreferences(score, "Unknown Location")
    }

    private fun saveScoreToSharedPreferences(score: Int, location: String) {
        val sharedPreferences = getSharedPreferences("GameScores", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val existingScores = sharedPreferences.getStringSet("scores", mutableSetOf()) ?: mutableSetOf()


        val newScoreEntry = "$score,$location"
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
                    val key = Pair(row, col)
                    val ballView = amberViews[key] ?: getOrCreateBallView(row, col)


                    ballView.y = row * 200f


                    val params = ballView.layoutParams as RelativeLayout.LayoutParams
                    val laneWidth = resources.displayMetrics.widthPixels / 5
                    val ballCenter = laneWidth / 2 - ballView.width / 2
                    params.marginStart = (col * laneWidth + ballCenter).toInt()
                    ballView.layoutParams = params


                    amberViews[key] = ballView
                }
            }
        }
    }

    private fun adjustSpeed() {
        amberSpeed = when {
            score < 20 -> 750L
            score < 30 -> 650L
            score < 40 -> 500L
            else -> 400L
        }
    }

}
