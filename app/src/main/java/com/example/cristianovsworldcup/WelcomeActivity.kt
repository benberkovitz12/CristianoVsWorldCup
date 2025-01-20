package com.example.cristianovsworldcup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)


        val helpJackButton = findViewById<Button>(R.id.helpJackButton)
        helpJackButton.setOnClickListener {

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val helpJackTiltButton = findViewById<Button>(R.id.helpJackTiltButton)
        helpJackTiltButton.setOnClickListener {
            val intent = Intent(this, TiltGameActivity::class.java)
            startActivity(intent)
        }

        val recordsButton = findViewById<Button>(R.id.recordsButton)
        recordsButton.setOnClickListener {
            val intent = Intent(this, RecordsActivity::class.java)
            startActivity(intent)
        }


    }
}