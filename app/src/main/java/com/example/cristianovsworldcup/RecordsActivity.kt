package com.example.cristianovsworldcup

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class RecordsActivity : AppCompatActivity(), TopRecordsFragment.OnRecordSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records)

        val sharedPreferences = getSharedPreferences("GameScores", MODE_PRIVATE)
        val scores = sharedPreferences.getStringSet("scores", emptySet())?.mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) {
                val score = parts[0].toIntOrNull()
                val address = parts[1]
                if (score != null && address.isNotBlank()) {
                    Pair(score, address)
                } else null
            } else null
        }?.sortedByDescending { it.first } ?: emptyList()

        Log.d("RecordsActivity", "Scores passed to TopRecordsFragment: $scores")


        val topRecordsFragment = TopRecordsFragment().apply {
            arguments = Bundle().apply {
                putSerializable("scores", ArrayList(scores))
            }
        }
        topRecordsFragment.setOnRecordSelectedListener(this)


        supportFragmentManager.beginTransaction()
            .replace(R.id.recordsFragmentContainer, topRecordsFragment)
            .commit()


        supportFragmentManager.beginTransaction()
            .replace(R.id.mapFragmentContainer, MapFragment())
            .commit()
    }

    override fun onRecordSelected(location: String) {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragmentContainer) as? MapFragment
        if (mapFragment != null) {
            mapFragment.zoomToLocation(location)
        } else {
            Log.e("RecordsActivity", "MapFragment not found!")
        }
    }
}