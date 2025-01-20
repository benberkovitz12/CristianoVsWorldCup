package com.example.cristianovsworldcup

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class TopRecordsFragment : Fragment() {

    interface OnRecordSelectedListener {
        fun onRecordSelected(location: String)
    }

    private var listener: OnRecordSelectedListener? = null

    fun setOnRecordSelectedListener(listener: OnRecordSelectedListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_top_records, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView = view.findViewById<ListView>(R.id.recordsListView)


        val sharedPreferences = requireActivity().getSharedPreferences("GameScores", Context.MODE_PRIVATE)
        val scores = sharedPreferences.getStringSet("scores", emptySet())?.mapNotNull {
            it.toIntOrNull()
        }?.sortedDescending() ?: emptyList()


        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            scores.map { "Score: $it" }
        )
        listView.adapter = adapter
    }
}