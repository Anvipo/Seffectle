package com.dNDTeam.seffectle.main

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dNDTeam.seffectle.R
import com.dNDTeam.seffectle.db.getClassesList

class MainActivityFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //создаём view
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        //находим recyclerview
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView_FAM)
        //т.к. этот rv служит только для отображения статичной информации, то ставим true
        recyclerView?.setHasFixedSize(true)
        //задаём layoutManager
        recyclerView?.layoutManager = LinearLayoutManager(context)

        //задаём адаптер
        recyclerView?.adapter = AdapterMAF(
            getClassesList(
                arguments!!.getInt("selectedDay"),
                context!!
            )
        )

        return view
    }
}