package com.dNDTeam.seffectle.main

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.dNDTeam.seffectle.R
import com.dNDTeam.seffectle.dataClasses.ClassFromSchedule
import kotlinx.android.synthetic.main.item_class.view.*

//адаптер для RecyclerView главного окна
class AdapterMAF(private val classesList: List<ClassFromSchedule>) :
    RecyclerView.Adapter<AdapterMAF.ViewHolderForMAF>() {
    //возвращает количество пар в списке
    override fun getItemCount() = classesList.size

    //создаёт ViewHolderForMAF этого адаптера
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolderForMAF(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_class, parent, false)
        )

    //заполняет View значениями
    override fun onBindViewHolder(holder: ViewHolderForMAF, position: Int) {
        holder.bind(position)
    }

    //внутренний класс для этого адаптера
    inner class ViewHolderForMAF(
        //поля класса
        itemView: View,
        private val classBeginTimeTV: TextView = itemView.classBeginTimeValue_TV_SCV,
        private val classEndTimeTV: TextView = itemView.classEndTimeValue_TV_SCV,
        private val classTypeTV: TextView = itemView.classTypeValue_TV_SCV,
        private val classNameTV: TextView = itemView.classNameValue_TV_SCV,
        private val teacherNameTV: TextView = itemView.teacherNameValue_TV_SCV,
        private val roomNameTV: TextView = itemView.roomNameValue_TV_SCV,
        private val classSerialNumberTV: TextView = itemView.classSerialNumber_TV_SCV
    ) : RecyclerView.ViewHolder(itemView) {
        //функция заполнения полей itemView
        fun bind(position: Int) {
            classBeginTimeTV.text = classesList[position].classBeginTime
            classEndTimeTV.text = classesList[position].classEndTime
            classTypeTV.text = classesList[position].classType
            teacherNameTV.text = classesList[position].teacherName
            classNameTV.text = classesList[position].className
            roomNameTV.text = classesList[position].roomName
            classSerialNumberTV.text = classesList[position].classSerialNumber
        }
    }
}