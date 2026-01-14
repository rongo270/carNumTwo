package com.rongo.carnumtwo.feature.score

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.rongo.carnumtwo.R
import com.rongo.carnumtwo.core.storage.ScoreStorage

class ScoreListFragment : Fragment() {

    var onScoreClicked: ((lat: Double, lon: Double) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_score_list, container, false)
        val listView = view.findViewById<ListView>(R.id.list_scores)

        val storage = ScoreStorage(requireContext())
        val scores = storage.getTopScores()

        val displayList = scores.mapIndexed { index, item ->
            "${index + 1}. ${item.name} - ${item.score}"
        }

        // UPDATED: Using custom layout 'R.layout.item_score' for white text
        val adapter = ArrayAdapter(requireContext(), R.layout.item_score, displayList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val item = scores[position]
            onScoreClicked?.invoke(item.lat, item.lon)
        }

        return view
    }
}