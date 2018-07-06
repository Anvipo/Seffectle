package com.dNDTeam.seffectle.scheduleActions

import android.annotation.SuppressLint
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import com.dNDTeam.seffectle.R
import com.dNDTeam.seffectle.dataClasses.ClassFromSchedule
import com.dNDTeam.seffectle.vk.VKUser
import kotlinx.android.synthetic.main.item_class_action.view.*
import org.jetbrains.anko.toast

//https://blog.stylingandroid.com/recyclerview-animations-changing-items/
//https://ziginsider.github.io/RecyclerView/
class ScheduleAdapter(
        private val items: MutableList<Pair<ClassFromSchedule, Boolean>>,
        private val context: Context,
        selectedDay: Int
) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_class_action, parent, false))

    fun getItem(position: Int): ClassFromSchedule = items[position].first

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount() = items.size

    private val newClass = ClassFromSchedule(selectedDay,
            VKUser.vkAccountOwner.vkId,
        classSerialNumber = "${context.getString(R.string.class_serial_number)}${itemCount + 1}"
    )

    private lateinit var mRecyclerView: RecyclerView

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        mRecyclerView = recyclerView
    }

    fun addNewItem() = items.add(newClass.copy() to true).also {
        notifyItemInserted(itemCount - 1)
    }

    fun hideAllItemsExcludeAtAndScrollTo(position: Int) {
        for (index in items.indices)
            if (index != position) {
                items[position] = items[position].first to false
                notifyItemChanged(position)
            }

        mRecyclerView.scrollToPosition(position)
    }

    enum class NotTimeFields {
        CLASS_NAME,
        TEACHER_NAME,
        ROOM_NAME
    }

    enum class TimeFields {
        CLASS_BEGIN_TIME,
        CLASS_END_TIME
    }

    inner class ViewHolder(
            itemView: View,
            private val classSerialNumberTV: TextView = itemView.classSerialNumber_TV_CAV,
            private val classBeginTimeET: EditText = itemView.classBeginTime_ET_CAV,
            private val classEndTimeET: EditText = itemView.classEndTime_ET_CAV,
            private val classTypeS: Spinner = itemView.classType_S_CAV,
            private val classNameET: EditText = itemView.className_ET_CAV,
            private val teacherNameET: EditText = itemView.teacherName_ET_CAV,
            private val roomNameET: EditText = itemView.roomName_ET_CAV,
            private val hidingLayout: LinearLayout = itemView.hidingLayout,
            addButton: ImageButton = itemView.add_class_button,
            removeButton: ImageButton = itemView.remove_class_button,
            upButton: ImageButton = itemView.move_up_button,
            downButton: ImageButton = itemView.move_down_button
    ) : RecyclerView.ViewHolder(itemView) {
        init {
            addButton.setOnClickListener(insert())
            removeButton.setOnClickListener(remove())
            upButton.setOnClickListener(moveUp())
            downButton.setOnClickListener(moveDown())
            itemView.setOnClickListener(toggleText())

            classBeginTimeET.addTextChangedListener(TextWatcherForTime(classBeginTimeET, TimeFields.CLASS_BEGIN_TIME))
            classEndTimeET.addTextChangedListener(TextWatcherForTime(classEndTimeET, TimeFields.CLASS_END_TIME))
            roomNameET.addTextChangedListener(EditTextWatcher(NotTimeFields.ROOM_NAME))
            teacherNameET.addTextChangedListener(EditTextWatcher(NotTimeFields.TEACHER_NAME))
            classNameET.addTextChangedListener(EditTextWatcher(NotTimeFields.CLASS_NAME))
            classTypeS.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    items[layoutPosition].first.classType =
                            context.resources.getStringArray(R.array.class_types_array)[position]
                }
            }
            val adapter = ArrayAdapter.createFromResource(context,
                    R.array.class_types_array, android.R.layout.simple_spinner_item)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            classTypeS.adapter = adapter
        }

        private fun insert(): (View) -> Unit = {
            if (itemCount != 7) {
                val newItemPosition = layoutPosition + 1
                items.add(newItemPosition, newClass.copy() to true)
                notifyItemInserted(newItemPosition)

                hideAllItemsExcludeAtAndScrollTo(newItemPosition)
            } else
                context.toast(context.getString(R.string.cant_add_class))
        }

        private fun remove(): (View) -> Unit = {
            layoutPosition.also { currentPosition ->
                items.removeAt(currentPosition)
                notifyItemRemoved(currentPosition)
                notifyItemRangeChanged(currentPosition, itemCount - currentPosition)
            }
        }

        private fun moveUp(): (View) -> Unit = {
            layoutPosition.takeIf { it > 0 }?.also { currentPosition ->
                items.removeAt(currentPosition).also {
                    items.add(currentPosition - 1, it)
                }
                notifyItemChanged(currentPosition)
                notifyItemChanged(currentPosition - 1)
            }
        }

        private fun moveDown(): (View) -> Unit = {
            layoutPosition.takeIf { it < items.size - 1 }?.also { currentPosition ->
                items.removeAt(currentPosition).also {
                    items.add(currentPosition + 1, it)
                }
                notifyItemChanged(currentPosition)
                notifyItemChanged(currentPosition + 1)
            }
        }

        private fun toggleText(): (View) -> Unit = {
            items[layoutPosition] = items[layoutPosition].first to !items[layoutPosition].second
            notifyItemChanged(layoutPosition)
        }

        @SuppressLint("SetTextI18n")
        fun bind(position: Int) {
            val data = items[position]

            classSerialNumberTV.text =
                    "${context.getString(R.string.class_serial_number)}${position + 1}"
            classBeginTimeET.setText(data.first.classBeginTime)
            classEndTimeET.setText(data.first.classEndTime)
            teacherNameET.setText(data.first.teacherName)
            classNameET.setText(data.first.className)
            roomNameET.setText(data.first.roomName)

            classTypeS.setSelection(
                    when {
                        data.first.classType == context.resources.getStringArray(R.array.class_types_array)[0] -> 0
                        else -> 1
                    })

            hidingLayout.visibility = if (data.second) View.VISIBLE else View.GONE
        }

        inner class EditTextWatcher(private var field: NotTimeFields) : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) = Unit

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) = Unit

            override fun afterTextChanged(editable: Editable) {
                when (field) {
                    NotTimeFields.CLASS_NAME -> items[layoutPosition].first.className = editable.toString()
                    NotTimeFields.ROOM_NAME -> items[layoutPosition].first.roomName = editable.toString()
                    NotTimeFields.TEACHER_NAME -> items[layoutPosition].first.teacherName = editable.toString()
                }
            }
        }

        inner class TextWatcherForTime(private val editText: EditText, private var field: TimeFields) : TextWatcher {
            private var hasColon = false    //Двоеточие уже стоит

            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                hasColon = editText.text.toString().contains(":")
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) = Unit

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(editable: Editable) {
                val text = editText.text.toString()
                if (text != "1" && text.length == 1 && text != "0" && text != "2")
                    editText.setText("0$text")

                if (text.length == 2 && !hasColon) {
                    editText.setText("$text:")
                    editText.setSelection(3)
                    hasColon = true
                }

                when (field) {
                    TimeFields.CLASS_BEGIN_TIME -> items[layoutPosition].first.classBeginTime = editable.toString()
                    TimeFields.CLASS_END_TIME -> items[layoutPosition].first.classEndTime = editable.toString()
                }
            }
        }
    }
}